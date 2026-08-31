package com.indomitable.carrieraircraft.event;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.combat.HitNotifier;
import com.indomitable.carrieraircraft.firecontrol.CameraStateTracker;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.formation.FormationManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
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
        HitNotifier.onPlayerLogout(playerId);
        CameraStateTracker.getInstance().clearPlayer(playerId);
    }

    /**
     * 新存档启动时清理全部单例状态。
     *
     * <p>在单机模式下，玩家从世界 A 切到世界 B 时 MinecraftServer 不会重启，
     * 仅触发 ServerStarting/Started 而不一定触发 ServerStopped（取决于 Forge 内部生命周期）。
     * 这里双保险：ServerStartedEvent 与 ServerStoppedEvent 都调用同一清理流程。
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        clearAllSingletons(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clearAllSingletons(event.getServer());
    }

    /** 清理所有 JVM 级单例的玩家/存档数据。 */
    private static void clearAllSingletons(net.minecraft.server.MinecraftServer server) {
        FireControlSystem.getInstance().clearAll();
        if (server != null) {
            FormationManager.getInstance().releaseAllForcedChunks(server);
        }
        FormationManager.getInstance().clear();
        CameraStateTracker.getInstance().clearAll();
        HitNotifier.clearAll();
    }
}
