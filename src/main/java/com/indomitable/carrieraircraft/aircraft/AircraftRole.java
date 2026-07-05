package com.indomitable.carrieraircraft.aircraft;

import java.util.Set;

/**
 * 机型角色枚举 —— 由武器槽配置推导，不硬编码。
 *
 * <p>推导规则参见 {@link #derive(AirWeaponMode, AmmoType, AircraftSpec)}。
 * 机型能力完全由 {@link AircraftSpec} + 武器槽决定，此枚举仅用于显示和分类。
 */
public enum AircraftRole {
    /** 水平轰炸机：前射+自卫炮塔 + 航弹 */
    BOMBER("bomber", "水平轰炸机", SquadronType.BOMBER),
    /** 俯冲轰炸机：前射 + 航弹（spec 支持俯冲） */
    DIVE_BOMBER("dive_bomber", "俯冲轰炸机", SquadronType.BOMBER),
    /** 鱼雷轰炸机：前射 + 鱼雷 */
    TORPEDO_BOMBER("torpedo_bomber", "鱼雷轰炸机", SquadronType.MULTIROLE),
    /** 火箭攻击机：前射 + 火箭弹 */
    ROCKET_ATTACKER("rocket_attacker", "火箭攻击机", SquadronType.MULTIROLE),
    /** 反潜巡逻机：无对空 + 航弹（目标类型=水下） */
    ASW_PATROL("asw_patrol", "反潜巡逻机", SquadronType.ASW);

    private final String id;
    private final String displayName;
    private final SquadronType squadronType;

    AircraftRole(String id, String displayName, SquadronType squadronType) {
        this.id = id;
        this.displayName = displayName;
        this.squadronType = squadronType;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public SquadronType squadronType() { return squadronType; }

    /**
     * 根据武器槽配置推导机型角色。
     *
     * @param airMode    对空武器模式（NONE / FORWARD / TURRET / BOTH）
     * @param seaAmmo    对海槽弹药类型
     * @param spec       飞机规格（用于区分水平/俯冲轰炸）
     * @return 推导出的机型角色
     */
    public static AircraftRole derive(AirWeaponMode airMode, AmmoType seaAmmo, AircraftSpec spec) {
        // 反潜机：无对空能力 + 航弹 + 有反潜范围
        if (airMode == AirWeaponMode.NONE && seaAmmo == AmmoType.AERIAL_BOMB && spec.aswRange() > 0) {
            return ASW_PATROL;
        }
        // 对海弹药决定主要角色
        return switch (seaAmmo) {
            case AERIAL_TORPEDO -> TORPEDO_BOMBER;
            case ROCKET -> ROCKET_ATTACKER;
            case AERIAL_BOMB -> {
                // 区分水平轰炸和俯冲轰炸
                if (spec.seaAttackModes().contains(SeaAttackMode.DIVE_BOMBING)) {
                    yield DIVE_BOMBER;
                }
                yield BOMBER;
            }
            default -> BOMBER;
        };
    }

    /**
     * 根据角色 ID 查找枚举值，找不到则返回 BOMBER。
     */
    public static AircraftRole byId(String id) {
        for (AircraftRole role : values()) {
            if (role.id.equals(id)) return role;
        }
        return BOMBER;
    }
}
