package com.indomitable.carrieraircraft.event;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.entity.BomberAircraftEntity;
import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * Mod 事件总线上的事件处理
 * 用于注册实体属性、能力等
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        // Phase 1: 注册轰炸机实体属性
        event.put(ModEntityTypes.BOMBER_AIRCRAFT.get(), BomberAircraftEntity.createAttributes().build());
    }
}
