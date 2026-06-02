package com.indomitable.carrieraircraft.firecontrol;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 火控系统 - Phase 1 MVP 版本
 *
 * 功能：
 * - 每个玩家维护一个锁定目标位置
 * - 提供目标设置和查询接口
 *
 * Phase 1 简化：
 * - 仅支持位置锁定（不支持实体锁定）
 * - 仅支持单一目标
 * - 无档位系统
 */
public class FireControlSystem {
    private static final FireControlSystem INSTANCE = new FireControlSystem();

    /** 玩家 UUID -> 锁定目标位置 */
    private final Map<UUID, Vec3> targetMap = new HashMap<>();

    private FireControlSystem() {}

    public static FireControlSystem getInstance() {
        return INSTANCE;
    }

    /**
     * 设置玩家的锁定目标
     * @param player 玩家
     * @param target 目标位置（null 表示清除锁定）
     */
    public void setTarget(ServerPlayer player, @Nullable Vec3 target) {
        UUID playerUUID = player.getUUID();

        if (target == null) {
            targetMap.remove(playerUUID);
        } else {
            targetMap.put(playerUUID, target);
        }
    }

    /**
     * 获取玩家的锁定目标
     * @param playerUUID 玩家 UUID
     * @return 锁定目标位置，如果没有锁定则返回 null
     */
    @Nullable
    public Vec3 getTarget(UUID playerUUID) {
        return targetMap.get(playerUUID);
    }

    /**
     * 检查玩家是否已锁定目标
     */
    public boolean hasTarget(UUID playerUUID) {
        return targetMap.containsKey(playerUUID);
    }

    /**
     * 清除玩家的锁定目标
     */
    public void clearTarget(UUID playerUUID) {
        targetMap.remove(playerUUID);
    }

    /**
     * 清除所有锁定（通常在服务器关闭时调用）
     */
    public void clearAll() {
        targetMap.clear();
    }

    /**
     * 便捷方法：使用玩家的视线方向设置目标
     * 射线检测找到玩家看向的方块位置
     */
    public void setTargetFromLookDirection(ServerPlayer player, double maxDistance) {
        var hitResult = player.pick(maxDistance, 0, false);
        if (hitResult != null) {
            Vec3 target = hitResult.getLocation();
            setTarget(player, target);
        }
    }
}
