package com.indomitable.carrieraircraft.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 常用 {@link Vec3} 操作的工具方法，集中避免散落的 {@code multiply(1, 0, 1)} / {@code with(Y, ...)} 风格。
 */
public final class Vec3Utils {
    private Vec3Utils() {}

    /** 返回一个 Y 分量为 0 的副本（投影到 XZ 平面）。 */
    public static Vec3 flatten(Vec3 v) {
        return new Vec3(v.x, 0.0, v.z);
    }

    /** 与 {@code new Vec3(v.x, y, v.z)} 等价，语义化命名。 */
    public static Vec3 withY(Vec3 v, double y) {
        return new Vec3(v.x, y, v.z);
    }

    /** 与 {@code v.with(Direction.Axis.Y, y)} 等价，但避免散布 Axis 枚举引用。 */
    public static Vec3 replaceY(Vec3 v, double y) {
        return withY(v, y);
    }

    public static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /** 便捷包装：避免在 hot path 中重复构造 {@link Direction.Axis} 引用。 */
    public static final Direction.Axis Y_AXIS = Direction.Axis.Y;
}