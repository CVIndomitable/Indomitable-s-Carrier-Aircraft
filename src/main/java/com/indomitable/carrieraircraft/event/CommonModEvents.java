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
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        IndomitableCarrierAircraft.LOGGER.info("=== EntityAttributeCreationEvent FIRED ===");

        // Phase 1: 注册B-25轰炸机实体属性
        try {
            var entityType = ModEntityTypes.B25_BOMBER.get();
            IndomitableCarrierAircraft.LOGGER.info("Got B-25 bomber entity type: {}", entityType);

            var attributes = BomberAircraftEntity.createAttributes().build();
            IndomitableCarrierAircraft.LOGGER.info("Created attributes: {}", attributes);

            event.put(entityType, attributes);
            IndomitableCarrierAircraft.LOGGER.info("✅ Successfully registered B-25 bomber entity attributes");
        } catch (Exception e) {
            IndomitableCarrierAircraft.LOGGER.error("❌ Failed to register B-25 bomber attributes", e);
            throw e;
        }
    }
}
