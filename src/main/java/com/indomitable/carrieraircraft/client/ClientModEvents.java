package com.indomitable.carrieraircraft.client;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.client.model.B25Model;
import com.indomitable.carrieraircraft.client.renderer.BombEntityRenderer;
import com.indomitable.carrieraircraft.client.renderer.BomberAircraftRenderer;
import com.indomitable.carrieraircraft.registry.ModEntityTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端专用的 Mod 事件处理
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Phase 1: 注册B-25和炸弹渲染器
        event.registerEntityRenderer(ModEntityTypes.B25_BOMBER.get(), BomberAircraftRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.BOMB.get(), BombEntityRenderer::new);

        IndomitableCarrierAircraft.LOGGER.info("Registered entity renderers");
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册B-25模型层
        event.registerLayerDefinition(B25Model.LAYER_LOCATION, B25Model::createBodyLayer);

        IndomitableCarrierAircraft.LOGGER.info("Registered B-25 model layer");
    }
}
