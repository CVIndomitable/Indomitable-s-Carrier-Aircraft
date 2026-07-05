package com.indomitable.carrieraircraft.aircraft;

public enum AutoLockMode {
    NEAREST("最近"),
    STRONGEST("最强"),
    FOCUS("集火"),
    SPREAD("分散"),
    TYPE_FILTER("类型");

    private final String displayName;

    AutoLockMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public AutoLockMode next() {
        AutoLockMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
