package com.indomitable.carrieraircraft.entity.ai;

/**
 * 飞机 AI 状态枚举 - Phase 1 MVP
 *
 * 状态转换流程：
 * STANDBY → APPROACH → DROPPING → RETURNING → (移除)
 */
public enum AircraftState {
    /**
     * 待命状态 - 在玩家头顶盘旋
     * 等待火控系统分配目标
     */
    STANDBY,

    /**
     * 赶赴状态 - 飞向目标上方
     * 到达后进入 DROPPING 状态
     */
    APPROACH,

    /**
     * 投弹状态 - 在目标上方投弹
     * 按照固定间隔投弹，弹药耗尽后进入 RETURNING
     */
    DROPPING,

    /**
     * 返航状态 - 飞回玩家位置
     * 到达后移除实体（Phase 1 简化处理，后续版本改为降落到航母）
     */
    RETURNING
}
