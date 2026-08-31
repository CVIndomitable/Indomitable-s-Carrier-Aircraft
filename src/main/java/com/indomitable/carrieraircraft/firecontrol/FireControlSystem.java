package com.indomitable.carrieraircraft.firecontrol;

import com.indomitable.carrieraircraft.aircraft.AssignmentMode;
import com.indomitable.carrieraircraft.data.CarrierAircraftSavedData;
import com.indomitable.carrieraircraft.targeting.FriendlyFireFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 火控系统。
 *
 * <p>支持玩家锁定最多 4 个目标，目标可以是实体或坐标。控制终端和飞机 AI 均从这里读取目标列表。
 */
public class FireControlSystem {
    public static final int MAX_TARGETS = 4;
    public static final double MAX_COORDINATE_TARGET_DISTANCE = 240.0;
    /** 设置盘旋点时玩家视线最大采样距离（与 {@link #MAX_COORDINATE_TARGET_DISTANCE} 共享语义）。 */
    public static final double RALLY_POINT_LOOK_DISTANCE = MAX_COORDINATE_TARGET_DISTANCE;
    /** 盘旋点相对采样点的抬高距离。 */
    public static final double RALLY_POINT_HEIGHT_OFFSET = 18.0;

    /** 坐标目标的有效期（5 分钟）。实体目标随实体死亡而失效，不受此限制。 */
    public static final long POSITION_TARGET_TTL_TICKS = 20L * 60L * 5L;

    private static final FireControlSystem INSTANCE = new FireControlSystem();

    private final Map<UUID, List<FireControlTarget>> targetMap = new HashMap<>();
    private final Map<UUID, PlayerAirControlSettings> settingsMap = new HashMap<>();

    private FireControlSystem() {}

    public static FireControlSystem getInstance() {
        return INSTANCE;
    }

    /**
     * 取得（或惰性加载）玩家的火控设置。
     *
     * <p>统一从此方法获取，避免出现「服务端用真实设置、客户端用默认值」的不一致。
     * 必须传入 {@link ServerLevel}，因为冷启动时需要从存档数据加载玩家自定义设置。
     * 所有调用方均在服务端主线程，{@code settingsMap} 的并发安全由 Minecraft 主线程模型保证。
     */
    public PlayerAirControlSettings settings(ServerLevel level, UUID playerId) {
        return settingsMap.computeIfAbsent(playerId,
                id -> CarrierAircraftSavedData.get(level).loadSettings(id));
    }

    public void saveSettings(ServerLevel level, UUID playerId) {
        PlayerAirControlSettings settings = settingsMap.get(playerId);
        if (settings != null) {
            CarrierAircraftSavedData.get(level).saveSettings(settings);
        }
    }

    public void addTarget(ServerPlayer player, FireControlTarget target) {
        List<FireControlTarget> targets = targetMap.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        if (targets.size() >= MAX_TARGETS) {
            targets.remove(0);
        }
        targets.add(target);
    }

    public void setSingleTarget(ServerPlayer player, FireControlTarget target) {
        List<FireControlTarget> targets = new ArrayList<>();
        targets.add(target);
        targetMap.put(player.getUUID(), targets);
    }

    public List<FireControlTarget> getTargets(UUID playerUUID) {
        return List.copyOf(targetMap.getOrDefault(playerUUID, List.of()));
    }

    @Nullable
    public FireControlTarget getAssignedTarget(ServerLevel level, UUID playerUUID, UUID aircraftUUID) {
        List<FireControlTarget> targets = getTargets(playerUUID);
        if (targets.isEmpty()) {
            return null;
        }

        AssignmentMode mode = settings(level, playerUUID).assignmentMode();
        if (mode == AssignmentMode.FOCUS || targets.size() == 1) {
            return targets.get(0);
        }

        int index = Math.floorMod(aircraftUUID.hashCode(), targets.size());
        return targets.get(index);
    }

    public void setTarget(ServerPlayer player, @Nullable Vec3 target) {
        if (target == null) {
            targetMap.remove(player.getUUID());
        } else {
            setSingleTarget(player, FireControlTarget.position(player.serverLevel(), target));
        }
    }

