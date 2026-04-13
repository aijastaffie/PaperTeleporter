package com.paperteleporter;

public enum Preset {
    OPEN(1, "Open platform without railings"),
    FENCED(2, "Platform with fence railings and stairs"),
    ENCLOSED(3, "Platform with full enclosure using walls"),
    TOWERING(4, "Platform with tall corner pillars"),
    FLAT_ROOF(5, "Platform with flat ceiling"),
    GABLE_ROOF(6, "Platform with gabled pitched roof");

    private final int number;
    private final String description;

    Preset(int number, String description) {
        this.number = number;
        this.description = description;
    }

    public int getNumber() {
        return number;
    }

    public String getDescription() {
        return description;
    }

    public static Preset fromNumber(int number) {
        for (Preset preset : values()) {
            if (preset.number == number) {
                return preset;
            }
        }
        return FENCED; // default
    }

    public static boolean isValid(int number) {
        return number >= 1 && number <= 6;
    }
}
