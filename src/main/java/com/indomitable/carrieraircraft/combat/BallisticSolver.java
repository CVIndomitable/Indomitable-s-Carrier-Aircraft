package com.indomitable.carrieraircraft.combat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 弹道解算引擎：给定初速度、阻力、重力、目标距离，计算最佳发射仰角。
 * 使用网格搜索 + 三分法搜索 [-45°, 45°]（含负仰角以支持高低差），每次迭代模拟数值弹道。
 *
 * <p>从皮兰港项目移植，用于计算飞机投弹时的弹道。
 * 物理模型与炸弹实体的运动一致。
 */
public final class BallisticSolver {

    /** 默认重力值（blocks/tick²） */
    public static final double DEFAULT_GRAVITY = 0.05;

    /** 最大迭代次数 */
    private static final int MAX_ITERATIONS = 100;

    /** 精度阈值（blocks） */
    private static final double ACCURACY_THRESHOLD = 0.5;

    /** 无解阈值（blocks）：误差超过此值视为打不到 */
    private static final double NO_SOLUTION_THRESHOLD = 50.0;

    /** 最大模拟步数 */
    private static final int MAX_STEPS = 2000;

    /** 缓存大小 */
    private static final int CACHE_SIZE = 512;

    private static final Map<SolutionKey, Double> cache = createLRUCache();
    private static boolean cacheEnabled = true;

    /** 创建 LRU 缓存 */
    private static Map<SolutionKey, Double> createLRUCache() {
        return new LinkedHashMap<SolutionKey, Double>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<SolutionKey, Double> eldest) {
                return size() > CACHE_SIZE;
            }
        };
    }

    /** 线程安全的缓存读（弹道解算可能从多个渲染线程调用） */
    private static Double cacheGet(SolutionKey key) {
        synchronized (cache) { return cache.get(key); }
    }

    /** 线程安全的缓存写 */
    private static void cachePut(SolutionKey key, double value) {
        synchronized (cache) { cache.put(key, value); }
    }

    /** 缓存键量化步长（格）。0.1 格精度足够瞄准使用，可大幅提高缓存命中率。 */
    private static final double DISTANCE_QUANTUM = 0.1;

    private BallisticSolver() {}

    /** 弹道解算入口 */
    public static double solve(double initialSpeed, double dragCoeff, double gravity,
                                double horizontalDist, double verticalDist) {
        if (initialSpeed <= 0 || horizontalDist <= 0) return 45.0 * Math.PI / 180.0;

        // 量化距离以提高缓存命中率
        double qHDist = quantize(horizontalDist);
        double qVDist = quantize(verticalDist);

        if (cacheEnabled) {
            SolutionKey key = new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist);
            Double cached = cacheGet(key);
            if (cached != null) return cached;
        }

        // 网格搜索 + 三分法：支持负仰角（高低差场景）
        // 先用粗网格扫描找到误差最小区域，再用三分法精细搜索
        double searchLow = -Math.PI / 4;   // -45°：允许俯射
        double searchHigh = Math.PI / 4;   //  45°：低弹道上界
        double bestAngle = 45.0 * Math.PI / 180.0;
        double minError = Double.MAX_VALUE;

        // 粗网格扫描（64 步），找到全局最优区域
        int gridSteps = 64;
        double gridStep = (searchHigh - searchLow) / gridSteps;
        double bestGridAngle = searchLow;
        for (int g = 0; g <= gridSteps; g++) {
            double a = searchLow + g * gridStep;
            double err = Math.abs(simulate(initialSpeed, a, dragCoeff, gravity, horizontalDist) - verticalDist);
            if (err < minError) {
                minError = err;
                bestAngle = a;
                bestGridAngle = a;
            }
        }

        if (minError > ACCURACY_THRESHOLD) {
            // 以网格最优点为中心，三分法精细搜索
            double refineRadius = gridStep * 2;
            double lowAngle = Math.max(searchLow, bestGridAngle - refineRadius);
            double highAngle = Math.min(searchHigh, bestGridAngle + refineRadius);

            for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
                if (highAngle - lowAngle < 1e-7) break;

                double mid1 = lowAngle + (highAngle - lowAngle) / 3;
                double mid2 = highAngle - (highAngle - lowAngle) / 3;

                double err1 = Math.abs(simulate(initialSpeed, mid1, dragCoeff, gravity, horizontalDist) - verticalDist);
                double err2 = Math.abs(simulate(initialSpeed, mid2, dragCoeff, gravity, horizontalDist) - verticalDist);

                if (err1 < minError) { minError = err1; bestAngle = mid1; }
                if (err2 < minError) { minError = err2; bestAngle = mid2; }

                if (err1 < ACCURACY_THRESHOLD || err2 < ACCURACY_THRESHOLD) break;

                if (err1 > err2) {
                    lowAngle = mid1;
                } else {
                    highAngle = mid2;
                }
            }
        }

        // 无解（打不到目标）时返回最大射程角
        if (minError > NO_SOLUTION_THRESHOLD) {
            bestAngle = calculateMaxRangeAngle(initialSpeed, dragCoeff, gravity);
        }

        if (cacheEnabled) {
            cachePut(new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist), bestAngle);
        }
        return bestAngle;
    }

    /**
     * 数值弹道模拟，模型与炸弹实体的运动一致。
     *
     * 物理模型：
     * 1. 先应用阻力到速度（vx 和 vy 分别缩放）
     * 2. 再应用重力到 vy
     * 3. 最后更新位置
     *
     * 时间步长固定为 1.0 tick。
     */
    private static double simulate(double v0, double angle, double dragCoeff, double gravity, double targetX) {
        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle);
        double x = 0, y = 0;
        double dragFactor = Math.max(0.1, 1.0 - dragCoeff);

        for (int step = 0; step < MAX_STEPS; step++) {
            // 1. 应用阻力
            vx *= dragFactor;
            vy *= dragFactor;

            // 2. 应用重力
            vy -= gravity;

            // 3. 位置更新
            x += vx;
            y += vy;

            // 到达目标水平距离
            if (x >= targetX) return y;
            // 落地（远低于发射点，放宽以支持大高低差场景）
            if (y < -300) break;
        }
        return y;
    }

    /** 计算最大射程发射角（有阻力时略高于 45°） */
    public static double calculateMaxRangeAngle(double v0, double dragCoeff, double gravity) {
        if (dragCoeff <= 0) return 45.0 * Math.PI / 180.0;
        double offset = Math.min(10, dragCoeff * 100);
        return (45.0 + offset) * Math.PI / 180.0;
    }

    /** 将距离量化到 DISTANCE_QUANTUM 精度，提高缓存命中率。 */
    private static double quantize(double value) {
        return Math.round(value / DISTANCE_QUANTUM) * DISTANCE_QUANTUM;
    }

    /** 强制开关缓存（用于调试/性能测试）。 */
    public static void setCacheEnabled(boolean enabled) {
        cacheEnabled = enabled;
        if (!enabled) cache.clear();
    }

    public static void clearCache() {
        cache.clear();
    }

    /** 缓存键：包含所有影响弹道的参数，距离已量化 */
    private record SolutionKey(double speed, double drag, double gravity, double hDist, double vDist) {}
}
