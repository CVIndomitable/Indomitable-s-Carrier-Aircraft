package com.indomitable.carrieraircraft.targeting;

import com.indomitable.carrieraircraft.aircraft.AutoLockMode;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class TargetingSubsystem {
    private static final double AUTO_LOCK_RANGE = 160.0;

    private TargetingSubsystem() {}

    @Nullable
    public static FireControlTarget resolveTarget(AircraftEntity aircraft, ServerLevel level) {
        UUID ownerId = aircraft.getOwnerUUID();
        if (ownerId == null) return null;

        FireControlSystem fireControl = FireControlSystem.getInstance();
        fireControl.pruneStaleTargets(ownerId, level);

        FireControlTarget fireControlTarget = fireControl.getAssignedTarget(level, ownerId, aircraft.getUUID());
        if (fireControlTarget != null && fireControlTarget.inDimension(level)) {
            Entity entity = fireControlTarget.resolveEntity(level);
            if (!fireControlTarget.isEntityTarget() || entity != null && entity.isAlive()) {
                return fireControlTarget;
            }
        }

        AutoLockMode mode = fireControl.settings(level, ownerId).autoLockMode();
        Entity autoTarget = findAutomaticTarget(aircraft, level, mode);
        return autoTarget == null ? null : FireControlTarget.entity(autoTarget);
    }

    @Nullable
    private static Entity findAutomaticTarget(AircraftEntity aircraft, ServerLevel level, AutoLockMode mode) {
        AABB search = aircraft.getBoundingBox().inflate(AUTO_LOCK_RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                search,
                entity -> entity.isAlive()
                        && !(entity instanceof Player)
                        && entity != aircraft
                        && FriendlyFireFilter.canAttack(aircraft, entity)
                        && aircraft.canAttackEntity(entity)
        );

        if (candidates.isEmpty()) return null;

        return switch (mode) {
            case STRONGEST -> candidates.stream()
                    .max(Comparator.comparingDouble(ThreatEvaluator::score))
                    .orElse(null);
            case SPREAD -> {
                int index = Math.floorMod(aircraft.getUUID().hashCode(), candidates.size());
                candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(aircraft)));
                yield candidates.get(index);
            }
            case TYPE_FILTER -> candidates.stream()
                    .filter(aircraft::preferredTargetType)
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(aircraft)))
                    .orElseGet(() -> candidates.stream()
                            .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(aircraft)))
                            .orElse(null));
            case FOCUS, NEAREST -> candidates.stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(aircraft)))
                    .orElse(null);
        };
    }
}
