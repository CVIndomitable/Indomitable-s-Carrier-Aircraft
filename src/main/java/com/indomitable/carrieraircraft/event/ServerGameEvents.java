package com.indomitable.carrieraircraft.event;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Forge 事件总线上的游戏事件处理
 * 用于处理服务器 tick、玩家交互等
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID)
public class ServerGameEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // TODO: Phase 1 - 处理飞机状态机更新等
    }
}
