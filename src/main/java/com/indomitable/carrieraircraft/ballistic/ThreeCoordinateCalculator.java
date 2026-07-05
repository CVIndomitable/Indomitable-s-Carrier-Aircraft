package com.indomitable.carrieraircraft.ballistic;

import com.indomitable.carrieraircraft.aircraft.AircraftSpec;
import com.indomitable.carrieraircraft.aircraft.AmmoType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 三坐标打击解算。
 *
 * <p>炸弹的下落时间与水平漂移按 {@link com.indomitable.carrieraircraft.entity.BombEntity}
 * 的逐 tick 物理（先乘阻力再加重力）模拟，保证解算结果与实际弹道一致。
 */
public final class ThreeCoordinateCalculator {
    /** 与 BombEntity 一致的物理常量 */
    private static final double BOMB_GRAVITY = 0.05;
    private static final double BOMB_DRAG = 0.98;
    private static final int MAX_SIM_TICKS = 400;

    /** 投弹航段相对目标的最低高度差 */
    private static final double MIN_DROP_ALTITUDE = 12.0;

    /** ATTACKING 航段的减速系数，与 AircraftEntity#tickAttacking 保持一致 */
    private static final double ATTACK_RUN_SPEED_FACTOR = 0.92;

    private ThreeCoordinateCalculator() {}

    public static AttackSolution solve(Vec3 aircraftPos, Vec3 targetPos, @Nullable Entity targetEntity,
                                       AircraftSpec spec, AmmoType ammoType) {
        double dropAltitude = Math.max(MIN_DROP_ALTITUDE, spec.attackHeight());
        double attackY = targetPos.y + dropAltitude;

        double flightTime = estimateWeaponFlightTime(aircraftPos, targetPos, dropAltitude, ammoType);
        Vec3 targetVelocity = targetEntity == null ? Vec3.ZERO : targetEntity.getDeltaMovement();
        Vec3 impactPoint = targetPos.add(targetVelocity.scale(flightTime));

        Vec3 horizontal = impactPoint.subtract(aircraftPos).multiply(1.0, 0.0, 1.0);
        if (horizontal.lengthSqr() < 0.001) {
            horizontal = new Vec3(0, 0, 1);
        }
        Vec3 attackDirection = horizontal.normalize();

        double releaseDistance = calculateReleaseDistance(spec, ammoType, flightTime);
        Vec3 dropPoint = impactPoint
                .subtract(attackDirection.scale(releaseDistance))
                .with(net.minecraft.core.Direction.Axis.Y, attackY);
        Vec3 turnPoint = impactPoint
                .add(attackDirection.scale(spec.turnDistance()))
                .with(net.minecraft.core.Direction.Axis.Y, attackY);

        return new AttackSolution(targetPos, impactPoint, dropPoint, turnPoint,
                attackDirection, flightTime, releaseDistance);
    }

    /** 按 BombEntity 的物理逐 tick 模拟（v' = v*drag + g），返回落下 height 所需 tick 数 */
    private static double simulateBombFallTicks(double height) {
        double velocity = 0.0;
        double fallen = 0.0;
        int ticks = 0;
        while (fallen < height && ticks < MAX_SIM_TICKS) {
            velocity = velocity * BOMB_DRAG + BOMB_GRAVITY;
            fallen += velocity;
            ticks++;
        }
        return ticks;
    }

    private static double estimateWeaponFlightTime(Vec3 aircraftPos, Vec3 targetPos,
                                                   double dropAltitude, AmmoType ammoType) {
        return switch (ammoType) {
            case AERIAL_BOMB -> simulateBombFallTicks(dropAltitude);
            case AERIAL_TORPEDO -> Math.max(20.0, aircraftPos.distanceTo(targetPos) / 0.75);
            case ROCKET -> Math.max(8.0, aircraftPos.distanceTo(targetPos) / 1.6);
            case MAGAZINE -> Math.max(4.0, aircraftPos.distanceTo(targetPos) / 2.0);
        };
    }

    /**
     * 投放提前距离。炸弹离机后继承机速并按阻力衰减，水平漂移为等比数列求和；
     * 鱼雷/火箭有自身动力，沿用固定离轴距离。
     */
    private static double calculateReleaseDistance(AircraftSpec spec, AmmoType ammoType, double flightTime) {
        return switch (ammoType) {
            case AERIAL_BOMB -> {
                double releaseSpeed = spec.speed() * ATTACK_RUN_SPEED_FACTOR;
                double dragSum = (1.0 - Math.pow(BOMB_DRAG, flightTime)) / (1.0 - BOMB_DRAG);
                yield Math.max(2.0, releaseSpeed * dragSum);
            }
            case AERIAL_TORPEDO -> 28.0;
            case ROCKET -> 18.0;
            case MAGAZINE -> 0.0;
        };
    }

    public record AttackSolution(
            Vec3 targetPoint,
            Vec3 impactPoint,
            Vec3 dropPoint,
            Vec3 turnPoint,
            Vec3 attackDirection,
            double estimatedWeaponFlightTime,
            double releaseDistance
    ) {}
}
