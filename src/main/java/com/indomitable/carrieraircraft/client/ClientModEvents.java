package com.indomitable.carrieraircraft.client;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.client.model.B25Model;
import com.indomitable.carrieraircraft.client.renderer.BombEntityRenderer;
import com.indomitable.carrieraircraft.client.renderer.AircraftRenderer;
import com.indomitable.carrieraircraft.client.screen.ControlTerminalScreen;
import com.indomitable.carrieraircraft.client.screen.DebugScreen;
import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import com.indomitable.carrieraircraft.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端专用的 Mod 事件处理
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.AIRCRAFT.get(), AircraftRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.BOMB.get(), BombEntityRenderer::new);

        IndomitableCarrierAircraft.LOGGER.info("Registered entity renderers");
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(B25Model.LAYER_LOCATION, B25Model::createBodyLayer);

        IndomitableCarrierAircraft.LOGGER.info("Registered B-25 model layer");
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CONTROL_TERMINAL.get(), ControlTerminalScreen::new);
        event.register(ModMenuTypes.DEBUG.get(), DebugScreen::new);

        IndomitableCarrierAircraft.LOGGER.info("Registered control terminal and debug screens");
    }
}
