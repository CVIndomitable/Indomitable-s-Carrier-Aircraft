package com.indomitable.carrieraircraft.entity;

import com.indomitable.carrieraircraft.aircraft.AmmoType;
import com.indomitable.carrieraircraft.aircraft.SeaAttackMode;
import com.indomitable.carrieraircraft.combat.HitNotifier;
import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import com.indomitable.carrieraircraft.targeting.FriendlyFireFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    /**
     * 弹道类型，决定 {@link BombEntity} 在 {@code tick()} 中应用哪一套物理模型。
     *
     * <p>取代旧的 {@code String weaponType}：所有引用都是编译期检查的枚举常量，避免拼写错误。
     */
    public enum WeaponType {
        /** 自由落体航弹（水平轰炸也是这一类，仅靠散布函数模拟精度）。 */
        AERIAL_BOMB,
        /** 俯冲轰炸：短时间追踪目标实体。 */
        GUIDED_BOMB,
        /** 鱼雷：入水后定深自走。 */
        AERIAL_TORPEDO,
        /** 火箭弹：自走直线弹道。 */
        ROCKET;

        /**
         * 根据对海槽弹药类型 + 攻击模式推导 {@link WeaponType}。
         *
         * <p>对海攻击模式包含 {@link SeaAttackMode#DIVE_BOMBING} 时升级为 {@link #GUIDED_BOMB}；
         * 其它情况直接映射为对应枚举。
         */
        public static WeaponType derive(AmmoType ammoType, java.util.Collection<SeaAttackMode> seaAttackModes) {
            if (ammoType == AmmoType.AERIAL_BOMB && seaAttackModes.contains(SeaAttackMode.DIVE_BOMBING)) {
                return GUIDED_BOMB;
            }
            return switch (ammoType) {
                case AERIAL_BOMB    -> AERIAL_BOMB;
                case AERIAL_TORPEDO -> AERIAL_TORPEDO;
                case ROCKET         -> ROCKET;
                default             -> AERIAL_BOMB;
            };
        }

        /** 兼容旧 NBT：从存档中读到的字符串还原枚举；无法识别时回落到 {@link #AERIAL_BOMB}。 */
        public static WeaponType fromLegacyString(String raw) {
            if (raw == null || raw.isEmpty()) return AERIAL_BOMB;
            try {
                return WeaponType.valueOf(raw);
            } catch (IllegalArgumentException e) {
                // 旧版本可能写入 "GUIDED_BOMB" 之外的别名（如 "DIVE_BOMB"）；保留兜底。
                return AERIAL_BOMB;
            }
        }
    }

    private static final EntityDataAccessor<Float> DATA_EXPLOSION_RADIUS =
            SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.FLOAT);

    /** 内部使用枚举；底层仍以字符串序列化以兼容旧存档。 */
    private static final EntityDataAccessor<String> DATA_WEAPON_TYPE =
            SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.STRING);

    /** 重力加速度 (blocks/tick²) */
    private static final double GRAVITY = 0.05;

    /** 空气阻力系数 (每 tick 速度保留比例) */
    private static final double DRAG_FACTOR = 0.98;

    /** 最大存活时间 (ticks)，防止卡住 */
    private static final int MAX_LIFETIME = 400; // 20秒

    /** 爆炸只破坏方块（受 mobGriefing 规则约束）；实体伤害由 BombExplosionDamageCalculator 控制。
     *  这里使用默认计算器（damagesEntities=false），实际伤害在 explode() 中通过自定义计算器应用。 */
    private static final ExplosionDamageCalculator BLOCK_ONLY_EXPLOSION = new BlockOnlyExplosionDamageCalculator();

    /** 注意不要与 Entity#tickCount 同名，否则会遮蔽父类字段 */
    private int lifeTicks = 0;

    /** 制导弹使用的目标实体。只需服务端保存，客户端跟随实体同步即可。 */
    private UUID targetUUID;

    /** 投弹的飞机名称，用于反馈消息 */
    private Component sourceAircraftName;

    /** 投弹的飞机 UUID，用于伤害来源 */
    private UUID sourceAircraftUUID;

    /** 弹药所有者（玩家），用于反馈消息 */
    private UUID ownerUUID;

    public BombEntity(EntityType<? extends BombEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false); // 使用自定义重力
    }

    /** 便捷构造函数：用于投弹 */
    public BombEntity(Level level, Vec3 position, Vec3 initialVelocity, float explosionRadius, float damage) {
        this(level, position, initialVelocity, explosionRadius, damage, WeaponType.AERIAL_BOMB);
    }

    /** 便捷构造函数：用于投放不同航空弹药 */
    public BombEntity(Level level, Vec3 position, Vec3 initialVelocity, float explosionRadius, float damage,
                      WeaponType weaponType) {
        this(level, position, initialVelocity, explosionRadius, damage, weaponType, null);
    }

    /** 便捷构造函数：用于需要追踪实体目标的弹药。 */
    public BombEntity(Level level, Vec3 position, Vec3 initialVelocity, float explosionRadius, float damage,
                      WeaponType weaponType, UUID targetUUID) {
        this(ModEntityTypes.BOMB.get(), level);
        this.setPos(position);
        this.setDeltaMovement(initialVelocity);
        this.setExplosionRadius(explosionRadius);
        this.setDamage(damage);
        this.setWeaponType(weaponType);
        this.targetUUID = targetUUID;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EXPLOSION_RADIUS, 3.0F);
        builder.define(DATA_DAMAGE, 20.0F);
        builder.define(DATA_WEAPON_TYPE, WeaponType.AERIAL_BOMB.name());
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
        WeaponType type = this.getWeaponType();
        switch (type) {
            case GUIDED_BOMB -> applyGuidedBombPhysics();
            case AERIAL_TORPEDO -> applyTorpedoPhysics();
            case ROCKET -> applyRocketPhysics();
            case AERIAL_BOMB -> applyFreeFallPhysics();
        }
    }

    /** 自由落体航弹：重力 + 阻力。 */
    private void applyFreeFallPhysics() {
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

    /** 俯冲轰炸航弹：短时间追踪目标，仍保留少量下坠。 */
    private void applyGuidedBombPhysics() {
        Vec3 motion = this.getDeltaMovement();
        Entity target = resolveTarget();
        if (target != null && target.isAlive()) {
            Vec3 aimPoint = target.getBoundingBox().getCenter();
            Vec3 desired = aimPoint.subtract(position());
            if (desired.lengthSqr() > 0.0001) {
                double speed = Math.max(0.75, motion.length());
                motion = motion.scale(0.78).add(desired.normalize().scale(speed).scale(0.22));
            }
        }
        motion = motion.scale(0.99).add(0, -GRAVITY * 0.45, 0);
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
    }

    /** 鱼雷：入水后定深自走；未入水时按重力下落。 */
    private void applyTorpedoPhysics() {
        Vec3 motion = this.getDeltaMovement();
        if (this.isInWaterOrBubble()) {
            Vec3 horizontal = new Vec3(motion.x, 0, motion.z);
            if (horizontal.lengthSqr() < 0.0001) {
                horizontal = new Vec3(0, 0, 1);
            }
            motion = horizontal.normalize().scale(0.78).add(0, -0.01, 0);
        } else {
            motion = motion.scale(0.99).add(0, -GRAVITY, 0);
        }
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
    }

    /** 火箭：自走直线弹道，轻微阻力，不受重力显著影响。 */
    private void applyRocketPhysics() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 0.0001) {
            motion = new Vec3(0, -0.02, 1.0);
        }
        double speed = Math.max(1.15, motion.length() * 0.995);
        motion = motion.normalize().scale(speed).add(0, -0.005, 0);
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
    }

    /** 检查碰撞（地面或实体） */
    private boolean checkCollision() {
        // 检查是否接触地面
        if (this.onGround()) {
            return true;
        }

        // 检查当前方块/脚下方块。水和空气没有实体碰撞形状，不会让鱼雷刚入水就爆炸。
        if (hasSolidCollision(this.blockPosition())) {
            return true;
        }
        BlockPos belowPos = this.blockPosition().below();
        if (hasSolidCollision(belowPos)) {
            // 距离方块表面很近
            double distToGround = this.getY() - belowPos.getY() - 1.0;
            if (distToGround < 0.3) {
                return true;
            }
        }

        // 检查是否碰撞实体。M19：不再一刀切排除所有 AircraftEntity，
        // 改为对友军飞机（包含本机 source）保留，对敌方飞机正常引爆。
        AABB aabb = this.getBoundingBox().inflate(0.5);
        Entity sourceAircraft = resolveSourceAircraft();
        return !this.level().getEntities(this, aabb, e -> {
            if (e == this) return false;
            if (e instanceof BombEntity) return false;
            if (e instanceof AircraftEntity aircraft) {
                if (sourceAircraft instanceof AircraftEntity src) {
                    // 友军飞机不引爆；敌方飞机正常引爆
                    return FriendlyFireFilter.canAttack(src, aircraft);
                }
                return true;
            }
            return true;
        }).isEmpty();
    }

    private Entity resolveSourceAircraft() {
        if (sourceAircraftUUID == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(sourceAircraftUUID);
    }

    private boolean hasSolidCollision(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return !state.getCollisionShape(this.level(), pos).isEmpty();
    }

    private Entity resolveTarget() {
        if (targetUUID == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(targetUUID);
    }

    /** 爆炸处理 */
    private void explode() {
        if (this.level().isClientSide) return;

        float radius = this.getExplosionRadius();
        float damage = this.getDamage();

        // 获取玩家所有者
        Player player = null;
        Entity ownerEntity = null;
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            ownerEntity = serverLevel.getEntity(ownerUUID);
            if (ownerEntity instanceof Player p) {
                player = p;
            }
        }

        // 1. 收集通过敌我识别的目标，并记录爆炸前的存活状态
        AABB damageBox = this.getBoundingBox().inflate(radius);
        Map<UUID, LivingEntity> validTargets = new HashMap<>();
        Map<UUID, Boolean> aliveBefore = new HashMap<>();
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, damageBox, LivingEntity::isAlive)) {
            if (!passesFriendlyFire(target, ownerEntity)) continue;
            validTargets.put(target.getUUID(), target);
            aliveBefore.put(target.getUUID(), target.isAlive());
        }

        // 2. 构造伤害来源（投弹飞机 → 玩家）
        Entity damageSource = this;
        if (sourceAircraftUUID != null && level() instanceof ServerLevel sl) {
            Entity aircraft = sl.getEntity(sourceAircraftUUID);
            if (aircraft != null) {
                damageSource = aircraft;
            }
        }
        DamageSource source = this.damageSources().explosion(damageSource, ownerEntity);

        // 3. 通过自定义计算器执行爆炸：
        //    - 距离衰减公式封装在 BombExplosionDamageCalculator
        //    - 友军保护通过预校验的 allowedTargets 集合实现
        Set<UUID> allowedIds = new HashSet<>(validTargets.keySet());
        this.level().explode(
                this,
                source,
                new BombExplosionDamageCalculator(damage, radius, allowedIds),
                this.getX(),
                this.getY(),
                this.getZ(),
                radius,
                false,
                Level.ExplosionInteraction.MOB
        );

        // 4. 发送命中/击杀反馈
        if (player != null) {
            for (LivingEntity target : validTargets.values()) {
                Boolean wasAlive = aliveBefore.get(target.getUUID());
                if (wasAlive == null || !wasAlive) continue;
                boolean killed = !target.isAlive() || target.getHealth() <= 0.0F;
                notifyOwner(player, target, killed);
            }
        }

        this.discard();
    }

    /** 校验敌我识别：友军飞机一律排除，友军玩家/召唤物通过 FriendlyFireFilter 过滤。 */
    private boolean passesFriendlyFire(LivingEntity target, Entity ownerEntity) {
        if (target instanceof AircraftEntity) {
            Entity source = sourceAircraftUUID != null && level() instanceof ServerLevel sl
                    ? sl.getEntity(sourceAircraftUUID) : null;
            if (source instanceof AircraftEntity aircraft
                    && !FriendlyFireFilter.canAttack(aircraft, target)) {
                return false;
            }
        }
        if (ownerUUID != null && !FriendlyFireFilter.canPlayerDamage(ownerUUID, ownerEntity, target)) {
            return false;
        }
        return true;
    }

    /**
     * 自定义爆炸伤害计算器：仅对预校验通过的实体应用距离衰减伤害。
     *
     * <p>由 {@link #explode()} 在调用 {@code level.explode()} 时传入，避免在主循环里手动 {@code target.hurt()}。
     */
    private static final class BombExplosionDamageCalculator extends ExplosionDamageCalculator {
        private final float baseDamage;
        private final float radius;
        private final Set<UUID> allowedTargets;

        BombExplosionDamageCalculator(float baseDamage, float radius, Set<UUID> allowedTargets) {
            this.baseDamage = baseDamage;
            this.radius = radius;
            this.allowedTargets = allowedTargets;
        }

        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
            return allowedTargets.contains(entity.getUUID());
        }

        @Override
        public float getEntityDamageAmount(Explosion explosion, Entity entity) {
            double distance = Math.sqrt(entity.distanceToSqr(explosion.center()));
            if (distance > radius) return 0.0F;
            float falloff = 1.0F - (float) (distance / (radius + 1.0));
            float scaled = baseDamage * falloff;
            return Math.max(0.0F, scaled);
        }
    }

    /** 爆炸仅破坏方块、视效、击退的占位计算器；实体伤害由 {@link BombExplosionDamageCalculator} 负责。 */
    private static final class BlockOnlyExplosionDamageCalculator extends ExplosionDamageCalculator {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }
    }

    /**
     * 向玩家发送命中/击杀反馈消息。
     *
     * @param player 玩家
     * @param target 被命中的目标
     * @param killed 是否击杀
     */
    private void notifyOwner(Player player, Entity target, boolean killed) {
        Component weaponName = sourceAircraftName != null
                ? sourceAircraftName
                : Component.translatable("entity.indomitablecarrieraircraft.aircraft");

        String key = killed
                ? "message.indomitablecarrieraircraft.weapon_kill"
                : "message.indomitablecarrieraircraft.weapon_hit";

        // M13：避免翻译键缺失时直接回显 key 字符串（污染聊天）。
        Component body;
        try {
            body = Component.translatable(key, weaponName, target.getDisplayName());
        } catch (Exception e) {
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.warn(
                    "Failed to localize hit feedback key '{}'", key, e);
            body = Component.literal((killed ? "[击杀] " : "[命中] ") + target.getDisplayName().getString());
        }
        HitNotifier.send(player, body);
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

    public WeaponType getWeaponType() {
        return WeaponType.fromLegacyString(this.entityData.get(DATA_WEAPON_TYPE));
    }

    public void setWeaponType(WeaponType weaponType) {
        this.entityData.set(DATA_WEAPON_TYPE, weaponType.name());
    }

    /**
     * 设置投弹飞机的名称（用于反馈消息）。
     *
     * @param name 飞机名称
     */
    public void setSourceAircraftName(Component name) {
        this.sourceAircraftName = name;
    }

    /**
     * 设置投弹飞机的 UUID（用于伤害来源）。
     *
     * @param aircraftUUID 飞机 UUID
     */
    public void setSourceAircraftUUID(UUID aircraftUUID) {
        this.sourceAircraftUUID = aircraftUUID;
    }

    /**
     * 便捷方法：同时设置飞机实体的名称和 UUID。
     *
     * @param aircraft 飞机实体
     */
    public void setSourceAircraft(Entity aircraft) {
        this.sourceAircraftUUID = aircraft.getUUID();
        this.sourceAircraftName = aircraft.getDisplayName();
    }

    /**
     * 设置弹药所有者（玩家）。
     *
     * @param owner 所有者实体
     */
    public void setOwner(Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    // ==================== NBT 序列化 ====================

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.lifeTicks = compound.getInt("TickCount");
        if (compound.hasUUID("TargetUUID")) {
            this.targetUUID = compound.getUUID("TargetUUID");
        }
        if (compound.hasUUID("SourceAircraftUUID")) {
            this.sourceAircraftUUID = compound.getUUID("SourceAircraftUUID");
        }
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
        if (compound.contains("SourceAircraftName")) {
            try {
                this.sourceAircraftName = Component.Serializer.fromJson(
                        compound.getString("SourceAircraftName"), registryAccess());
            } catch (Exception e) {
                // 解析失败时使用默认名称
                this.sourceAircraftName = null;
            }
        }
        if (compound.contains("ExplosionRadius")) {
            this.setExplosionRadius(compound.getFloat("ExplosionRadius"));
        }
        if (compound.contains("Damage")) {
            this.setDamage(compound.getFloat("Damage"));
        }
        if (compound.contains("WeaponType")) {
            this.setWeaponType(WeaponType.fromLegacyString(compound.getString("WeaponType")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("TickCount", this.lifeTicks);
        if (this.targetUUID != null) {
            compound.putUUID("TargetUUID", this.targetUUID);
        }
        if (this.sourceAircraftUUID != null) {
            compound.putUUID("SourceAircraftUUID", this.sourceAircraftUUID);
        }
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
        if (this.sourceAircraftName != null) {
            compound.putString("SourceAircraftName",
                    Component.Serializer.toJson(this.sourceAircraftName, registryAccess()));
        }
        compound.putFloat("ExplosionRadius", this.getExplosionRadius());
        compound.putFloat("Damage", this.getDamage());
        compound.putString("WeaponType", this.getWeaponType().name());
    }
}
