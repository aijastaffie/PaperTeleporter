package com.paperteleporter;

import java.util.ArrayList;
import java.util.List;

public final class CustomPresetData {
    private int presetNumber;
    private String name;
    private List<BlockEntry> blocks;

    public CustomPresetData() {
        this.blocks = new ArrayList<>();
    }

    public CustomPresetData(int presetNumber, String name, List<BlockEntry> blocks) {
        this.presetNumber = presetNumber;
        this.name = name;
        this.blocks = new ArrayList<>(blocks);
    }

    public int getPresetNumber() {
        return presetNumber;
    }

    public void setPresetNumber(int presetNumber) {
        this.presetNumber = presetNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BlockEntry> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BlockEntry> blocks) {
        this.blocks = new ArrayList<>(blocks);
    }

    public static final class BlockEntry {
        private int x;
        private int y;
        private int z;
        private String material;

        public BlockEntry() {
        }

        public BlockEntry(int x, int y, int z, String material) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
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

        public String getMaterial() {
            return material;
        }
    }
}
