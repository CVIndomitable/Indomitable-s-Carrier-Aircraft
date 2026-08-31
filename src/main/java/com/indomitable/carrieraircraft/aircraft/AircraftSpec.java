package com.indomitable.carrieraircraft.aircraft;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.EnumSet;
import java.util.Objects;
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
    private static final int MAX_AMMO_CAPACITY = 100_000;

    /**
     * 紧凑构造器只执行不可空 / 集合不可变等结构性检查。
     *
     * <p>数值范围校验（{@link #requireRange} / {@link #requireIntRange}）已迁移到
     * {@link AircraftSpecLoader#parseSpec}——只在 JSON 数据包解析阶段强校验。
     * 来自 NBT 不可信数据的恢复走宽松模式（{@link #load} 中夹紧到边界值），
     * 避免单字段越界就导致整个 entity NBT 读取失败。
     */
    public AircraftSpec {
        airWeaponMode = Objects.requireNonNull(airWeaponMode, "airWeaponMode");
        seaAttackModes = Set.copyOf(Objects.requireNonNull(seaAttackModes, "seaAttackModes"));
        allowedSeaAmmo = Set.copyOf(Objects.requireNonNull(allowedSeaAmmo, "allowedSeaAmmo"));
        if (seaAttackModes.isEmpty() || allowedSeaAmmo.isEmpty()) {
            throw new IllegalArgumentException("Aircraft attack modes and allowed ammunition must not be empty");
        }
        // NONE 机型不允许挂载可攻击弹药
        if (airWeaponMode == com.indomitable.carrieraircraft.aircraft.AirWeaponMode.NONE
                && magazineCapacity > 0) {
            throw new IllegalArgumentException(
                    "airWeaponMode.NONE requires magazineCapacity == 0, got " + magazineCapacity);
        }
    }

    /**
     * 数据包加载阶段使用的强校验：所有数值都必须在合理范围内，
     * 否则视为数据错误抛出 {@link IllegalArgumentException}。
     *
     * <p>由 {@link AircraftSpecLoader#parseSpec} 调用；NBT 加载不走此方法。
     */
    public AircraftSpec validated() {
        requireRange("speed", speed, 0.01, 4.0);
        requireRange("standbyHeight", standbyHeight, 0.0, 512.0);
        requireRange("attackHeight", attackHeight, 0.0, 512.0);
        requireRange("attackRange", attackRange, 0.0, 512.0);
        requireRange("turnDistance", turnDistance, 0.0, 2048.0);
        requireRange("health", health, 1.0, 2048.0);
        requireRange("weaponDamage", weaponDamage, 0.0, 2048.0);
        requireRange("explosionRadius", explosionRadius, 0.0, 64.0);
        requireRange("turretRange", turretRange, 0.0, 512.0);
        requireRange("aswRange", aswRange, 0.0, 512.0);
        requireIntRange("seaAmmoCapacity", seaAmmoCapacity, 0, MAX_AMMO_CAPACITY);
        requireIntRange("magazineCapacity", magazineCapacity, 0, MAX_AMMO_CAPACITY);
        requireIntRange("burstSize", burstSize, 1, MAX_AMMO_CAPACITY);
        return this;
    }

    private static void requireRange(String name, double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " must be finite and in [" + min + ", " + max + "]");
        }
    }

    private static void requireIntRange(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be in [" + min + ", " + max + "]");
        }
    }

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

    /**
     * 从 NBT 恢复规格。字段缺失或越界时安全夹紧到边界值，而不抛异常。
     *
     * <p>NBT 来自不可信数据（玩家可能用 mod 改存储），任何字段越界都直接抛
     * {@link IllegalArgumentException} 会导致 entity 整体加载失败、丢失实例。
     * 这里采用宽松模式：缺失字段用 {@link AircraftSpecLoader} 当前默认机型兜底，
     * 越界字段夹紧到合法区间。
     */
    public static AircraftSpec load(CompoundTag tag) {
        AircraftSpec defaults = AircraftSpecLoader.getInstance().getSpec("b25");
        return new AircraftSpec(
                clampDouble("Speed", getOrDefaultDouble(tag, "Speed", defaults::speed), 0.01, 4.0),
                clampDouble("StandbyHeight", getOrDefaultDouble(tag, "StandbyHeight", defaults::standbyHeight), 0.0, 512.0),
                clampDouble("AttackHeight", getOrDefaultDouble(tag, "AttackHeight", defaults::attackHeight), 0.0, 512.0),
                clampDouble("AttackRange", getOrDefaultDouble(tag, "AttackRange", defaults::attackRange), 0.0, 512.0),
                clampDouble("TurnDistance", getOrDefaultDouble(tag, "TurnDistance", defaults::turnDistance), 0.0, 2048.0),
                clampDouble("Health", getOrDefaultDouble(tag, "Health", defaults::health), 1.0, 2048.0),
                clampInt("SeaAmmoCapacity", getOrDefaultInt(tag, "SeaAmmoCapacity", defaults::seaAmmoCapacity), 0, MAX_AMMO_CAPACITY),
                clampInt("MagazineCapacity", getOrDefaultInt(tag, "MagazineCapacity", defaults::magazineCapacity), 0, MAX_AMMO_CAPACITY),
                clampInt("BurstSize", getOrDefaultInt(tag, "BurstSize", defaults::burstSize), 1, MAX_AMMO_CAPACITY),
                clampFloat("WeaponDamage", getOrDefaultFloat(tag, "WeaponDamage", defaults::weaponDamage), 0.0F, 2048.0F),
                clampFloat("ExplosionRadius", getOrDefaultFloat(tag, "ExplosionRadius", defaults::explosionRadius), 0.0F, 64.0F),
                clampDouble("TurretRange", getOrDefaultDouble(tag, "TurretRange", defaults::turretRange), 0.0, 512.0),
                clampDouble("AswRange", getOrDefaultDouble(tag, "AswRange", defaults::aswRange), 0.0, 512.0),
                loadEnum(tag, "AirWeaponMode", AirWeaponMode.class, defaults.airWeaponMode),
                tag.contains("SeaAttackModes") ? loadSeaAttackModes(tag) : defaults.seaAttackModes,
                tag.contains("AllowedSeaAmmo") ? loadAllowedSeaAmmo(tag) : defaults.allowedSeaAmmo
        );
    }

    private static double getOrDefaultDouble(CompoundTag tag, String key, java.util.function.DoubleSupplier fallback) {
        return tag.contains(key) ? tag.getDouble(key) : fallback.getAsDouble();
    }

    private static int getOrDefaultInt(CompoundTag tag, String key, java.util.function.IntSupplier fallback) {
        return tag.contains(key) ? tag.getInt(key) : fallback.getAsInt();
    }

    private static float getOrDefaultFloat(CompoundTag tag, String key, java.util.function.Supplier<Float> fallback) {
        return tag.contains(key) ? tag.getFloat(key) : fallback.get();
    }

    private static double clampDouble(String name, double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        if (value < min) {
            IndomitableCarrierAircraft.LOGGER.warn("Aircraft spec {}={} below minimum {}, clamped", name, value, min);
            return min;
        }
        if (value > max) {
            IndomitableCarrierAircraft.LOGGER.warn("Aircraft spec {}={} above maximum {}, clamped", name, value, max);
            return max;
        }
        return value;
    }

    private static int clampInt(String name, int value, int min, int max) {
        if (value < min) {
            IndomitableCarrierAircraft.LOGGER.warn("Aircraft spec {}={} below minimum {}, clamped", name, value, min);
            return min;
        }
        if (value > max) {
            IndomitableCarrierAircraft.LOGGER.warn("Aircraft spec {}={} above maximum {}, clamped", name, value, max);
            return max;
        }
        return value;
    }

    private static float clampFloat(String name, float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        if (value < min) {
            IndomitableCarrierAircraft.LOGGER.warn("Aircraft spec {}={} below minimum {}, clamped", name, value, min);
            return min;
        }
        if (value > max) {
            IndomitableCarrierAircraft.LOGGER.warn("Aircraft spec {}={} above maximum {}, clamped", name, value, max);
            return max;
        }
        return value;
    }

    private static <E extends Enum<E>> E loadEnum(CompoundTag tag, String key, Class<E> type, E fallback) {
        if (!tag.contains(key)) return fallback;
        try { return Enum.valueOf(type, tag.getString(key)); }
        catch (IllegalArgumentException ignored) { return fallback; }
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
