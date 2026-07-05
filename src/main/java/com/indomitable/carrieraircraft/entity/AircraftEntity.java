package com.indomitable.carrieraircraft.entity;

import com.indomitable.carrieraircraft.aircraft.*;
import com.indomitable.carrieraircraft.ballistic.ThreeCoordinateCalculator;
import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import com.indomitable.carrieraircraft.firecontrol.PlayerAirControlSettings;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.item.AviationAmmoItem;
import com.indomitable.carrieraircraft.registry.ModDataComponents;
import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import com.indomitable.carrieraircraft.registry.ModItems;
import com.indomitable.carrieraircraft.targeting.FriendlyFireFilter;
import com.indomitable.carrieraircraft.targeting.TargetingSubsystem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 通用舰载机实体。
 *
 * <p>机型由武器槽配置唯一决定（对空槽 + 对海槽），不使用硬编码枚举。
 * 所有机型共用同一实体类型，行为由 {@link #seaWeaponSlot} 和 {@link #airWeaponSlot} 的内容派生。
 *
 * <p>对空能力类型（前射/自卫炮塔/无）由 {@link AircraftSpec#airWeaponMode()} 决定；
 * 对海攻击行为（水平轰炸/俯冲/鱼雷/火箭）由对海槽弹药类型决定。
 */
public class AircraftEntity extends FlyingMob {

    // ==================== 同步数据键 ====================
    private static final EntityDataAccessor<String> DATA_STATE =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SEA_AMMO_COUNT =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AIR_AMMO_COUNT =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    /** 机型 ID，由武器槽配置推导（如 "b25"、"btd_torpedo"），用于客户端显示 */
    private static final EntityDataAccessor<String> DATA_ROLE_ID =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);

    // ==================== AI 常量 ====================
    private static final double ARRIVAL_THRESHOLD = 4.0;
    private static final double HOVER_THRESHOLD = 2.0;
    private static final int TARGET_REFRESH_INTERVAL = 10;
    private static final int POST_ATTACK_TICKS = 45;
    private static final int DOGFIGHT_BURST_INTERVAL = 8;
    private static final int PASS_COOLDOWN_TICKS = 12;

    // ==================== 实例数据 ====================
    @Nullable
    private UUID ownerUUID;
    private AircraftSpec spec;

    private WeaponSlot seaWeaponSlot;
    private WeaponSlot airWeaponSlot;

    @Nullable
    private FireControlTarget currentTarget;
    @Nullable
    private ThreeCoordinateCalculator.AttackSolution attackSolution;
    @Nullable
    private Vec3 orbitPoint;
    @Nullable
    private Vec3 offlineHoldPoint;
    @Nullable
    private Entity dogfightTarget;
    @Nullable


    /** 盘旋相位偏移（弧度），每架飞机不同，避免所有飞机挤在同一轨道点 */
    private double orbitPhase;

    private int stateTimer;
    private int fireTimer;
    private int targetRefreshTimer;
    private int aswPingTimer;
    private final Set<UUID> glowingTargets = new HashSet<>();

    // ── 调试模式 ──
    private boolean debugMode = false;
    @Nullable
    private AircraftState debugState = null;
    private boolean debugLoop = false;
    @Nullable
    private Vec3 debugStartPos = null;
    @Nullable
    private Vec3 debugTargetPos = null;
    private int debugLoopTimer = 0;
    private static final int DEBUG_LOOP_RESET_TICKS = 100;

    // ==================== 构造 ====================

    public AircraftEntity(EntityType<? extends AircraftEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.orbitPhase = level.random.nextDouble() * Math.PI * 2;
        // 默认值，create() 会覆盖
        this.spec = AircraftSpecLoader.getInstance().getSpec("b25");
        this.seaWeaponSlot = new WeaponSlot(AmmoType.AERIAL_BOMB, 6, 6);
        this.airWeaponSlot = new WeaponSlot(AmmoType.MAGAZINE, 1000, 1000);
    }

    /**
     * 创建飞机实例。
     *
     * @param spec             飞机规格（速度、血量、载弹量等）
     * @param seaAmmoType      对海槽弹药类型（航弹/鱼雷/火箭弹）
     * @param preferredSeaAmmo 可选：覆盖对海槽弹药类型（需要 spec 允许）
     */
    public static AircraftEntity create(ServerLevel level, UUID ownerUUID, Vec3 spawnPos,
                                        AircraftSpec spec, AmmoType seaAmmoType,
                                        @Nullable AmmoType preferredSeaAmmo) {
        AircraftEntity aircraft = new AircraftEntity(ModEntityTypes.AIRCRAFT.get(), level);
        aircraft.setPos(spawnPos);
        aircraft.ownerUUID = ownerUUID;
        aircraft.spec = spec;
        aircraft.airWeaponSlot = new WeaponSlot(AmmoType.MAGAZINE, spec.magazineCapacity(), spec.magazineCapacity());

        // 确定对海槽弹药
        AmmoType actualSeaAmmo = seaAmmoType;
        if (preferredSeaAmmo != null && spec.supportsAmmo(preferredSeaAmmo)) {
            actualSeaAmmo = preferredSeaAmmo;
        }
        aircraft.seaWeaponSlot = new WeaponSlot(actualSeaAmmo, spec.seaAmmoCapacity(), spec.seaAmmoCapacity());

        // 同步到客户端
        aircraft.entityData.set(DATA_ROLE_ID, AircraftRole.derive(spec.airWeaponMode(),
                aircraft.seaWeaponSlot.ammoType(), spec).id());
        aircraft.syncAmmoData();

        aircraft.setState(AircraftState.STANDBY);
        FormationManager.getInstance().registerAircraft(ownerUUID, aircraft);
        return aircraft;
    }

    /** 简化创建：使用 spec 的默认对海弹药 */
    public static AircraftEntity create(ServerLevel level, UUID ownerUUID, Vec3 spawnPos, AircraftSpec spec) {
        AmmoType defaultSeaAmmo = defaultSeaAmmoFor(spec);
        return create(level, ownerUUID, spawnPos, spec, defaultSeaAmmo, null);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.FOLLOW_RANGE, 192.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, AircraftState.STANDBY.name());
        builder.define(DATA_SEA_AMMO_COUNT, 0);
        builder.define(DATA_AIR_AMMO_COUNT, 0);
        builder.define(DATA_ROLE_ID, AircraftRole.BOMBER.id());
    }

    // ==================== Tick ====================

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (debugMode) {
                tickDebug();
            } else {
                updateStateMachine();
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            clearAswGlow();
            if (ownerUUID != null) {
                FormationManager.getInstance().unregisterAircraft(ownerUUID, this.getUUID());
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ==================== 交互 ====================

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /** 把当前飞机转为物品栈，携带规格和剩余弹药数据 */
    private ItemStack toStack() {
        Item spawnerItem = getSpawnerItemForRole(getRole());
        ItemStack stack = new ItemStack(spawnerItem);
        stack.set(ModDataComponents.AIRCRAFT_ROLE, getRole().id());
        stack.set(ModDataComponents.SEA_AMMO_COUNT, seaWeaponSlot.count());
        stack.set(ModDataComponents.AIR_AMMO_COUNT, airWeaponSlot.count());
        return stack;
    }

    /** 根据推导出的机型找到对应的飞机物品 */
    private static Item getSpawnerItemForRole(AircraftRole role) {
        return switch (role) {
            case BOMBER -> ModItems.B25_SPAWNER.get();
            case DIVE_BOMBER -> ModItems.BTD_SPAWNER.get();
            case TORPEDO_BOMBER -> ModItems.BTD_TORPEDO_SPAWNER.get();
            case ROCKET_ATTACKER -> ModItems.ROCKET_ATTACKER_SPAWNER.get();
            case ASW_PATROL -> ModItems.ASW_PATROL_SPAWNER.get();
        };
    }

    // ==================== 状态机 ====================

    private void updateStateMachine() {
        if (ownerUUID == null) {
            discard();
            return;
        }

        if (tickCount % 40 == 0 && level() instanceof ServerLevel serverLevel) {
            FormationManager.getInstance().registerAircraft(ownerUUID, this);
            FormationManager.getInstance().updateLeaderChunkLoading(serverLevel, ownerUUID);
        }

        AircraftState currentState = getState();
        if (currentState != AircraftState.RETURNING) {
            tickSelfDefense();
            tickAswDetection();
        }
        stateTimer++;

        switch (currentState) {
            case STANDBY -> tickStandby();
            case ORBITING -> tickOrbiting();
            case LOCKED -> tickLocked();
            case APPROACH -> tickApproach();
            case ATTACKING -> tickAttacking();
            case DROPPING -> tickDropping();
            case POST_ATTACK -> tickPostAttack();
            case DOGFIGHT -> tickDogfight();
            case RETURNING -> tickReturning();
        }
    }

    private void tickStandby() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) { holdWhileOwnerOffline(); return; }
        offlineHoldPoint = null;

        Vec3 standbyPos = owner.position().add(0, spec.standbyHeight(), 0);
        hoverAround(standbyPos, 8.0);

        if (hasSeaAmmo() && readyForSortie() && refreshTarget()) {
            setState(AircraftState.LOCKED);
        }
    }

    private void tickOrbiting() {
        Vec3 center = orbitPoint;
        if (center == null) { setState(AircraftState.STANDBY); return; }
        hoverAround(center, 12.0);
        if (hasSeaAmmo() && readyForSortie() && refreshTarget()) {
            setState(AircraftState.LOCKED);
        }
    }

    private void tickLocked() {
        if (!refreshTarget()) {
            setState(orbitPoint == null ? AircraftState.STANDBY : AircraftState.ORBITING);
            return;
        }
        FireControlTarget target = currentTarget;
        if (target == null || !(level() instanceof ServerLevel serverLevel)) {
            setState(AircraftState.STANDBY); return;
        }
        Vec3 targetPoint = target.currentPosition(serverLevel);
        Entity targetEntity = target.resolveEntity(serverLevel);
        attackSolution = ThreeCoordinateCalculator.solve(
                position(), targetPoint, targetEntity, spec, seaWeaponSlot.ammoType());
        setState(AircraftState.APPROACH);
    }

    private void tickApproach() {
        if (attackSolution == null) { setState(AircraftState.LOCKED); return; }
        if (++targetRefreshTimer >= TARGET_REFRESH_INTERVAL) {
            targetRefreshTimer = 0;
            updateAttackSolution();
        }
        flyTowards(attackSolution.dropPoint(), spec.speed());
        if (position().distanceToSqr(attackSolution.dropPoint()) < ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD) {
            setState(AircraftState.ATTACKING);
        }
    }

    private void tickAttacking() {
        if (attackSolution == null) { setState(AircraftState.LOCKED); return; }
        Vec3 releaseLine = attackSolution.impactPoint()
                .subtract(attackSolution.attackDirection().scale(2.0))
                .with(net.minecraft.core.Direction.Axis.Y, attackSolution.dropPoint().y);
        flyTowards(releaseLine, spec.speed() * 0.92);
        double distanceToImpact = position().multiply(1, 0, 1)
                .distanceTo(attackSolution.impactPoint().multiply(1, 0, 1));
        if (stateTimer > PASS_COOLDOWN_TICKS && distanceToImpact <= attackSolution.releaseDistance()) {
            setState(AircraftState.DROPPING);
        }
    }

    private void tickDropping() {
        if (attackSolution == null) { setState(AircraftState.RETURNING); return; }
        int released = releaseSeaWeapons();
        if (released <= 0) { setState(AircraftState.RETURNING); return; }
        flyTowards(attackSolution.turnPoint(), spec.speed());
        setState(AircraftState.POST_ATTACK);
    }

    private void tickPostAttack() {
        if (attackSolution != null) { flyTowards(attackSolution.turnPoint(), spec.speed()); }
        if (stateTimer < POST_ATTACK_TICKS) return;
        if (!hasSeaAmmo()) { setState(AircraftState.RETURNING); return; }
        if (!refreshTarget()) {
            setState(orbitPoint == null ? AircraftState.STANDBY : AircraftState.ORBITING);
            return;
        }
        setState(AircraftState.LOCKED);
    }

    private void tickDogfight() {
        if (dogfightTarget == null || !dogfightTarget.isAlive() || !spec.canAttackAir() || airWeaponSlot.count() <= 0) {
            dogfightTarget = null;
            setState(hasSeaAmmo() ? AircraftState.LOCKED : AircraftState.RETURNING);
            return;
        }
        Vec3 intercept = dogfightTarget.position().add(dogfightTarget.getDeltaMovement().scale(8.0));
        flyTowards(intercept, spec.speed() * 1.08);

        double distSq = distanceToSqr(dogfightTarget);
        if (distSq < spec.attackRange() * spec.attackRange()) {
            fireTimer++;
            if (fireTimer >= DOGFIGHT_BURST_INTERVAL) {
                fireTimer = 0;
                shootAirTarget(dogfightTarget);
            }
        }
        if (distSq > 220.0 * 220.0 || stateTimer > 220) {
            dogfightTarget = null;
            setState(hasSeaAmmo() ? AircraftState.LOCKED : AircraftState.STANDBY);
        }
    }

    private static final double COLLECT_RANGE = 3.0;

    private void tickReturning() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) { holdWhileOwnerOffline(); return; }
        offlineHoldPoint = null;

        Vec3 ownerPos = owner.position();
        double distToOwner = position().distanceTo(ownerPos);

        // 越靠近玩家，目标高度越低，螺旋下降
        double descendFactor = Math.min(1.0, distToOwner / (spec.standbyHeight() * 2.0));
        double targetY = ownerPos.y + spec.standbyHeight() * descendFactor;
        Vec3 returnPos = new Vec3(ownerPos.x, targetY, ownerPos.z);

        flyTowards(returnPos, spec.speed());

        // 靠近玩家自动回收（距离基于到玩家的3D距离）
        if (distToOwner < COLLECT_RANGE + spec.standbyHeight() * 0.1) {
            ItemStack stack = toStack();
            if (owner.getInventory().add(stack)) {
                owner.sendSystemMessage(Component.literal(String.format(
                        "已回收 %s（对海 %d/%d，对空 %d/%d）",
                        getRole().displayName(),
                        seaWeaponSlot.count(), seaWeaponSlot.capacity(),
                        airWeaponSlot.count(), airWeaponSlot.capacity()
                )).withStyle(ChatFormatting.GREEN));
                discard();
            }
        }
    }

    private void holdWhileOwnerOffline() {
        if (offlineHoldPoint == null) {
            offlineHoldPoint = orbitPoint != null ? orbitPoint : position();
        }
        hoverAround(offlineHoldPoint, 12.0);
    }

    // ==================== 调试模式 ====================

    /** 进入调试模式：瞬移到 startPos 并强制进入指定状态。 */
    public void debugExecuteState(AircraftState state, Vec3 startPos, Vec3 targetPos, boolean loop) {
        this.debugMode = true;
        this.debugState = state;
        this.debugLoop = loop;
        this.debugStartPos = startPos;
        this.debugTargetPos = targetPos;
        this.debugLoopTimer = 0;

        // 瞬移到起始位置
        setPos(startPos);
        setDeltaMovement(Vec3.ZERO);

        // 根据状态准备数据
        if (level() instanceof ServerLevel serverLevel) {
            if (state == AircraftState.ORBITING) {
                this.orbitPoint = targetPos;
            } else if (state == AircraftState.LOCKED || state == AircraftState.APPROACH
                    || state == AircraftState.ATTACKING || state == AircraftState.DROPPING
                    || state == AircraftState.POST_ATTACK) {
                this.currentTarget = FireControlTarget.position(serverLevel, targetPos);
                this.attackSolution = ThreeCoordinateCalculator.solve(
                        startPos, targetPos, null, spec, seaWeaponSlot.ammoType());
            } else if (state == AircraftState.DOGFIGHT) {
                // DOGFIGHT 需要实体目标，用位置模拟：创建一个临时的攻击解算
                this.currentTarget = FireControlTarget.position(serverLevel, targetPos);
            }
        }

        setState(state);
    }

    /** 退出调试模式，回到 STANDBY。 */
    public void debugStop() {
        this.debugMode = false;
        this.debugState = null;
        this.debugLoop = false;
        this.debugStartPos = null;
        this.debugTargetPos = null;
        this.debugLoopTimer = 0;
        setState(AircraftState.STANDBY);
    }

    public boolean isDebugMode() { return debugMode; }

    /** 调试模式下的 tick：执行对应状态的行为，循环时自动重置。 */
    private void tickDebug() {
        if (debugState == null) { debugStop(); return; }

        stateTimer++;
        debugLoopTimer++;

        // 执行对应状态的行为
        switch (debugState) {
            case STANDBY -> {
                Vec3 center = debugStartPos != null ? debugStartPos : position();
                hoverAround(center.add(0, spec.standbyHeight(), 0), 8.0);
            }
            case ORBITING -> {
                Vec3 center = debugTargetPos != null ? debugTargetPos : position();
                hoverAround(center, 12.0);
            }
            case LOCKED -> {
                // 锁定状态：悬停并保持锁定
                hoverAround(position(), 4.0);
            }
            case APPROACH -> {
                if (attackSolution == null) { resetDebugLoop(); return; }
                flyTowards(attackSolution.dropPoint(), spec.speed());
                if (position().distanceToSqr(attackSolution.dropPoint()) < ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD) {
                    if (debugLoop) { resetDebugLoop(); } else { debugStop(); }
                }
            }
            case ATTACKING -> {
                if (attackSolution == null) { resetDebugLoop(); return; }
                Vec3 releaseLine = attackSolution.impactPoint()
                        .subtract(attackSolution.attackDirection().scale(2.0))
                        .with(net.minecraft.core.Direction.Axis.Y, attackSolution.dropPoint().y);
                flyTowards(releaseLine, spec.speed() * 0.92);
                double dist = position().multiply(1, 0, 1)
                        .distanceTo(attackSolution.impactPoint().multiply(1, 0, 1));
                if (stateTimer > PASS_COOLDOWN_TICKS && dist <= attackSolution.releaseDistance()) {
                    if (debugLoop) { resetDebugLoop(); } else { debugStop(); }
                }
            }
            case DROPPING -> {
                if (attackSolution == null) { resetDebugLoop(); return; }
                flyTowards(attackSolution.turnPoint(), spec.speed());
                if (debugLoop && stateTimer > 20) { resetDebugLoop(); }
                else if (!debugLoop && stateTimer > 20) { debugStop(); }
            }
            case POST_ATTACK -> {
                if (attackSolution != null) { flyTowards(attackSolution.turnPoint(), spec.speed()); }
                if (stateTimer >= POST_ATTACK_TICKS) {
                    if (debugLoop) { resetDebugLoop(); } else { debugStop(); }
                }
            }
            case DOGFIGHT -> {
                // 模拟追击目标位置
                if (debugTargetPos != null) {
                    flyTowards(debugTargetPos, spec.speed() * 1.08);
                    if (position().distanceToSqr(debugTargetPos) < 16.0) {
                        if (debugLoop) { resetDebugLoop(); } else { debugStop(); }
                    }
                }
                if (stateTimer > 220) {
                    if (debugLoop) { resetDebugLoop(); } else { debugStop(); }
                }
            }
            case RETURNING -> {
                ServerPlayer owner = getOwnerPlayer();
                if (owner != null) {
                    Vec3 returnPos = owner.position().add(0, spec.standbyHeight(), 0);
                    flyTowards(returnPos, spec.speed());
                    if (position().distanceToSqr(returnPos) < ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD) {
                        if (debugLoop) { resetDebugLoop(); } else { debugStop(); }
                    }
                }
            }
        }

        // 安全超时：防止卡死
        if (debugLoopTimer > DEBUG_LOOP_RESET_TICKS * 3) {
            if (debugLoop) { resetDebugLoop(); } else { debugStop(); }
        }
    }

    /** 循环模式：瞬移回起点，重新初始化状态。 */
    private void resetDebugLoop() {
        if (debugStartPos != null) {
            setPos(debugStartPos);
            setDeltaMovement(Vec3.ZERO);
        }
        stateTimer = 0;
        fireTimer = 0;
        debugLoopTimer = 0;

        // 重新准备攻击解算
        if (level() instanceof ServerLevel serverLevel && debugTargetPos != null) {
            if (debugState == AircraftState.APPROACH || debugState == AircraftState.ATTACKING
                    || debugState == AircraftState.DROPPING || debugState == AircraftState.POST_ATTACK) {
                this.attackSolution = ThreeCoordinateCalculator.solve(
                        debugStartPos, debugTargetPos, null, spec, seaWeaponSlot.ammoType());
            }
        }
    }

    // ==================== 目标与解算 ====================

    private boolean refreshTarget() {
        if (currentTarget != null) {
            // 手动指定的目标（火控或 setTarget）优先保留，不被自动锁定覆盖
            if (targetRefreshTimer < TARGET_REFRESH_INTERVAL) { targetRefreshTimer++; return true; }
            // 定期验证目标是否仍然有效
            if (!(level() instanceof ServerLevel serverLevel)) return false;
            if (currentTarget.isEntityTarget()) {
                Entity entity = currentTarget.resolveEntity(serverLevel);
                if (entity != null && entity.isAlive()) { targetRefreshTimer = 0; return true; }
            } else {
                // 坐标目标始终有效（有过期时间由 FireControlTarget 管理）
                targetRefreshTimer = 0;
                return true;
            }
        }
        // 无目标或目标失效，尝试获取新目标
        targetRefreshTimer = 0;
        if (!(level() instanceof ServerLevel serverLevel)) return false;
        currentTarget = TargetingSubsystem.resolveTarget(this, serverLevel);
        if (currentTarget == null) { attackSolution = null; return false; }
        if (!FormationManager.getInstance().shouldParticipate(this, currentTarget, serverLevel)) {
            currentTarget = null;
            attackSolution = null;
            return false;
        }
        return true;
    }

    private boolean readyForSortie() {
        if (ownerUUID == null || !(level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        return FormationManager.getInstance().isReadyForSortie(serverLevel, ownerUUID, getUUID());
    }

    private void updateAttackSolution() {
        if (!(level() instanceof ServerLevel serverLevel) || currentTarget == null) return;
        Entity targetEntity = currentTarget.resolveEntity(serverLevel);
        if (currentTarget.isEntityTarget() && (targetEntity == null || !targetEntity.isAlive())) {
            currentTarget = null; attackSolution = null;
            setState(AircraftState.LOCKED); return;
        }
        attackSolution = ThreeCoordinateCalculator.solve(
                position(), currentTarget.currentPosition(serverLevel), targetEntity, spec, seaWeaponSlot.ammoType());
    }

    // ==================== 自卫与反潜 ====================

    private void tickSelfDefense() {
        if (!(level() instanceof ServerLevel serverLevel) || getState() == AircraftState.DOGFIGHT) return;
        if (!spec.canAttackAir() || airWeaponSlot.count() <= 0 || ownerUUID == null) return;

        PlayerAirControlSettings settings = FireControlSystem.getInstance().settings(serverLevel, ownerUUID);
        AirDefenseMode defenseMode = settings.airDefenseMode();
        if (defenseMode == AirDefenseMode.LOW_AGGRESSION) return;

        double range = Math.max(spec.turretRange(), spec.attackRange());
        AABB box = getBoundingBox().inflate(range);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && !entity.onGround()
                        && FriendlyFireFilter.canAttack(this, entity));
        if (targets.isEmpty()) return;

        LivingEntity nearest = targets.stream()
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (nearest == null) return;

        if (defenseMode == AirDefenseMode.ACTIVE && spec.airWeaponMode().hasForwardGun()) {
            dogfightTarget = nearest;
            setState(AircraftState.DOGFIGHT);
        } else if (spec.airWeaponMode().hasTurret()
                && distanceToSqr(nearest) <= spec.turretRange() * spec.turretRange()) {
            shootAirTarget(nearest);
        }
    }

    private void tickAswDetection() {
        if (!(level() instanceof ServerLevel serverLevel) || spec.aswRange() <= 0.0) return;
        if (++aswPingTimer < 40) return;
        aswPingTimer = 0;
        AABB box = getBoundingBox().inflate(spec.aswRange());
        Set<UUID> currentTargets = new HashSet<>();
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box, this::isUnderwaterHostile)) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false, true));
            currentTargets.add(entity.getUUID());
        }
        glowingTargets.removeIf(uuid -> !currentTargets.contains(uuid));
        glowingTargets.addAll(currentTargets);
    }

    private boolean isUnderwaterHostile(LivingEntity entity) {
        return entity.isAlive() && entity.isInWaterOrBubble()
                && FriendlyFireFilter.canAttack(this, entity);
    }

    /** 清除所有被本机标记为发光的水下目标。 */
    private void clearAswGlow() {
        glowingTargets.clear();
    }

    // ==================== 武器释放 ====================

    private int releaseSeaWeapons() {
        if (!hasSeaAmmo() || attackSolution == null) return 0;
        int requested = calculateReleaseCount();
        int released = seaWeaponSlot.consume(requested);
        syncAmmoData();
        for (int i = 0; i < released; i++) {
            Vec3 offset = calculateWeaponOffset(i, released);
            spawnSeaWeapon(offset);
        }
        return released;
    }

    private int calculateReleaseCount() {
        if (ownerUUID == null) return spec.burstSize();
        PlayerAirControlSettings settings = level() instanceof ServerLevel serverLevel
                ? FireControlSystem.getInstance().settings(serverLevel, ownerUUID)
                : FireControlSystem.getInstance().settings(ownerUUID);
        float remainingNominalDamage = seaWeaponSlot.count() * spec.weaponDamage();
        if (remainingNominalDamage < settings.minimumEffectiveDamage()) return seaWeaponSlot.count();
        return Math.max(1, Math.min(settings.bombsPerPass(), spec.burstSize()));
    }

    private Vec3 calculateWeaponOffset(int index, int total) {
        if (total <= 1 || attackSolution == null) return Vec3.ZERO;
        Vec3 side = new Vec3(-attackSolution.attackDirection().z, 0, attackSolution.attackDirection().x);
        double centered = index - (total - 1) / 2.0;
        return side.scale(centered * 1.3);
    }

    private void spawnSeaWeapon(Vec3 offset) {
        if (attackSolution == null) return;
        Vec3 spawnPos = position().add(0, -1.0, 0).add(offset);
        Vec3 velocity = getDeltaMovement();
        AmmoType ammoType = seaWeaponSlot.ammoType();
        String weaponType = ammoType.name();
        UUID targetUUID = null;

        if (ammoType == AmmoType.AERIAL_TORPEDO) {
            velocity = attackSolution.attackDirection().scale(0.85).add(0, -0.08, 0);
        } else if (ammoType == AmmoType.ROCKET) {
            velocity = attackSolution.attackDirection().scale(1.7).add(0, -0.04, 0);
        } else if (spec.seaAttackModes().contains(SeaAttackMode.DIVE_BOMBING)) {
            weaponType = "GUIDED_BOMB";
            if (level() instanceof ServerLevel serverLevel && currentTarget != null) {
                Entity target = currentTarget.resolveEntity(serverLevel);
                if (target != null && target.isAlive()) {
                    targetUUID = target.getUUID();
                }
            }
        } else if (spec.seaAttackModes().contains(SeaAttackMode.LEVEL_BOMBING)) {
            double scatter = 0.035;
            velocity = velocity.add(
                    level().random.triangle(0.0, scatter),
                    0.0,
                    level().random.triangle(0.0, scatter)
            );
        }

        BombEntity bomb = new BombEntity(level(), spawnPos, velocity,
                spec.explosionRadius(), spec.weaponDamage(), weaponType, targetUUID);
        level().addFreshEntity(bomb);
    }

    private void shootAirTarget(Entity target) {
        if (airWeaponSlot.consume(12) <= 0) { syncAmmoData(); return; }
        syncAmmoData();
        if (target instanceof LivingEntity living) {
            DamageSource source = damageSources().mobAttack(this);
            living.hurt(source, 4.0F);
        }
    }

    // ==================== 移动辅助 ====================

    private void hoverAround(Vec3 center, double radius) {
        double angle = orbitPhase + tickCount * (Math.PI / 90.0);
        Vec3 desired = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
        double hoverSpeed = spec.speed() * 0.55;
        if (position().distanceToSqr(desired) > HOVER_THRESHOLD * HOVER_THRESHOLD) {
            flyTowards(desired, hoverSpeed);
        } else {
            // 保持最低速度，避免到达目标点后速度归零导致抽搐
            Vec3 vel = getDeltaMovement();
            Vec3 damped = vel.scale(0.75);
            double minSpeed = hoverSpeed * 0.25;
            if (damped.lengthSqr() < minSpeed * minSpeed) {
                Vec3 tangent = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
                damped = tangent.scale(minSpeed);
            }
            setDeltaMovement(damped);
        }
    }

    private void flyTowards(Vec3 target, double speed) {
        Vec3 direction = target.subtract(position());
        if (direction.lengthSqr() < 0.0001) { setDeltaMovement(getDeltaMovement().scale(0.8)); return; }
        Vec3 desiredMotion = direction.normalize().scale(speed);
        // 平滑过渡：当前运动向目标运动插值，避免每帧突变导致抽搐
        Vec3 current = getDeltaMovement();
        Vec3 smoothed = current.scale(0.6).add(desiredMotion.scale(0.4));
        // 确保速度不低于目标速度的 70%
        if (smoothed.lengthSqr() < speed * speed * 0.49) {
            smoothed = desiredMotion.scale(0.8);
        }
        setDeltaMovement(smoothed);
        Vec3 displayMotion = smoothed.lengthSqr() > 0.0001 ? smoothed : desiredMotion;
        setYRot((float) (Math.atan2(displayMotion.z, displayMotion.x) * 180.0 / Math.PI) - 90);
        setXRot((float) (Math.asin(-displayMotion.normalize().y) * 180.0 / Math.PI));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    // ==================== 弹药辅助 ====================

    private boolean hasSeaAmmo() {
        return seaWeaponSlot.hasAmmo();
    }

    public boolean canAttackEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !(entity instanceof Enemy)
                || entity instanceof AircraftEntity) return false;
        if (spec.seaAttackModes().contains(SeaAttackMode.ASW_BOMBING)) return isUnderwaterHostile(living);
        if (seaWeaponSlot.ammoType() == AmmoType.AERIAL_TORPEDO) return isWaterTarget(living);
        return true;
    }

    public boolean preferredTargetType(Entity entity) {
        if (spec.seaAttackModes().contains(SeaAttackMode.ASW_BOMBING))
            return entity instanceof LivingEntity living && isUnderwaterHostile(living);
        if (seaWeaponSlot.ammoType() == AmmoType.AERIAL_TORPEDO) return isWaterTarget(entity);
        return true;
    }

    private boolean isWaterTarget(Entity entity) {
        return entity.isInWaterOrBubble()
                || level().getFluidState(entity.blockPosition()).is(net.minecraft.tags.FluidTags.WATER)
                || level().getFluidState(entity.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER);
    }

    public boolean rearmFromInventory(ServerPlayer owner) {
        int oldSea = seaWeaponSlot.count();
        int oldAir = airWeaponSlot.count();
        if (owner.getAbilities().instabuild) {
            seaWeaponSlot.setCount(spec.seaAmmoCapacity());
            airWeaponSlot.setCount(spec.magazineCapacity());
        } else {
            restockFromInventory(seaWeaponSlot, owner);
            restockFromInventory(airWeaponSlot, owner);
        }
        syncAmmoData();
        return oldSea != seaWeaponSlot.count() || oldAir != airWeaponSlot.count();
    }

    private void restockFromInventory(WeaponSlot slot, ServerPlayer owner) {
        if (slot.capacity() <= 0) return;
        var inventory = owner.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && slot.count() < slot.capacity(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!(stack.getItem() instanceof AviationAmmoItem ammo) || ammo.ammoType() != slot.ammoType()) continue;
            if (ammo.ammoType() == AmmoType.MAGAZINE) {
                restockMagazineFromStack(slot, ammo, stack, inventory);
                continue;
            }
            while (!stack.isEmpty() && slot.count() < slot.capacity()) {
                slot.add(Math.max(1, ammo.rounds()));
                stack.shrink(1);
            }
        }
    }

    private void restockMagazineFromStack(WeaponSlot slot, AviationAmmoItem ammo, ItemStack stack,
                                          net.minecraft.world.entity.player.Inventory inventory) {
        while (!stack.isEmpty() && slot.count() < slot.capacity()) {
            int needed = slot.capacity() - slot.count();
            int available = ammo.rounds(stack);
            int taken = Math.min(needed, available);
            slot.add(taken);
            int remaining = available - taken;

            if (remaining <= 0) {
                stack.shrink(1);
                if (!stack.isEmpty()) {
                    stack.remove(ModDataComponents.MAGAZINE_ROUNDS);
                }
            } else if (stack.getCount() == 1) {
                ammo.setMagazineRounds(stack, remaining);
                return;
            } else {
                stack.shrink(1);
                ItemStack partial = new ItemStack(stack.getItem());
                ammo.setMagazineRounds(partial, remaining);
                inventory.add(partial);
                return;
            }
        }
    }

    private static AmmoType defaultSeaAmmoFor(AircraftSpec spec) {
        if (spec.allowedSeaAmmo().contains(AmmoType.AERIAL_BOMB)) return AmmoType.AERIAL_BOMB;
        if (spec.allowedSeaAmmo().contains(AmmoType.AERIAL_TORPEDO)) return AmmoType.AERIAL_TORPEDO;
        if (spec.allowedSeaAmmo().contains(AmmoType.ROCKET)) return AmmoType.ROCKET;
        return AmmoType.AERIAL_BOMB;
    }

    // ==================== 状态同步 ====================

    private void syncAmmoData() {
        this.entityData.set(DATA_SEA_AMMO_COUNT, seaWeaponSlot.count());
        this.entityData.set(DATA_AIR_AMMO_COUNT, airWeaponSlot.count());
    }

    private void setState(AircraftState state) {
        AircraftState oldState = getState();
        if (oldState != state) {
            stateTimer = 0;
            fireTimer = 0;
            targetRefreshTimer = TARGET_REFRESH_INTERVAL;
            if (state == AircraftState.RETURNING) clearAswGlow();
        }
        this.entityData.set(DATA_STATE, state.name());
    }

    @Nullable
    private ServerPlayer getOwnerPlayer() {
        if (ownerUUID == null || !(level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
    }

    // ==================== 公开 API ====================

    public void setTarget(@Nullable Vec3 position) {
        if (position == null || !(level() instanceof ServerLevel serverLevel)) {
            currentTarget = null;
        } else {
            currentTarget = FireControlTarget.position(serverLevel, position);
        }
        attackSolution = null;
        if (position != null) setState(AircraftState.LOCKED);
    }

    public void setOrbitPoint(@Nullable Vec3 orbitPoint) {
        this.orbitPoint = orbitPoint;
        setState(orbitPoint == null ? AircraftState.STANDBY : AircraftState.ORBITING);
    }

    public void recallToOwner() {
        currentTarget = null;
        attackSolution = null;
        dogfightTarget = null;
        setNoGravity(true);
        setState(AircraftState.RETURNING);
    }

    /** 推导当前机型（由武器槽配置决定） */
    public AircraftRole getRole() {
        return AircraftRole.derive(spec.airWeaponMode(), seaWeaponSlot.ammoType(), spec);
    }

    public AircraftState getState() {
        try { return AircraftState.valueOf(entityData.get(DATA_STATE)); }
        catch (IllegalArgumentException e) { return AircraftState.STANDBY; }
    }

    public int getAmmoCount() { return entityData.get(DATA_SEA_AMMO_COUNT); }

    public void setAmmoCount(int count) { seaWeaponSlot.setCount(count); syncAmmoData(); }

    public int getAirAmmoCount() { return entityData.get(DATA_AIR_AMMO_COUNT); }

    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }

    // ==================== NBT 序列化 ====================

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.hasUUID("Owner")) this.ownerUUID = compound.getUUID("Owner");
        if (compound.contains("OrbitPhase")) this.orbitPhase = compound.getDouble("OrbitPhase");

        // 恢复飞机规格
        if (compound.contains("Spec")) {
            this.spec = AircraftSpec.load(compound.getCompound("Spec"));
        }

        if (compound.contains("SeaWeapon")) {
            this.seaWeaponSlot = WeaponSlot.load(compound.getCompound("SeaWeapon"),
                    defaultSeaAmmoFor(spec), spec.seaAmmoCapacity());
        }
        if (compound.contains("AirWeapon")) {
            this.airWeaponSlot = WeaponSlot.load(compound.getCompound("AirWeapon"),
                    AmmoType.MAGAZINE, spec.magazineCapacity());
        }

        if (compound.contains("OrbitX")) {
            this.orbitPoint = new Vec3(compound.getDouble("OrbitX"), compound.getDouble("OrbitY"), compound.getDouble("OrbitZ"));
        }
        if (compound.contains("TargetX") && this.level() instanceof ServerLevel serverLevel) {
            this.currentTarget = FireControlTarget.position(serverLevel,
                    new Vec3(compound.getDouble("TargetX"), compound.getDouble("TargetY"), compound.getDouble("TargetZ")));
        }

        try { setState(AircraftState.valueOf(compound.getString("AircraftState"))); }
        catch (IllegalArgumentException ignored) { setState(AircraftState.STANDBY); }

        // 更新推导出的机型 ID
        this.entityData.set(DATA_ROLE_ID, getRole().id());
        syncAmmoData();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        if (this.ownerUUID != null) compound.putUUID("Owner", this.ownerUUID);
        compound.putDouble("OrbitPhase", this.orbitPhase);
        compound.put("Spec", spec.save());
        compound.putString("AircraftState", getState().name());
        compound.put("SeaWeapon", seaWeaponSlot.save());
        compound.put("AirWeapon", airWeaponSlot.save());

        if (this.orbitPoint != null) {
            compound.putDouble("OrbitX", orbitPoint.x); compound.putDouble("OrbitY", orbitPoint.y); compound.putDouble("OrbitZ", orbitPoint.z);
        }
        if (this.currentTarget != null) {
            Vec3 pos = currentTarget.fallbackPosition();
            compound.putDouble("TargetX", pos.x); compound.putDouble("TargetY", pos.y); compound.putDouble("TargetZ", pos.z);
        }
    }
}
