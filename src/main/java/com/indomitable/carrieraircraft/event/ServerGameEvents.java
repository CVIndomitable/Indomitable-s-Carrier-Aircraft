package com.indomitable.carrieraircraft.event;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.formation.FormationManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.UUID;

/**
 * 服务器生命周期事件。
 *
 * <p>火控系统与编组管理器是 JVM 级静态单例，必须在玩家下线和服务器停止时清理，
 * 否则长期运行会泄漏离线玩家数据，单机模式下还会把状态带进下一个存档。
 */
@EventBusSubscriber(modid = IndomitableCarrierAircraft.MOD_ID)
public class ServerGameEvents {

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        if (event.getEntity() instanceof ServerPlayer player) {
            FormationManager.getInstance().releaseForcedChunks(player.serverLevel(), playerId);
        }
        FireControlSystem.getInstance().clearPlayer(playerId);
        FormationManager.getInstance().clearPlayer(playerId);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FireControlSystem.getInstance().clearAll();
        FormationManager.getInstance().clear();
    }
}
