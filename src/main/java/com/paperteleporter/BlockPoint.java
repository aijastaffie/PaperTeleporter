package com.paperteleporter;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public final class BlockPoint {
    private int x;
    private int y;
    private int z;

    public BlockPoint() {
    }

    public BlockPoint(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockPoint fromLocation(Location location) {
        return new BlockPoint(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlockPoint that)) {
            return false;
        }
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
