package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体类型注册器
 * 用于注册飞机实体和弹药实体
 */
public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IndomitableCarrierAircraft.MOD_ID);

    // TODO: Phase 1 - 添加水平轰炸机实体和炸弹实体
}
