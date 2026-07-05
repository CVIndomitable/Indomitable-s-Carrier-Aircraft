package com.indomitable.carrieraircraft.aircraft;

public enum AirDefenseMode {
    SELF_DEFENSE("自卫"),
    ACTIVE("主动"),
    LOW_AGGRESSION("管控");

    private final String displayName;

    AirDefenseMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public AirDefenseMode next() {
        AirDefenseMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
