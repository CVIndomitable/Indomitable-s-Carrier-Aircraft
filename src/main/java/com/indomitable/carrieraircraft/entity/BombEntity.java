package com.indomitable.carrieraircraft.entity;

import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 航空弹药实体。
 *
 * 特性：
 * - 自由落体物理（重力 + 空气阻力）
 * - 接触地面/实体时爆炸
 * - 可配置伤害和爆炸半径
 *
 * 物理模型：
 * 1. 先应用阻力到速度（vx, vy, vz 分别缩放）
 * 2. 再应用重力到 vy
 * 3. 最后更新位置
 */
public class BombEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_EXPLOSION_RADIUS =
            SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<String> DATA_WEAPON_TYPE =
            SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.STRING);

    /** 重力加速度 (blocks/tick²) */
    private static final double GRAVITY = 0.05;

    /** 空气阻力系数 (每 tick 速度保留比例) */
    private static final double DRAG_FACTOR = 0.98;

    /** 最大存活时间 (ticks)，防止卡住 */
    private static final int MAX_LIFETIME = 400; // 20秒

    /** 爆炸只破坏方块（受 mobGriefing 规则约束）；实体伤害由 explode() 里的衰减公式控制，避免双重伤害 */
    private static final ExplosionDamageCalculator BLOCK_ONLY_EXPLOSION =
            new SimpleExplosionDamageCalculator(true, false, Optional.empty(), Optional.empty());

    /** 注意不要与 Entity#tickCount 同名，否则会遮蔽父类字段 */
    private int lifeTicks = 0;

    public BombEntity(EntityType<? extends BombEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false); // 使用自定义重力
    }

    /** 便捷构造函数：用于投弹 */
    public BombEntity(Level level, Vec3 position, Vec3 initialVelocity, float explosionRadius, float damage) {
        this(level, position, initialVelocity, explosionRadius, damage, "AERIAL_BOMB");
    }

    /** 便捷构造函数：用于投放不同航空弹药 */
    public BombEntity(Level level, Vec3 position, Vec3 initialVelocity, float explosionRadius, float damage, String weaponType) {
        this(ModEntityTypes.BOMB.get(), level);
        this.setPos(position);
        this.setDeltaMovement(initialVelocity);
        this.setExplosionRadius(explosionRadius);
        this.setDamage(damage);
        this.setWeaponType(weaponType);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EXPLOSION_RADIUS, 3.0F);
        builder.define(DATA_DAMAGE, 20.0F);
        builder.define(DATA_WEAPON_TYPE, "AERIAL_BOMB");
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // 超时自动移除
            if (++lifeTicks > MAX_LIFETIME) {
                this.discard();
                return;
            }

            // 检查碰撞
            if (checkCollision()) {
                explode();
                return;
            }
        }

        // 物理模拟
        applyPhysics();
    }

    /** 应用物理模拟 */
    private void applyPhysics() {
        Vec3 motion = this.getDeltaMovement();

        // 1. 应用空气阻力
        motion = motion.scale(DRAG_FACTOR);

        // 2. 应用重力
        motion = motion.add(0, -GRAVITY, 0);

        // 3. 更新速度
        this.setDeltaMovement(motion);

        // 4. 移动实体
        this.move(MoverType.SELF, motion);
    }

    /** 检查碰撞（地面或实体） */
    private boolean checkCollision() {
        // 检查是否接触地面
        if (this.onGround()) {
            return true;
        }

        // 检查脚下方块
        BlockPos belowPos = this.blockPosition().below();
        BlockState belowState = this.level().getBlockState(belowPos);
        if (!belowState.isAir()) {
            // 距离方块表面很近
            double distToGround = this.getY() - belowPos.getY() - 1.0;
            if (distToGround < 0.3) {
                return true;
            }
        }

        // 检查是否碰撞实体（不与投弹的飞机和其他弹药相互引爆）
        AABB aabb = this.getBoundingBox().inflate(0.5);
        return !this.level().getEntities(this, aabb,
                e -> e != this && !(e instanceof BombEntity) && !(e instanceof AircraftEntity)).isEmpty();
    }

    /** 爆炸处理 */
    private void explode() {
        if (this.level().isClientSide) return;

        float radius = this.getExplosionRadius();
        float damage = this.getDamage();

        AABB damageBox = this.getBoundingBox().inflate(radius);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, damageBox, LivingEntity::isAlive)) {
            if (target instanceof AircraftEntity) {
                continue;
            }
            double distance = target.distanceTo(this);
            if (distance > radius) {
                continue;
            }
            float scaledDamage = (float) (damage * (1.0 - distance / (radius + 1.0)));
            if (scaledDamage <= 0.0F) {
                continue;
            }
            target.hurt(this.damageSources().explosion(this, null), scaledDamage);
        }

        // 爆炸只负责方块破坏、视效与击退；MOB 模式受 mobGriefing 游戏规则约束
        this.level().explode(
                this,
                null,
                BLOCK_ONLY_EXPLOSION,
                this.getX(),
                this.getY(),
                this.getZ(),
                radius,
                false,
                Level.ExplosionInteraction.MOB
        );

        this.discard();
    }

    // ==================== Getter/Setter ====================

    public float getExplosionRadius() {
        return this.entityData.get(DATA_EXPLOSION_RADIUS);
    }

    public void setExplosionRadius(float radius) {
        this.entityData.set(DATA_EXPLOSION_RADIUS, radius);
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public String getWeaponType() {
        return this.entityData.get(DATA_WEAPON_TYPE);
    }

    public void setWeaponType(String weaponType) {
        this.entityData.set(DATA_WEAPON_TYPE, weaponType);
    }

    // ==================== NBT 序列化 ====================

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.lifeTicks = compound.getInt("TickCount");
        if (compound.contains("ExplosionRadius")) {
            this.setExplosionRadius(compound.getFloat("ExplosionRadius"));
        }
        if (compound.contains("Damage")) {
            this.setDamage(compound.getFloat("Damage"));
        }
        if (compound.contains("WeaponType")) {
            this.setWeaponType(compound.getString("WeaponType"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("TickCount", this.lifeTicks);
        compound.putFloat("ExplosionRadius", this.getExplosionRadius());
        compound.putFloat("Damage", this.getDamage());
        compound.putString("WeaponType", this.getWeaponType());
    }
}
