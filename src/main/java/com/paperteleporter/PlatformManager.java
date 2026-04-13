package com.paperteleporter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.Wall.Height;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Lantern;
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
    public static final int MAX_PLATFORMS = 216;
    private static final int DEFAULT_MIN_SPAWN_DISTANCE = 20;

    private final JavaPlugin plugin;
    private final PlatformStore store;
    private final CustomPresetStore customPresetStore;

    private PlatformMaterials materials;
    private int minSpawnDistance = DEFAULT_MIN_SPAWN_DISTANCE;
    private final Map<String, PlatformData> byId = new LinkedHashMap<>();
    private final Map<UUID, PlatformData> byNpc = new HashMap<>();
    private final Map<Integer, CustomPresetData> customPresets = new HashMap<>();

    public PlatformManager(JavaPlugin plugin, PlatformStore store, PlatformMaterials materials) {
        this.plugin = plugin;
        this.store = store;
        this.customPresetStore = new CustomPresetStore(plugin);
        this.materials = materials;
        try {
            this.customPresets.putAll(customPresetStore.load());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load custom presets: " + e.getMessage());
        }
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

    public String createManualBackup() {
        try {
            store.save(new ArrayList<>(byId.values()));
            customPresetStore.save(customPresets);
            return ChatColor.GREEN + "Manual backup created in plugins/PaperTeleporter-backups/.";
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create manual backup: " + e.getMessage());
            return ChatColor.RED + "Failed to create manual backup. Check server logs.";
        }
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

    public int getMinSpawnDistance() {
        return minSpawnDistance;
    }

    public PlatformData getPlatformByChestLocation(BlockPoint chestLocation) {
        for (PlatformData platform : byId.values()) {
            if (platform.getChestLocation() != null && platform.getChestLocation().equals(chestLocation)) {
                return platform;
            }
        }
        return null;
    }

    public String setMinSpawnDistance(int distance) {
        if (distance < 20) {
            return ChatColor.RED + "Minimum spawn distance must be at least 20 blocks.";
        }
        this.minSpawnDistance = distance;
        return ChatColor.GREEN + "Minimum spawn distance set to " + distance + " blocks.";
    }

    public CustomPresetData getCustomPreset(int number) {
        return customPresets.get(number);
    }

    private int findNextFreeCustomPresetNumber() {
        for (int i = 7; i <= 99; i++) {
            if (!customPresets.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    // CW steps from South: S=0, W=1, N=2, E=3
    private int facingToCwSteps(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    private BlockFace rotateFaceCw(BlockFace face, int steps) {
        BlockFace[] ring = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (int i = 0; i < ring.length; i++) {
            if (ring[i] == face) {
                return ring[(i + steps) % 4];
            }
        }
        return face; // UP, DOWN, non-cardinal: unchanged
    }

    private void rotateBlockDataCw(BlockData blockData, int steps) {
        if (steps == 0) return;
        if (blockData instanceof Directional directional) {
            directional.setFacing(rotateFaceCw(directional.getFacing(), steps));
        }
    }

    public String saveCustomPreset(Player player, Location centerLocation) {
        int presetNumber = findNextFreeCustomPresetNumber();
        if (presetNumber == -1) {
            return ChatColor.RED + "No free custom preset slots left (7-99).";
        }
        String presetName = "custom-" + presetNumber;

        World world = centerLocation.getWorld();
        if (world == null) {
            return ChatColor.RED + "Could not resolve world.";
        }

        BlockPoint center = BlockPoint.fromLocation(centerLocation);

        // Center floor must stay empty in template build phase; spawn block is added only when platform is created.
        Block centerBlock = world.getBlockAt(center.getX(), center.getY(), center.getZ());
        if (!isIgnorableTemplateBlock(centerBlock.getType())) {
            return ChatColor.RED + "Center block must be AIR. Stand in the center hole when saving preset.";
        }

        // Require one-block-deep center hole: block below center must not be air.
        Block belowCenter = world.getBlockAt(center.getX(), center.getY() - 1, center.getZ());
        if (belowCenter.getType().isAir()) {
            return ChatColor.RED + "Stand in a 1-block-deep center hole before saving preset.";
        }

        List<CustomPresetData.BlockEntry> blocks = new ArrayList<>();

        for (int r = 0; r < 7; r++) {
            for (int f = 0; f < 7; f++) {
                for (int y = 0; y < 7; y++) {
                    int x = center.getX() - 3 + r;
                    int blockY = center.getY() + y;
                    int z = center.getZ() - 3 + f;
                    Block block = world.getBlockAt(x, blockY, z);
                    if (!isIgnorableTemplateBlock(block.getType())) {
                        blocks.add(new CustomPresetData.BlockEntry(r, y, f, block.getType().name(), block.getBlockData().getAsString()));
                    }
                    world.getBlockAt(x, blockY, z).setType(Material.AIR, false);
                }
            }
        }

        // player's actual facing at save time (without rotate180)
        int saveCwSteps = facingToCwSteps(DirectionPair.fromYaw(player.getLocation().getYaw()).openingFace());

        CustomPresetData data = new CustomPresetData(presetNumber, presetName, blocks);
        data.setSaveDirectionCwSteps(saveCwSteps);
        customPresets.put(presetNumber, data);

        try {
            customPresetStore.save(customPresets);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save custom preset: " + e.getMessage());
            return ChatColor.RED + "Failed to save custom preset.";
        }

            return ChatColor.GREEN + "Custom preset " + presetNumber + " saved and template area cleared."
                + ChatColor.GRAY + " (stand position used as center)";
    }

            private boolean isIgnorableTemplateBlock(Material material) {
            return material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR
                || material == Material.SNOW;
            }

    public String removeCustomPreset(int presetNumber) {
        if (!customPresets.containsKey(presetNumber)) {
            return ChatColor.RED + "Custom preset " + presetNumber + " does not exist.";
        }
        customPresets.remove(presetNumber);
        try {
            customPresetStore.save(customPresets);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save custom presets: " + e.getMessage());
            return ChatColor.RED + "Failed to remove custom preset.";
        }
        return ChatColor.GREEN + "Custom preset " + presetNumber + " removed.";
    }

    private void addDefaultStructure(World world, BlockPoint center) {
        world.getBlockAt(center.getX(), center.getY(), center.getZ()).setType(materials.spawn(), false);
        world.getBlockAt(center.getX(), center.getY(), center.getZ() - 1).setType(materials.npc(), false);
        int lightY = center.getY() + 2;
        world.getBlockAt(center.getX() - 3, lightY, center.getZ() - 3).setType(materials.light(), false);
        world.getBlockAt(center.getX() - 3, lightY, center.getZ() + 3).setType(materials.light(), false);
        world.getBlockAt(center.getX() + 3, lightY, center.getZ() - 3).setType(materials.light(), false);
        world.getBlockAt(center.getX() + 3, lightY, center.getZ() + 3).setType(materials.light(), false);
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
        return createPlatformWithPreset(player, idInput, anchorLocation, 2); // Default FENCED
    }

    public String createPlatformWithPreset(Player player, String idInput, Location anchorLocation, int presetNumber) {
        String id = idInput.toLowerCase();
        if (byId.size() >= MAX_PLATFORMS) {
            return ChatColor.RED + "Cannot create more platforms. Maximum is " + MAX_PLATFORMS + ".";
        }

        if (containsId(id)) {
            return ChatColor.RED + "Platform id already exists: " + id;
        }

        // Check if it's a custom preset or built-in
        boolean isCustom = presetNumber > 6;
        if (isCustom) {
            if (presetNumber < 7 || presetNumber > 99) {
                return ChatColor.RED + "Invalid custom preset number. Valid: 7-99";
            }
            if (customPresets == null || !customPresets.containsKey(presetNumber)) {
                return ChatColor.RED + "Custom preset " + presetNumber + " not found.";
            }
        } else {
            if (!Preset.isValid(presetNumber)) {
                return ChatColor.RED + "Invalid preset number. Valid: 1-6 (built-in) or 7-99 (custom)";
            }
        }

        World world = anchorLocation.getWorld();
        if (world == null) {
            return ChatColor.RED + "Could not resolve world for platform.";
        }

        DirectionPair direction = DirectionPair.fromYaw(player.getLocation().getYaw()).rotate180();

        int baseX = anchorLocation.getBlockX();
        int baseY = anchorLocation.getBlockY();
        int baseZ = anchorLocation.getBlockZ();

        int centerX = baseX - (direction.forwardX * 3);
        int centerY = baseY;
        int centerZ = baseZ - (direction.forwardZ * 3);

        BlockPoint center = new BlockPoint(centerX, centerY, centerZ);
        String worldName = world.getName();
        for (PlatformData existing : byId.values()) {
            if (existing.getWorldName().equals(worldName) && existing.getSpawnPoint().equals(center)) {
                return ChatColor.RED + "Cannot create platform: location overlaps with existing platform '" + existing.getId() + "'.";
            }
            if (existing.getWorldName().equals(worldName)) {
                double distance = center.distanceTo(existing.getSpawnPoint());
                if (distance < minSpawnDistance) {
                    return ChatColor.RED + "Platform too close to '" + existing.getId() + "' (" + String.format("%.1f", distance) + "m, min: " + minSpawnDistance + "m).";
                }
            }
        }

        BuildResult result;
        if (isCustom) {
            CustomPresetData customData = customPresets.get(presetNumber);
            result = buildCustomPlatform(world, center, direction, customData);
        } else {
            Preset preset = Preset.fromNumber(presetNumber);
            result = buildPlatform(world, center, direction, preset);
        }

        BlockPoint anchor = BlockPoint.fromLocation(anchorLocation);
        PlatformData data = new PlatformData(id, worldName, anchor, center, result.npcUuid(), result.chestLocation(), result.protectedBlocks(), direction.toIndex(), presetNumber);

        byId.put(id, data);
        byNpc.put(result.npcUuid(), data);

        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save platforms.json: " + e.getMessage());
        }

        return ChatColor.GREEN + "Platform created with id " + id + " (preset " + presetNumber + ")";
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

        Preset preset = Preset.fromNumber(data.getPresetNumber());
        BuildResult result = buildPlatform(world, data.getSpawnPoint(), rotated, preset);
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

    public String changePreset(String idInput, int newPresetNumber) {
        String id = idInput.toLowerCase();
        PlatformData data = byId.get(id);
        if (data == null) {
            return ChatColor.RED + "Unknown platform id: " + id;
        }

        if (data.getPresetNumber() == newPresetNumber) {
            return ChatColor.YELLOW + "Platform " + id + " is already preset " + newPresetNumber;
        }

        boolean isCustom = newPresetNumber > 6;
        if (isCustom) {
            if (newPresetNumber < 7 || newPresetNumber > 99) {
                return ChatColor.RED + "Invalid custom preset number. Valid: 7-99";
            }
            if (!customPresets.containsKey(newPresetNumber)) {
                return ChatColor.RED + "Custom preset " + newPresetNumber + " not found.";
            }
        } else if (!Preset.isValid(newPresetNumber)) {
            return ChatColor.RED + "Invalid preset number. Valid: 1-6 (built-in) or 7-99 (custom)";
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

        DirectionPair direction = DirectionPair.fromIndex(data.getDirectionIndex());
        BuildResult result;
        if (isCustom) {
            result = buildCustomPlatform(world, data.getSpawnPoint(), direction, customPresets.get(newPresetNumber));
        } else {
            Preset newPreset = Preset.fromNumber(newPresetNumber);
            result = buildPlatform(world, data.getSpawnPoint(), direction, newPreset);
        }

        data.setPresetNumber(newPresetNumber);
        data.setNpcUuid(result.npcUuid());
        data.setProtectedBlocks(result.protectedBlocks());

        byNpc.remove(oldNpcUuid);
        byNpc.put(result.npcUuid(), data);

        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save platforms.json: " + e.getMessage());
        }

        return ChatColor.GREEN + "Changed platform " + id + " to preset " + newPresetNumber;
    }

    private BuildResult buildPlatform(World world, BlockPoint center, DirectionPair direction, Preset preset) {
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        List<BlockPoint> protectedBlocks = new ArrayList<>();
        List<BlockPoint> railingBlocks = new ArrayList<>();

        for (int r = 0; r < 7; r++) {
            for (int f = 0; f < 7; f++) {
                int x = centerX + direction.rightX * (r - 3) + direction.forwardX * (f - 3);
                int z = centerZ + direction.rightZ * (r - 3) + direction.forwardZ * (f - 3);

                for (int y = centerY; y <= centerY + 6; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    protectedBlocks.add(new BlockPoint(x, y, z));
                }

                Block floorBlock = world.getBlockAt(x, centerY, z);
                if (r == 3 && f == 3) {
                    floorBlock.setType(materials.spawn(), false);
                } else if (r == 3 && f == 2) {
                    floorBlock.setType(materials.npc(), false);
                } else if (f == 6 && r >= 2 && r <= 4 && preset != Preset.OPEN) {
                    floorBlock.setType(materials.stairs(), false);
                    BlockData stairData = floorBlock.getBlockData();
                    if (stairData instanceof Stairs stairs) {
                        stairs.setFacing(direction.openingFaceOpposite());
                        floorBlock.setBlockData(stairs, false);
                    }
                } else {
                    floorBlock.setType(materials.platform(), false);
                }

                boolean perimeter = r == 0 || r == 6 || f == 0 || f == 6;
                boolean opening = f == 6 && r >= 2 && r <= 4;
                
                if (perimeter && !opening) {
                    Block railBlock = world.getBlockAt(x, centerY + 1, z);
                    if (preset == Preset.OPEN) {
                        railBlock.setType(Material.AIR, false);
                    } else if (preset == Preset.FENCED) {
                        railBlock.setType(materials.fence(), false);
                        railingBlocks.add(BlockPoint.fromLocation(railBlock.getLocation()));
                    } else if (preset == Preset.ENCLOSED) {
                        railBlock.setType(Material.BRICK_WALL, false);
                        railingBlocks.add(BlockPoint.fromLocation(railBlock.getLocation()));
                    } else if (preset == Preset.TOWERING) {
                        railBlock.setType(materials.fence(), false);
                        railingBlocks.add(BlockPoint.fromLocation(railBlock.getLocation()));
                        boolean isCorner = (r == 0 || r == 6) && (f == 0 || f == 6);
                        if (isCorner) {
                            for (int hy = centerY + 2; hy <= centerY + 4; hy++) {
                                world.getBlockAt(x, hy, z).setType(materials.fence(), false);
                            }
                        }
                    } else if (preset == Preset.FLAT_ROOF) {
                        railBlock.setType(materials.fence(), false);
                        railingBlocks.add(BlockPoint.fromLocation(railBlock.getLocation()));
                        boolean isCorner = (r == 0 || r == 6) && (f == 0 || f == 6);
                        if (isCorner) {
                            for (int hy = centerY + 2; hy <= centerY + 4; hy++) {
                                world.getBlockAt(x, hy, z).setType(materials.fence(), false);
                            }
                        }
                    } else if (preset == Preset.GABLE_ROOF) {
                        railBlock.setType(materials.fence(), false);
                        railingBlocks.add(BlockPoint.fromLocation(railBlock.getLocation()));
                    }
                } else if (!perimeter && !opening && preset == Preset.FLAT_ROOF) {
                    world.getBlockAt(x, centerY + 2, z).setType(Material.OAK_SLAB, false);
                }
            }
        }

        if (preset == Preset.FLAT_ROOF) {
            for (int r = 0; r < 7; r++) {
                for (int f = 0; f < 7; f++) {
                    boolean perimeter = r == 0 || r == 6 || f == 0 || f == 6;
                    boolean opening = f == 6 && r >= 2 && r <= 4;
                    if (!perimeter && !opening) {
                        Location roofLocation = relativeLocation(world, centerX, centerY + 2, centerZ, direction, r, f);
                        world.getBlockAt(roofLocation).setType(Material.OAK_SLAB, false);
                    }
                }
            }
        } else if (preset == Preset.GABLE_ROOF) {
            for (int r = 0; r < 7; r++) {
                for (int f = 0; f < 7; f++) {
                    int distFromCenter = Math.abs(r - 3);
                    int roofY = centerY + 2 + (3 - distFromCenter);
                    Location roofLocation = relativeLocation(world, centerX, roofY, centerZ, direction, r, f);
                    Block roofBlock = world.getBlockAt(roofLocation);
                    if (r == 3) {
                        roofBlock.setType(Material.OAK_LOG, false);
                    } else {
                        roofBlock.setType(Material.OAK_STAIRS, false);
                        BlockData roofData = roofBlock.getBlockData();
                        if (roofData instanceof Directional directional) {
                            if (r < 3) {
                                directional.setFacing(direction.rightFace());
                            } else if (r > 3) {
                                directional.setFacing(direction.rightFaceOpposite());
                            }
                            roofBlock.setBlockData(directional, false);
                        }
                    }
                }
            }
        }

        if (preset == Preset.GABLE_ROOF) {
            int ridgeY = centerY + 5;
            for (int f : new int[]{0, 6}) {
                Location lanternLoc = relativeLocation(world, centerX, ridgeY - 1, centerZ, direction, 3, f);
                Block lanternBlock = world.getBlockAt(lanternLoc);
                lanternBlock.setType(Material.LANTERN, false);
                BlockData lanternData = lanternBlock.getBlockData();
                if (lanternData instanceof Lantern lantern) {
                    lantern.setHanging(true);
                    lanternBlock.setBlockData(lantern, false);
                }
            }
        } else {
            int lightY = switch (preset) {
                case OPEN -> centerY + 1;
                case FENCED -> centerY + 2;
                default -> centerY + 6;
            };
            placeLight(world, centerX, lightY, centerZ, direction, 0, 0);
            placeLight(world, centerX, lightY, centerZ, direction, 6, 0);
            placeLight(world, centerX, lightY, centerZ, direction, 0, 6);
            placeLight(world, centerX, lightY, centerZ, direction, 6, 6);
        }

        fixWallConnections(world, railingBlocks);

        // Create chest next to NPC
        Location chestLocation = relativeLocation(world, centerX, centerY + 1, centerZ, direction, 3, 1);
        Block chestBlock = world.getBlockAt(chestLocation);
        chestBlock.setType(Material.CHEST, false);
        protectedBlocks.add(BlockPoint.fromLocation(chestLocation));
        BlockPoint chestPoint = BlockPoint.fromLocation(chestLocation);

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

        return new BuildResult(villager.getUniqueId(), protectedBlocks, chestPoint);
    }

    private BuildResult buildCustomPlatform(World world, BlockPoint center, DirectionPair direction, CustomPresetData customPreset) {
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        List<BlockPoint> protectedBlocks = new ArrayList<>();
        List<BlockPoint> railingBlocks = new ArrayList<>();

        // Clear the 7x7x7 area first
        for (int r = 0; r < 7; r++) {
            for (int f = 0; f < 7; f++) {
                int x = centerX + direction.rightX * (r - 3) + direction.forwardX * (f - 3);
                int z = centerZ + direction.rightZ * (r - 3) + direction.forwardZ * (f - 3);

                for (int y = centerY; y <= centerY + 6; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    protectedBlocks.add(new BlockPoint(x, y, z));
                }
            }
        }

        // Compute rotation delta: how many CW steps to rotate block data so it matches the new placement direction.
        // build direction facing (player's actual facing at build time) = direction.openingFaceOpposite()
        int buildCwSteps = facingToCwSteps(direction.openingFaceOpposite());
        int saveCwSteps = customPreset.getSaveDirectionCwSteps();
        int rotateDelta = (buildCwSteps - saveCwSteps + 4) % 4;

        // Place custom blocks from the preset data
        if (customPreset.getBlocks() != null) {
            for (CustomPresetData.BlockEntry entry : customPreset.getBlocks()) {
                int rx = entry.getX();
                int ry = entry.getY();
                int rz = entry.getZ();
                Material material = Material.matchMaterial(entry.getMaterial());

                if (material == null) {
                    plugin.getLogger().warning("Unknown material in custom preset: " + entry.getMaterial());
                    continue;
                }

                // Rotate custom templates 180 degrees to match built-in platform orientation.
                int worldX = centerX + direction.rightX * (3 - rx) + direction.forwardX * (3 - rz);
                int worldY = centerY + ry;
                int worldZ = centerZ + direction.rightZ * (3 - rx) + direction.forwardZ * (3 - rz);

                Block block = world.getBlockAt(worldX, worldY, worldZ);
                String serializedData = entry.getBlockData();
                if (serializedData != null && !serializedData.isBlank()) {
                    try {
                        BlockData blockData = Bukkit.createBlockData(serializedData);
                        rotateBlockDataCw(blockData, rotateDelta);
                        block.setBlockData(blockData, false);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Failed to parse custom block data '" + serializedData + "': " + ex.getMessage());
                        block.setType(material, false);
                    }
                } else {
                    block.setType(material, false);
                }
                if (material == Material.BRICK_WALL || material.name().endsWith("_WALL") || material.name().endsWith("_FENCE")) {
                    railingBlocks.add(new BlockPoint(worldX, worldY, worldZ));
                }
                protectedBlocks.add(new BlockPoint(worldX, worldY, worldZ));
            }
        }

        // Spawn and NPC marker blocks are standardized for all platforms.
        world.getBlockAt(relativeLocation(world, centerX, centerY, centerZ, direction, 3, 3)).setType(materials.spawn(), false);
        world.getBlockAt(relativeLocation(world, centerX, centerY, centerZ, direction, 3, 2)).setType(materials.npc(), false);

        fixWallConnections(world, railingBlocks);

        // Create NPC at same position logic as built-in platforms.
        Location chestLocation = relativeLocation(world, centerX, centerY + 1, centerZ, direction, 3, 1);
        Block chestBlock = world.getBlockAt(chestLocation);
        chestBlock.setType(Material.CHEST, false);
        protectedBlocks.add(BlockPoint.fromLocation(chestLocation));
        BlockPoint chestPoint = BlockPoint.fromLocation(chestLocation);

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

        return new BuildResult(villager.getUniqueId(), protectedBlocks, chestPoint);
    }

    private void fixWallConnections(World world, List<BlockPoint> railingBlocks) {
        for (BlockPoint block : railingBlocks) {
            Block b = world.getBlockAt(block.getX(), block.getY(), block.getZ());
            BlockData data = b.getBlockData();
            if (data instanceof Wall wall) {
                wall.setHeight(BlockFace.NORTH, isConnectableSide(world, block, BlockFace.NORTH) ? Height.LOW : Height.NONE);
                wall.setHeight(BlockFace.SOUTH, isConnectableSide(world, block, BlockFace.SOUTH) ? Height.LOW : Height.NONE);
                wall.setHeight(BlockFace.EAST, isConnectableSide(world, block, BlockFace.EAST) ? Height.LOW : Height.NONE);
                wall.setHeight(BlockFace.WEST, isConnectableSide(world, block, BlockFace.WEST) ? Height.LOW : Height.NONE);
                b.setBlockData(wall, false);
            } else if (data instanceof Fence fence) {
                fence.setFace(BlockFace.NORTH, isConnectableSide(world, block, BlockFace.NORTH));
                fence.setFace(BlockFace.SOUTH, isConnectableSide(world, block, BlockFace.SOUTH));
                fence.setFace(BlockFace.EAST, isConnectableSide(world, block, BlockFace.EAST));
                fence.setFace(BlockFace.WEST, isConnectableSide(world, block, BlockFace.WEST));
                b.setBlockData(fence, false);
            }
        }
    }

    private boolean isConnectableSide(World world, BlockPoint origin, BlockFace face) {
        Block neighbor = world.getBlockAt(origin.getX() + face.getModX(), origin.getY(), origin.getZ() + face.getModZ());
        Material material = neighbor.getType();
        if (material.isAir()) {
            return false;
        }
        if (material.isSolid()) {
            return true;
        }
        String name = material.name();
        return name.endsWith("_FENCE") || name.endsWith("_WALL") || name.endsWith("_FENCE_GATE");
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

    private record BuildResult(UUID npcUuid, List<BlockPoint> protectedBlocks, BlockPoint chestLocation) {
    }

    private record DirectionPair(int forwardX, int forwardZ, int rightX, int rightZ) {
        BlockFace openingFace() {
            if (forwardX == 1) {
                return BlockFace.EAST;
            }
            if (forwardX == -1) {
                return BlockFace.WEST;
            }
            if (forwardZ == 1) {
                return BlockFace.SOUTH;
            }
            return BlockFace.NORTH;
        }

        BlockFace openingFaceOpposite() {
            if (forwardX == 1) {
                return BlockFace.WEST;
            }
            if (forwardX == -1) {
                return BlockFace.EAST;
            }
            if (forwardZ == 1) {
                return BlockFace.NORTH;
            }
            return BlockFace.SOUTH;
        }

        BlockFace rightFace() {
            if (rightX == 1) {
                return BlockFace.EAST;
            }
            if (rightX == -1) {
                return BlockFace.WEST;
            }
            if (rightZ == 1) {
                return BlockFace.SOUTH;
            }
            return BlockFace.NORTH;
        }

        BlockFace rightFaceOpposite() {
            if (rightX == 1) {
                return BlockFace.WEST;
            }
            if (rightX == -1) {
                return BlockFace.EAST;
            }
            if (rightZ == 1) {
                return BlockFace.NORTH;
            }
            return BlockFace.SOUTH;
        }

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
