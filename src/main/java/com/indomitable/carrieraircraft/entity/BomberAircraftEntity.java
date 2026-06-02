package com.indomitable.carrieraircraft.entity;

import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 水平轰炸机实体 - Phase 1 MVP
 *
 * 特性：
 * - 基础状态机（STANDBY、APPROACH、DROPPING、RETURNING）
 * - 简单路径飞行
 * - 自由落体投弹
 * - 弹药耗尽后返航
 */
public class BomberAircraftEntity extends FlyingMob {
    // ==================== 数据同步器 ====================
    private static final EntityDataAccessor<String> DATA_STATE =
            SynchedEntityData.defineId(BomberAircraftEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> DATA_AMMO_COUNT =
            SynchedEntityData.defineId(BomberAircraftEntity.class, EntityDataSerializers.INT);

    // ==================== 配置常量 ====================

    /** 飞行速度 (blocks/tick) */
    private static final double FLIGHT_SPEED = 0.5;

    /** 飞行高度（玩家头顶） */
    private static final double STANDBY_HEIGHT_OFFSET = 20.0;

    /** 投弹高度（目标上方） */
    private static final double BOMBING_HEIGHT = 80.0;

    /** 投弹间隔 (ticks) */
    private static final int BOMBING_INTERVAL = 20; // 1秒

    /** 炸弹爆炸半径 */
    private static final float BOMB_EXPLOSION_RADIUS = 4.0F;

    /** 炸弹伤害 */
    private static final float BOMB_DAMAGE = 30.0F;

    /** 到达目标的容差距离 */
    private static final double ARRIVAL_THRESHOLD = 3.0;

    // ==================== 状态数据 ====================

    /** 所属玩家 UUID */
    @Nullable
    private UUID ownerUUID;

    /** 目标位置（锁定目标的坐标） */
    @Nullable
    private Vec3 targetPosition;

    /** 投弹计时器 */
    private int bombingTimer = 0;

    public BomberAircraftEntity(EntityType<? extends BomberAircraftEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // 飞行实体不受重力
    }

    /** 便捷构造函数：创建飞机 */
    public static BomberAircraftEntity create(ServerLevel level, UUID ownerUUID, Vec3 spawnPos) {
        BomberAircraftEntity aircraft = new BomberAircraftEntity(ModEntityTypes.BOMBER_AIRCRAFT.get(), level);
        aircraft.setPos(spawnPos);
        aircraft.ownerUUID = ownerUUID;
        aircraft.setState(AircraftState.STANDBY);
        aircraft.setAmmoCount(6); // 默认6发炸弹
        return aircraft;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 128.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, AircraftState.STANDBY.name());
        builder.define(DATA_AMMO_COUNT, 6);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            updateStateMachine();
        }

