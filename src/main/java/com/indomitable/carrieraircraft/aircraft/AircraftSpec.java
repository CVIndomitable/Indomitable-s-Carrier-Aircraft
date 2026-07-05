package com.indomitable.carrieraircraft.aircraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.EnumSet;
import java.util.Set;

/**
 * 机型数据表。
 *
 * <p>后续可以由 JSON 数据包覆盖，这里先提供内置默认值保证模组可玩。
 * 所有机型共用同一实体类型，行为完全由 {@link #airWeaponMode()} + 对海槽弹药类型决定。
 *
 * <p>standbyHeight 与 attackHeight 都是相对高度差：前者相对玩家，后者相对目标。
 */
public record AircraftSpec(
        double speed,
        double standbyHeight,
        double attackHeight,
        double attackRange,
        double turnDistance,
        double health,
        int seaAmmoCapacity,
        int magazineCapacity,
        int burstSize,
        float weaponDamage,
        float explosionRadius,
        double turretRange,
        double aswRange,
        AirWeaponMode airWeaponMode,
        Set<SeaAttackMode> seaAttackModes,
        Set<AmmoType> allowedSeaAmmo
) {
    // ==================== 内置规格 ====================

    /** B-25：水平轰炸机，前射+自卫炮塔，航弹 */
    public static final AircraftSpec B25 = new AircraftSpec(
            0.55, 22.0, 36.0, 18.0, 64.0, 60.0,
            6, 1000, 2, 30.0F, 4.0F, 28.0, 0.0,
            AirWeaponMode.BOTH,
            EnumSet.of(SeaAttackMode.LEVEL_BOMBING),
            EnumSet.of(AmmoType.AERIAL_BOMB)
    );

    /** BTD：俯冲轰炸机，前射，航弹/鱼雷 */
    public static final AircraftSpec BTD = new AircraftSpec(
            0.68, 18.0, 20.0, 14.0, 54.0, 45.0,
            4, 1000, 1, 24.0F, 3.0F, 0.0, 0.0,
            AirWeaponMode.FORWARD,
            EnumSet.of(SeaAttackMode.DIVE_BOMBING, SeaAttackMode.TORPEDO),
            EnumSet.of(AmmoType.AERIAL_BOMB, AmmoType.AERIAL_TORPEDO)
    );

    /** 火箭攻击机：前射，火箭弹 */
    public static final AircraftSpec ROCKET_ATTACKER = new AircraftSpec(
            0.72, 18.0, 16.0, 20.0, 58.0, 40.0,
            8, 1000, 4, 22.0F, 2.8F, 0.0, 0.0,
            AirWeaponMode.FORWARD,
            EnumSet.of(SeaAttackMode.ROCKET),
            EnumSet.of(AmmoType.ROCKET)
    );

    /** 反潜巡逻机：无对空，航弹，目标类型=水下 */
    public static final AircraftSpec ASW_PATROL = new AircraftSpec(
            0.50, 20.0, 24.0, 16.0, 56.0, 42.0,
            5, 0, 1, 18.0F, 3.5F, 0.0, 48.0,
            AirWeaponMode.NONE,
            EnumSet.of(SeaAttackMode.ASW_BOMBING),
            EnumSet.of(AmmoType.AERIAL_BOMB)
    );

    // ==================== 查询方法 ====================

    public boolean supportsAmmo(AmmoType ammoType) {
        return allowedSeaAmmo.contains(ammoType);
    }

    public boolean canAttackAir() {
        return airWeaponMode != AirWeaponMode.NONE && magazineCapacity > 0;
    }

    // ==================== NBT 序列化 ====================

    /** 将规格写入 NBT */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Speed", speed);
        tag.putDouble("StandbyHeight", standbyHeight);
        tag.putDouble("AttackHeight", attackHeight);
        tag.putDouble("AttackRange", attackRange);
        tag.putDouble("TurnDistance", turnDistance);
        tag.putDouble("Health", health);
        tag.putInt("SeaAmmoCapacity", seaAmmoCapacity);
        tag.putInt("MagazineCapacity", magazineCapacity);
        tag.putInt("BurstSize", burstSize);
        tag.putFloat("WeaponDamage", weaponDamage);
        tag.putFloat("ExplosionRadius", explosionRadius);
        tag.putDouble("TurretRange", turretRange);
        tag.putDouble("AswRange", aswRange);
        tag.putString("AirWeaponMode", airWeaponMode.name());

        ListTag seaModes = new ListTag();
        for (SeaAttackMode mode : seaAttackModes) seaModes.add(StringTag.valueOf(mode.name()));
        tag.put("SeaAttackModes", seaModes);

        ListTag allowedAmmo = new ListTag();
        for (AmmoType ammo : allowedSeaAmmo) allowedAmmo.add(StringTag.valueOf(ammo.name()));
        tag.put("AllowedSeaAmmo", allowedAmmo);

        return tag;
    }

    /** 从 NBT 恢复规格，字段缺失时使用 B25 默认值 */
    public static AircraftSpec load(CompoundTag tag) {
        return new AircraftSpec(
                tag.contains("Speed") ? tag.getDouble("Speed") : B25.speed,
                tag.contains("StandbyHeight") ? tag.getDouble("StandbyHeight") : B25.standbyHeight,
                tag.contains("AttackHeight") ? tag.getDouble("AttackHeight") : B25.attackHeight,
                tag.contains("AttackRange") ? tag.getDouble("AttackRange") : B25.attackRange,
                tag.contains("TurnDistance") ? tag.getDouble("TurnDistance") : B25.turnDistance,
                tag.contains("Health") ? tag.getDouble("Health") : B25.health,
                tag.contains("SeaAmmoCapacity") ? tag.getInt("SeaAmmoCapacity") : B25.seaAmmoCapacity,
                tag.contains("MagazineCapacity") ? tag.getInt("MagazineCapacity") : B25.magazineCapacity,
                tag.contains("BurstSize") ? tag.getInt("BurstSize") : B25.burstSize,
                tag.contains("WeaponDamage") ? tag.getFloat("WeaponDamage") : B25.weaponDamage,
                tag.contains("ExplosionRadius") ? tag.getFloat("ExplosionRadius") : B25.explosionRadius,
                tag.contains("TurretRange") ? tag.getDouble("TurretRange") : B25.turretRange,
                tag.contains("AswRange") ? tag.getDouble("AswRange") : B25.aswRange,
                tag.contains("AirWeaponMode") ? AirWeaponMode.valueOf(tag.getString("AirWeaponMode")) : B25.airWeaponMode,
                tag.contains("SeaAttackModes") ? loadSeaAttackModes(tag) : B25.seaAttackModes,
                tag.contains("AllowedSeaAmmo") ? loadAllowedSeaAmmo(tag) : B25.allowedSeaAmmo
        );
    }

    private static Set<SeaAttackMode> loadSeaAttackModes(CompoundTag tag) {
        Set<SeaAttackMode> modes = EnumSet.noneOf(SeaAttackMode.class);
        ListTag list = tag.getList("SeaAttackModes", 8); // 8 = TAG_STRING
        for (int i = 0; i < list.size(); i++) {
            try { modes.add(SeaAttackMode.valueOf(list.getString(i))); }
            catch (IllegalArgumentException ignored) {}
        }
        return modes;
    }

    private static Set<AmmoType> loadAllowedSeaAmmo(CompoundTag tag) {
        Set<AmmoType> ammo = EnumSet.noneOf(AmmoType.class);
        ListTag list = tag.getList("AllowedSeaAmmo", 8);
        for (int i = 0; i < list.size(); i++) {
            try { ammo.add(AmmoType.valueOf(list.getString(i))); }
            catch (IllegalArgumentException ignored) {}
        }
        return ammo;
    }
}
