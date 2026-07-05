package com.indomitable.carrieraircraft.entity.ai;

/**
 * 飞机 AI 状态枚举。
 *
 * <p>这些值既是服务端状态机的分支，也会同步给客户端用于渲染/调试。
 */
public enum AircraftState {
    /**
     * 待命状态 - 在玩家头顶盘旋
     * 等待火控系统分配目标
     */
    STANDBY,

    /**
     * 盘旋待命 - 在玩家或控制终端指定的固定盘旋点等待命令
     */
    ORBITING,

    /**
     * 锁定 - 解析火控/自动锁定目标并计算三坐标
     */
    LOCKED,

    /**
     * 赶赴状态 - 飞向目标上方
     * 到达后进入 DROPPING 状态
     */
    APPROACH,

    /**
     * 打击中 - 进入攻击航段，稳定航向后释放弹药
     */
    ATTACKING,

    /**
     * 投弹状态 - 在目标上方投弹
     * 按照固定间隔投弹，弹药耗尽后进入 RETURNING
     */
    DROPPING,

    /**
     * 确认打击效果 - 飞越目标后判断是否复攻、换目标或返航
     */
    POST_ATTACK,

    /**
     * 空战 - 前射型飞机主动追击空中目标
     */
    DOGFIGHT,

    /**
     * 返航状态 - 飞回玩家位置，直接降落并回收
     */
    RETURNING
}
