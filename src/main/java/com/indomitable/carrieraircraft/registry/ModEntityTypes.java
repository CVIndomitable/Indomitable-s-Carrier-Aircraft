package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.entity.BombEntity;
import com.indomitable.carrieraircraft.entity.BomberAircraftEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体类型注册器
 * 用于注册飞机实体和弹药实体
 */
public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IndomitableCarrierAircraft.MOD_ID);

    // ==================== Phase 1: 水平轰炸机与炸弹 ====================

    public static final DeferredHolder<EntityType<?>, EntityType<BomberAircraftEntity>> BOMBER_AIRCRAFT =
            ENTITY_TYPES.register("bomber_aircraft", () -> EntityType.Builder.of(
                    BomberAircraftEntity::new,
                    MobCategory.MISC
            )
            .sized(2.0F, 1.0F) // 宽度2格，高度1格
            .clientTrackingRange(128)
            .updateInterval(1)
            .build("bomber_aircraft"));

    public static final DeferredHolder<EntityType<?>, EntityType<BombEntity>> BOMB =
            ENTITY_TYPES.register("bomb", () -> EntityType.Builder.<BombEntity>of(
                    BombEntity::new,
                    MobCategory.MISC
            )
            .sized(0.5F, 0.5F) // 小型抛射物
            .clientTrackingRange(64)
            .updateInterval(1)
            .build("bomb"));
}
