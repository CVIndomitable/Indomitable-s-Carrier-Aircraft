package com.indomitable.carrieraircraft.aircraft;

import net.minecraft.nbt.CompoundTag;

public final class WeaponSlot {
    private AmmoType ammoType;
    private int count;
    private final int capacity;

    public WeaponSlot(AmmoType ammoType, int count, int capacity) {
        this.ammoType = ammoType;
        this.count = Math.max(0, count);
        this.capacity = Math.max(0, capacity);
        clamp();
    }

    public AmmoType ammoType() {
        return ammoType;
    }

    public void setAmmoType(AmmoType ammoType) {
        this.ammoType = ammoType;
    }

    public int count() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
        clamp();
    }

    public int capacity() {
        return capacity;
    }

    public boolean hasAmmo() {
        return count > 0;
    }

    public int consume(int requested) {
        int consumed = Math.min(Math.max(0, requested), count);
        count -= consumed;
        return consumed;
    }

    public int add(int amount) {
        int accepted = Math.min(Math.max(0, amount), Math.max(0, capacity - count));
        count += accepted;
        return accepted;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("AmmoType", ammoType.name());
        tag.putInt("Count", count);
        tag.putInt("Capacity", capacity);
        return tag;
    }

    public static WeaponSlot load(CompoundTag tag, AmmoType fallbackType, int fallbackCapacity) {
        AmmoType type = fallbackType;
        if (tag.contains("AmmoType")) {
            try {
                type = AmmoType.valueOf(tag.getString("AmmoType"));
            } catch (IllegalArgumentException ignored) {
                type = fallbackType;
            }
        }
        int capacity = tag.contains("Capacity") ? tag.getInt("Capacity") : fallbackCapacity;
        return new WeaponSlot(type, tag.getInt("Count"), capacity);
    }

    private void clamp() {
        if (capacity > 0) {
            count = Math.min(count, capacity);
        }
    }
}