        // 调试：每20 tick输出一次位置
        if (this.tickCount % 20 == 0) {
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.info(
                "Bomber aircraft {} at {} state: {} ammo: {}",
                this.getId(), this.position(), getState(), getAmmoCount()
            );
        }
    }

    /** 状态机更新 */
    private void updateStateMachine() {
        AircraftState state = getState();

        switch (state) {
            case STANDBY -> tickStandby();
            case APPROACH -> tickApproach();
            case DROPPING -> tickDropping();
            case RETURNING -> tickReturning();
        }
    }

    /** 待命状态：在玩家头顶盘旋 */
    private void tickStandby() {
        if (ownerUUID == null) {
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.warn("Aircraft {} has no owner, discarding", this.getId());
            this.discard();
            return;
        }

        // 获取玩家 - 使用getPlayerByUUID而不是getEntity
        var owner = ((ServerLevel) this.level()).getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) {
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.warn("Aircraft {} owner not found, discarding", this.getId());
            this.discard();
            return;
        }

        // 盘旋在玩家头顶
        Vec3 standbyPos = owner.position().add(0, STANDBY_HEIGHT_OFFSET, 0);
        flyTowards(standbyPos);

        // 如果有目标，进入赶赴状态
        if (targetPosition != null) {
            setState(AircraftState.APPROACH);
        }
    }

    /** 赶赴状态：飞向目标上方 */
    private void tickApproach() {
        if (targetPosition == null) {
            setState(AircraftState.STANDBY);
            return;
        }

        // 飞向目标上方的轰炸位置
        Vec3 bombingPos = new Vec3(targetPosition.x, BOMBING_HEIGHT, targetPosition.z);
        flyTowards(bombingPos);

        // 检查是否到达
        double distSq = this.position().distanceToSqr(bombingPos);
        if (distSq < ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD) {
            setState(AircraftState.DROPPING);
            bombingTimer = 0;
        }
    }

    /** 投弹状态：投弹并盘旋 */
    private void tickDropping() {
        if (targetPosition == null) {
            setState(AircraftState.RETURNING);
            return;
        }

        // 保持在目标上方
        Vec3 bombingPos = new Vec3(targetPosition.x, BOMBING_HEIGHT, targetPosition.z);
        flyTowards(bombingPos);

        // 投弹逻辑
        bombingTimer++;
        if (bombingTimer >= BOMBING_INTERVAL) {
            bombingTimer = 0;

            if (getAmmoCount() > 0) {
                dropBomb();
                setAmmoCount(getAmmoCount() - 1);
            } else {
                // 弹药耗尽，返航
                setState(AircraftState.RETURNING);
            }
        }
    }

    /** 返航状态：飞回玩家位置并移除 */
    private void tickReturning() {
        if (ownerUUID == null) {
            this.discard();
            return;
        }

        var owner = ((ServerLevel) this.level()).getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) {
            this.discard();
            return;
        }

        // 飞回玩家位置
        Vec3 returnPos = owner.position().add(0, STANDBY_HEIGHT_OFFSET, 0);
        flyTowards(returnPos);

        // 到达后移除（Phase 1 简化处理）
        double distSq = this.position().distanceToSqr(returnPos);
        if (distSq < ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD) {
            this.discard();
        }
    }

    /** 朝目标飞行 */
    private void flyTowards(Vec3 target) {
        Vec3 direction = target.subtract(this.position()).normalize();
        Vec3 motion = direction.scale(FLIGHT_SPEED);
        this.setDeltaMovement(motion);
        this.move(net.minecraft.world.entity.MoverType.SELF, motion);

        // 调整朝向
        this.setYRot((float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90);
        this.setXRot((float) (Math.asin(-direction.y) * 180.0 / Math.PI));
    }

    /** 投弹 */
    private void dropBomb() {
        if (this.level().isClientSide) return;

        // 炸弹初速度继承飞机速度
        Vec3 bombVelocity = this.getDeltaMovement();

        // 创建炸弹实体
        BombEntity bomb = new BombEntity(
                this.level(),
                this.position().add(0, -1, 0), // 从飞机下方投下
                bombVelocity,
                BOMB_EXPLOSION_RADIUS,
                BOMB_DAMAGE
        );

        this.level().addFreshEntity(bomb);
    }

    // ==================== 公共接口 ====================

    public void setTarget(@Nullable Vec3 position) {
        this.targetPosition = position;
    }

    public AircraftState getState() {
        String stateName = this.entityData.get(DATA_STATE);
        try {
            return AircraftState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            return AircraftState.STANDBY;
        }
    }

    public void setState(AircraftState state) {
        this.entityData.set(DATA_STATE, state.name());
    }

    public int getAmmoCount() {
        return this.entityData.get(DATA_AMMO_COUNT);
    }

    public void setAmmoCount(int count) {
        this.entityData.set(DATA_AMMO_COUNT, count);
    }

    // ==================== NBT 序列化 ====================

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.hasUUID("Owner")) {
            this.ownerUUID = compound.getUUID("Owner");
        }

        if (compound.contains("TargetX")) {
            this.targetPosition = new Vec3(
                    compound.getDouble("TargetX"),
                    compound.getDouble("TargetY"),
                    compound.getDouble("TargetZ")
            );
        }

        this.bombingTimer = compound.getInt("BombingTimer");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID);
        }

        if (this.targetPosition != null) {
            compound.putDouble("TargetX", this.targetPosition.x);
            compound.putDouble("TargetY", this.targetPosition.y);
            compound.putDouble("TargetZ", this.targetPosition.z);
        }

        compound.putInt("BombingTimer", this.bombingTimer);
    }
}
