package com.indomitable.carrieraircraft.aircraft;

public enum AssignmentMode {
    FOCUS("集火"),
    BALANCED("均衡");

    private final String displayName;

    AssignmentMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public AssignmentMode next() {
        return this == FOCUS ? BALANCED : FOCUS;
    }
}
