package com.paperteleporter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlatformManager {
    private final JavaPlugin plugin;
    private final PlatformStore store;

    private PlatformMaterials materials;
    private final Map<String, PlatformData> byId = new LinkedHashMap<>();
    private final Map<UUID, PlatformData> byNpc = new HashMap<>();

    public PlatformManager(JavaPlugin plugin, PlatformStore store, PlatformMaterials materials) {
        this.plugin = plugin;
        this.store = store;
        this.materials = materials;
    }

    public void load() throws IOException {
        byId.clear();
        byNpc.clear();
        for (PlatformData data : store.load()) {
            byId.put(data.getId(), data);
            byNpc.put(data.getNpcUuid(), data);
        }
    }

    public void save() throws IOException {
        store.save(new ArrayList<>(byId.values()));
    }

    public void reloadMaterials(PlatformMaterials materials) {
        this.materials = materials;
    }

    public boolean containsId(String id) {
        return byId.containsKey(id.toLowerCase());
    }

    public PlatformData findByNpc(UUID uuid) {
        return byNpc.get(uuid);
    }

    public Collection<PlatformData> allPlatforms() {
        return byId.values();
    }

    public boolean isProtectedBlock(Location location) {
        for (PlatformData data : byId.values()) {
            if (data.isProtectedBlock(location)) {
                return true;
            }
        }
        return false;
    }

    public String createPlatform(Player player, String idInput, Location anchorLocation) {
        String id = idInput.toLowerCase();
        if (containsId(id)) {
            return ChatColor.RED + "Platform id already exists: " + id;
        }

        World world = anchorLocation.getWorld();
        if (world == null) {
            return ChatColor.RED + "Could not resolve world for platform.";
        }

        DirectionPair direction = DirectionPair.fromYaw(player.getLocation().getYaw()).rotate180();

        int baseX = anchorLocation.getBlockX();
        int baseY = anchorLocation.getBlockY();
        int baseZ = anchorLocation.getBlockZ();

        int centerX = baseX + (direction.rightX * 6) + (direction.forwardX * 3);
        int centerY = baseY;
        int centerZ = baseZ + (direction.rightZ * 6) + (direction.forwardZ * 3);

        BlockPoint center = new BlockPoint(centerX, centerY, centerZ);
        BuildResult result = buildPlatform(world, center, direction);

        BlockPoint anchor = BlockPoint.fromLocation(anchorLocation);
        PlatformData data = new PlatformData(id, world.getName(), anchor, center, result.npcUuid(), result.protectedBlocks(), direction.toIndex());

        byId.put(id, data);
        byNpc.put(result.npcUuid(), data);

        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save platforms.json: " + e.getMessage());
        }

        return ChatColor.GREEN + "Platform created with id " + id;
    }

    public String rotatePlatform(String idInput) {
        String id = idInput.toLowerCase();
        PlatformData data = byId.get(id);
        if (data == null) {
            return ChatColor.RED + "Unknown platform id: " + id;
        }

        World world = Bukkit.getWorld(data.getWorldName());
        if (world == null) {
            return ChatColor.RED + "Target world for platform is not loaded.";
        }

        for (BlockPoint block : data.getProtectedBlocks()) {
            world.getBlockAt(block.getX(), block.getY(), block.getZ()).setType(Material.AIR, false);
        }

        UUID oldNpcUuid = data.getNpcUuid();
        Entity oldNpc = world.getEntity(oldNpcUuid);
        if (oldNpc != null) {
            oldNpc.remove();
        }

        DirectionPair current = DirectionPair.fromIndex(data.getDirectionIndex());
        DirectionPair rotated = current.rotate90CounterClockwise();

        BuildResult result = buildPlatform(world, data.getSpawnPoint(), rotated);
        data.setDirectionIndex(rotated.toIndex());
        data.setNpcUuid(result.npcUuid());
        data.setProtectedBlocks(result.protectedBlocks());

        byNpc.remove(oldNpcUuid);
        byNpc.put(result.npcUuid(), data);

        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save platforms.json: " + e.getMessage());
        }

        return ChatColor.GREEN + "Rotated platform " + id + " by 90 degrees counterclockwise.";
    }

    public String removePlatform(String idInput) {
        String id = idInput.toLowerCase();
        PlatformData data = byId.remove(id);
        if (data == null) {
            return ChatColor.RED + "Unknown platform id: " + id;
        }
        byNpc.remove(data.getNpcUuid());

        World world = Bukkit.getWorld(data.getWorldName());
        if (world != null) {
            for (BlockPoint block : data.getProtectedBlocks()) {
                world.getBlockAt(block.getX(), block.getY(), block.getZ()).setType(Material.AIR, false);
            }

            Entity entity = world.getEntity(data.getNpcUuid());
            if (entity != null) {
                entity.remove();
            }
        }

        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save platforms.json: " + e.getMessage());
        }

        return ChatColor.YELLOW + "Removed platform " + id;
    }

    public void teleport(Player player, PlatformData data) {
        World world = Bukkit.getWorld(data.getWorldName());
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Target world for platform is not loaded.");
            return;
        }
        player.teleport(data.spawnLocation(world));
        player.sendMessage(ChatColor.GREEN + "Teleported to platform " + data.getId());
    }

    private BuildResult buildPlatform(World world, BlockPoint center, DirectionPair direction) {
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        List<BlockPoint> protectedBlocks = new ArrayList<>();

        for (int r = 0; r < 7; r++) {
            for (int f = 0; f < 7; f++) {
                int x = centerX + direction.rightX * (r - 3) + direction.forwardX * (f - 3);
                int z = centerZ + direction.rightZ * (r - 3) + direction.forwardZ * (f - 3);

                for (int y = centerY; y <= centerY + 4; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    protectedBlocks.add(new BlockPoint(x, y, z));
                }

                Block floorBlock = world.getBlockAt(x, centerY, z);
                if (r == 3 && f == 3) {
                    floorBlock.setType(materials.spawn(), false);
                } else if (r == 3 && f == 2) {
                    floorBlock.setType(materials.npc(), false);
                } else {
                    floorBlock.setType(materials.platform(), false);
                }

                boolean perimeter = r == 0 || r == 6 || f == 0 || f == 6;
                boolean opening = f == 6 && r >= 2 && r <= 4;
                if (perimeter && !opening) {
                    world.getBlockAt(x, centerY + 1, z).setType(materials.fence(), false);
                }
            }
        }

        placeLight(world, centerX, centerY + 2, centerZ, direction, 0, 0);
        placeLight(world, centerX, centerY + 2, centerZ, direction, 6, 0);
        placeLight(world, centerX, centerY + 2, centerZ, direction, 0, 6);
        placeLight(world, centerX, centerY + 2, centerZ, direction, 6, 6);

        Location npcLocation = relativeLocation(world, centerX, centerY + 1, centerZ, direction, 3, 2).add(0.5, 0.0, 0.5);
        Location openingCenter = relativeLocation(world, centerX, centerY + 1, centerZ, direction, 3, 6).add(0.5, 0.0, 0.5);
        npcLocation.setDirection(openingCenter.toVector().subtract(npcLocation.toVector()));
        Villager villager = (Villager) world.spawnEntity(npcLocation, EntityType.VILLAGER);
        villager.customName(Component.text("Teleporter NPC", NamedTextColor.GOLD));
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setPersistent(true);

        return new BuildResult(villager.getUniqueId(), protectedBlocks);
    }

    private void placeLight(World world, int centerX, int y, int centerZ, DirectionPair direction, int r, int f) {
        Location location = relativeLocation(world, centerX, y, centerZ, direction, r, f);
        world.getBlockAt(location).setType(materials.light(), false);
    }

    private Location relativeLocation(World world, int centerX, int y, int centerZ, DirectionPair direction, int r, int f) {
        int x = centerX + direction.rightX * (r - 3) + direction.forwardX * (f - 3);
        int z = centerZ + direction.rightZ * (r - 3) + direction.forwardZ * (f - 3);
        return new Location(world, x, y, z);
    }

    private record BuildResult(UUID npcUuid, List<BlockPoint> protectedBlocks) {
    }

    private record DirectionPair(int forwardX, int forwardZ, int rightX, int rightZ) {
        DirectionPair rotate180() {
            return new DirectionPair(-forwardX, -forwardZ, -rightX, -rightZ);
        }

        DirectionPair rotate90CounterClockwise() {
            return new DirectionPair(-rightX, -rightZ, forwardX, forwardZ);
        }

        int toIndex() {
            if (forwardX == 0 && forwardZ == 1 && rightX == -1 && rightZ == 0) {
                return 0;
            }
            if (forwardX == 1 && forwardZ == 0 && rightX == 0 && rightZ == 1) {
                return 1;
            }
            if (forwardX == 0 && forwardZ == -1 && rightX == 1 && rightZ == 0) {
                return 2;
            }
            return 3;
        }

        static DirectionPair fromIndex(int index) {
            return switch (Math.floorMod(index, 4)) {
                case 0 -> new DirectionPair(0, 1, -1, 0);
                case 1 -> new DirectionPair(1, 0, 0, 1);
                case 2 -> new DirectionPair(0, -1, 1, 0);
                default -> new DirectionPair(-1, 0, 0, -1);
            };
        }

        static DirectionPair fromYaw(float yaw) {
            float normalized = ((yaw % 360) + 360) % 360;

            if (normalized >= 45 && normalized < 135) {
                return new DirectionPair(-1, 0, 0, -1);
            }
            if (normalized >= 135 && normalized < 225) {
                return new DirectionPair(0, -1, 1, 0);
            }
            if (normalized >= 225 && normalized < 315) {
                return new DirectionPair(1, 0, 0, 1);
            }
            return new DirectionPair(0, 1, -1, 0);
        }
    }
}
