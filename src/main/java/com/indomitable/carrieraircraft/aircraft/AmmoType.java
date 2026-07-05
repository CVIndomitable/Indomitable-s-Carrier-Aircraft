package com.indomitable.carrieraircraft.aircraft;

public enum AmmoType {
    MAGAZINE,
    AERIAL_BOMB,
    AERIAL_TORPEDO,
    ROCKET;

    public boolean isSeaWeapon() {
        return this == AERIAL_BOMB || this == AERIAL_TORPEDO || this == ROCKET;
    }
}
