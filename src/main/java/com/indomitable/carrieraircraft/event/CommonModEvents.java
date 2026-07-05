package com.indomitable.carrieraircraft.event;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.aircraft.AircraftSpecLoader;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.network.AddTargetPayload;
import com.indomitable.carrieraircraft.network.CommandPayload;
import com.indomitable.carrieraircraft.network.DebugCommandPayload;
import com.indomitable.carrieraircraft.network.FormationCommandPayload;
import com.indomitable.carrieraircraft.network.TerminalSyncPayload;
import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Mod 事件总线上的事件处理
 * 用于注册实体属性、网络包等
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        IndomitableCarrierAircraft.LOGGER.info("=== EntityAttributeCreationEvent FIRED ===");

        try {
            var entityType = ModEntityTypes.AIRCRAFT.get();
            IndomitableCarrierAircraft.LOGGER.info("Got AIRCRAFT entity type: {}", entityType);

            var attributes = AircraftEntity.createAttributes().build();
            IndomitableCarrierAircraft.LOGGER.info("Created attributes: {}", attributes);

            event.put(entityType, attributes);
            IndomitableCarrierAircraft.LOGGER.info("Successfully registered AIRCRAFT entity attributes");
        } catch (Exception e) {
            IndomitableCarrierAircraft.LOGGER.error("Failed to register AIRCRAFT attributes", e);
            throw e;
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(IndomitableCarrierAircraft.MOD_ID);
        registrar.playToServer(
                CommandPayload.TYPE,
                CommandPayload.CODEC,
                CommandPayload::handle
        );
        registrar.playToServer(
                DebugCommandPayload.TYPE,
                DebugCommandPayload.CODEC,
                DebugCommandPayload::handle
        );
        registrar.playToServer(
                FormationCommandPayload.TYPE,
                FormationCommandPayload.CODEC,
                FormationCommandPayload::handle
        );
        registrar.playToServer(
                AddTargetPayload.TYPE,
                AddTargetPayload.CODEC,
                AddTargetPayload::handle
        );

        // 服务端 → 客户端
        registrar.playToClient(
                TerminalSyncPayload.TYPE,
                TerminalSyncPayload.CODEC,
                TerminalSyncPayload::handle
        );

        IndomitableCarrierAircraft.LOGGER.info("Registered network payloads");
    }

    @SubscribeEvent
    public static void registerReloadListeners(AddReloadListenerEvent event) {
        event.addListener(AircraftSpecLoader.getInstance());
        IndomitableCarrierAircraft.LOGGER.info("Registered aircraft spec reload listener");
    }
}
