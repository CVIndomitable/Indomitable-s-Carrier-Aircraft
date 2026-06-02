package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DataComponent 注册器
 * 用于存储飞机状态、弹药数量、锁定目标等数据
 */
public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IndomitableCarrierAircraft.MOD_ID);

    // TODO: Phase 1 - 添加飞机相关的 DataComponent
    // 例如：AIRCRAFT_STATE（状态机状态）、AMMO_COUNT（弹药数量）、TARGET_LIST（锁定目标列表）等
}
