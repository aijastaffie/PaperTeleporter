package com.paperteleporter;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PlatformData {
    private String id;
    private String worldName;
    private BlockPoint anchor;
    private BlockPoint spawnPoint;
    private UUID npcUuid;
    private List<BlockPoint> protectedBlocks;
    private int directionIndex;
    private int presetNumber;

    public PlatformData() {
    }

    public PlatformData(String id, String worldName, BlockPoint anchor, BlockPoint spawnPoint, UUID npcUuid, List<BlockPoint> protectedBlocks, int directionIndex, int presetNumber) {
        this.id = id;
        this.worldName = worldName;
        this.anchor = anchor;
        this.spawnPoint = spawnPoint;
        this.npcUuid = npcUuid;
        this.protectedBlocks = new ArrayList<>(protectedBlocks);
        this.directionIndex = directionIndex;
        this.presetNumber = presetNumber;
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public BlockPoint getAnchor() {
        return anchor;
    }

    public BlockPoint getSpawnPoint() {
        return spawnPoint;
    }

    public UUID getNpcUuid() {
        return npcUuid;
    }

    public List<BlockPoint> getProtectedBlocks() {
        return protectedBlocks;
    }

    public int getDirectionIndex() {
        return directionIndex;
    }

    public void setNpcUuid(UUID npcUuid) {
        this.npcUuid = npcUuid;
    }

    public void setProtectedBlocks(List<BlockPoint> protectedBlocks) {
        this.protectedBlocks = new ArrayList<>(protectedBlocks);
    }

    public void setDirectionIndex(int directionIndex) {
        this.directionIndex = directionIndex;
    }

    public int getPresetNumber() {
        return presetNumber;
    }

    public void setPresetNumber(int presetNumber) {
        this.presetNumber = presetNumber;
    }

    public Set<BlockPoint> protectedBlockSet() {
        return new HashSet<>(protectedBlocks);
    }

    public boolean isProtectedBlock(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        return protectedBlocks.contains(BlockPoint.fromLocation(location));
    }

    public Location spawnLocation(World world) {
        return spawnPoint.toLocation(world).add(0.5, 1.0, 0.5);
    }
}