    /**
     * 清理失效目标：超时的坐标目标，以及已加载且确认死亡的实体目标。
     * 实体目标所在区块未加载时保留，避免误删仍然有效的目标。
     *
     * <p>每玩家每 40 tick 最多清理一次，避免每架飞机各自调用时形成 O(N²) 行为。
     */
    private static final long PRUNE_INTERVAL_TICKS = 40L;
    private final Map<UUID, Long> lastPruneTick = new HashMap<>();

    public void pruneStaleTargets(UUID playerId, ServerLevel level) {
        List<FireControlTarget> targets = targetMap.get(playerId);
        if (targets == null) {
            return;
        }
        long now = level.getGameTime();
        Long last = lastPruneTick.get(playerId);
        if (last != null && now - last < PRUNE_INTERVAL_TICKS) {
            return;
        }
        lastPruneTick.put(playerId, now);
        targets.removeIf(target -> {
            if (!target.inDimension(level)) {
                return false;
            }
            if (target.isEntityTarget()) {
                Entity entity = target.resolveEntity(level);
                return entity != null && !entity.isAlive();
            }
            return now - target.createdGameTime() > POSITION_TARGET_TTL_TICKS;
        });
        if (targets.isEmpty()) {
            targetMap.remove(playerId);
        }
    }

    /** 玩家下线时调用，防止静态单例长期持有离线玩家数据。 */
    public void clearPlayer(UUID playerId) {
        targetMap.remove(playerId);
        settingsMap.remove(playerId);
        lastPruneTick.remove(playerId);
    }

    @Nullable
    public Vec3 getTarget(UUID playerUUID) {
        List<FireControlTarget> targets = targetMap.get(playerUUID);
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        return targets.get(0).fallbackPosition();
    }

    public boolean hasTarget(UUID playerUUID) {
        List<FireControlTarget> targets = targetMap.get(playerUUID);
        return targets != null && !targets.isEmpty();
    }

    public void clearTarget(UUID playerUUID) {
        targetMap.remove(playerUUID);
    }

    /**
     * 删除指定索引的目标。
     *
     * @param playerUUID 玩家 UUID
     * @param index 目标索引（0-based）
     * @return 是否成功删除
     */
    public boolean removeTarget(UUID playerUUID, int index) {
        List<FireControlTarget> targets = targetMap.get(playerUUID);
        if (targets == null || index < 0 || index >= targets.size()) {
            return false;
        }
        targets.remove(index);
        if (targets.isEmpty()) {
            targetMap.remove(playerUUID);
        }
        return true;
    }

    public void clearAll() {
        targetMap.clear();
        settingsMap.clear();
        lastPruneTick.clear();
    }

    /**
     * 通过世界坐标添加打击目标（控制终端 GUI 用）。
     */
    public boolean addTargetByCoordinates(ServerPlayer player, double x, double y, double z) {
        Vec3 pos = new Vec3(x, y, z);
        if (!isValidCoordinateTarget(player, pos)) {
            return false;
        }
        addTarget(player, FireControlTarget.position(player.serverLevel(), pos));
        return true;
    }

    public boolean isValidCoordinateTarget(ServerPlayer player, Vec3 pos) {
        if (!Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos blockPos = BlockPos.containing(pos);
        return level.isInWorldBounds(blockPos)
                && level.getWorldBorder().isWithinBounds(blockPos)
                && player.distanceToSqr(pos) <= MAX_COORDINATE_TARGET_DISTANCE * MAX_COORDINATE_TARGET_DISTANCE;
    }

    /**
     * 使用玩家视线设置目标。优先锁定实体，没看中实体时锁定方块/坐标。
     */
    @Nullable
    public FireControlTarget setTargetFromLookDirection(ServerPlayer player, double maxDistance, boolean append) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level,
                player,
                eye,
                end,
                searchBox,
                entity -> FriendlyFireFilter.canPlayerTarget(player.getUUID(), entity)
        );

        FireControlTarget target;
        if (entityHit != null) {
            target = FireControlTarget.entity(entityHit.getEntity());
        } else {
            HitResult blockHit = player.pick(maxDistance, 0, false);
            if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) {
                return null;
            }
            target = FireControlTarget.position(level, blockHit.getLocation());
        }

        if (append) {
            addTarget(player, target);
        } else {
            setSingleTarget(player, target);
        }
        return target;
    }
}
