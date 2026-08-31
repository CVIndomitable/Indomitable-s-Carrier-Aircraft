package com.indomitable.carrieraircraft.aircraft;

public enum AmmoType {
    MAGAZINE,
    AERIAL_BOMB,
    AERIAL_TORPEDO,
    ROCKET;

    public boolean isSeaWeapon() {
        return this == AERIAL_BOMB || this == AERIAL_TORPEDO || this == ROCKET;
    }

    public String translationKey() {
        return switch (this) {
            case MAGAZINE -> "ammo_type.indomitablecarrieraircraft.magazine";
            case AERIAL_BOMB -> "ammo_type.indomitablecarrieraircraft.aerial_bomb";
            case AERIAL_TORPEDO -> "ammo_type.indomitablecarrieraircraft.aerial_torpedo";
            case ROCKET -> "ammo_type.indomitablecarrieraircraft.rocket";
        };
    }
}
