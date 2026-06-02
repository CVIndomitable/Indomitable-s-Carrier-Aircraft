package com.indomitable.carrieraircraft.event;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * Mod 事件总线上的事件处理
 * 用于注册实体属性、能力等
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        // TODO: Phase 1 - 注册飞机实体的属性
        // event.put(ModEntityTypes.HORIZONTAL_BOMBER.get(), HorizontalBomberEntity.createAttributes().build());
    }
}
