package com.indomitable.carrieraircraft.client;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端专用的 Mod 事件处理
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // TODO: Phase 1 - 注册飞机实体渲染器
    }
}
