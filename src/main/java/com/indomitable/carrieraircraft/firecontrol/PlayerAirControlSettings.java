package com.indomitable.carrieraircraft.firecontrol;

import com.indomitable.carrieraircraft.aircraft.AirDefenseMode;
import com.indomitable.carrieraircraft.aircraft.AssignmentMode;
import com.indomitable.carrieraircraft.aircraft.AutoLockMode;

import java.util.UUID;

public final class PlayerAirControlSettings {
    private final UUID playerId;
    private AutoLockMode autoLockMode = AutoLockMode.NEAREST;
    private AssignmentMode assignmentMode = AssignmentMode.FOCUS;
    private AirDefenseMode airDefenseMode = AirDefenseMode.SELF_DEFENSE;
    private int bombsPerPass = 1;
    private float minimumEffectiveDamage = 20.0F;

    public PlayerAirControlSettings(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }

    public AutoLockMode autoLockMode() {
        return autoLockMode;
    }

    public AutoLockMode cycleAutoLockMode() {
        this.autoLockMode = autoLockMode.next();
        return autoLockMode;
    }

    public void setAutoLockMode(AutoLockMode mode) {
        this.autoLockMode = mode;
    }

    public AssignmentMode assignmentMode() {
        return assignmentMode;
    }

    public AssignmentMode cycleAssignmentMode() {
        this.assignmentMode = assignmentMode.next();
        return assignmentMode;
    }

    public void setAssignmentMode(AssignmentMode mode) {
        this.assignmentMode = mode;
    }

    public AirDefenseMode airDefenseMode() {
        return airDefenseMode;
    }

    public AirDefenseMode cycleAirDefenseMode() {
        this.airDefenseMode = airDefenseMode.next();
        return airDefenseMode;
    }

    public void setAirDefenseMode(AirDefenseMode mode) {
        this.airDefenseMode = mode;
    }

    public int bombsPerPass() {
        return bombsPerPass;
    }

    public int cycleBombsPerPass() {
        this.bombsPerPass = bombsPerPass % 4 + 1;
        return bombsPerPass;
    }

    public void setBombsPerPass(int value) {
        this.bombsPerPass = Math.max(1, Math.min(4, value));
    }

    public float minimumEffectiveDamage() {
        return minimumEffectiveDamage;
    }

    public float cycleMinimumEffectiveDamage() {
        this.minimumEffectiveDamage = switch ((int) minimumEffectiveDamage) {
            case 0 -> 20.0F;
            case 20 -> 40.0F;
            case 40 -> 80.0F;
            default -> 0.0F;
        };
        return minimumEffectiveDamage;
    }

    public void setMinimumEffectiveDamage(float value) {
        this.minimumEffectiveDamage = value;
    }
}
