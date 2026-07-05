package com.indomitable.carrieraircraft.aircraft;

public enum AirWeaponMode {
    NONE,
    FORWARD,
    TURRET,
    BOTH;

    public boolean hasForwardGun() {
        return this == FORWARD || this == BOTH;
    }

    public boolean hasTurret() {
        return this == TURRET || this == BOTH;
    }
}
