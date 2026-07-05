package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.entity.BombEntity;
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

    // ==================== 飞机与航空弹药 ====================

    /**
     * 统一飞机实体类型。
     * 机型由武器槽配置决定，不使用硬编码枚举。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<AircraftEntity>> AIRCRAFT =
            ENTITY_TYPES.register("aircraft", () -> {
                IndomitableCarrierAircraft.LOGGER.info("Creating AIRCRAFT entity type");
                return EntityType.Builder.of(
                        AircraftEntity::new,
                        MobCategory.MISC
                )
                .sized(2.0F, 1.0F) // 宽度2格，高度1格
                .clientTrackingRange(128)
                .updateInterval(1)
                .build("aircraft");
            });

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
