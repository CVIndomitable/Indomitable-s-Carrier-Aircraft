package com.indomitable.carrieraircraft.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 舰载机武器命中/击杀聊天通知的按玩家开关状态。
 *
 * <p>默认开启，玩家可通过控制终端关闭。
 * 通知以系统消息形式发送（类似字幕位置），不干扰聊天栏。
 */
public final class HitNotifier {
    private HitNotifier() {}

    /** 已关闭通知的玩家 UUID 集合 */
    private static final Set<UUID> optedOut = ConcurrentHashMap.newKeySet();

    /**
     * 检查玩家是否启用了命中反馈。
     *
     * @param playerId 玩家 UUID
     * @return {@code true} 如果玩家启用了通知
     */
    public static boolean isEnabled(UUID playerId) {
        return !optedOut.contains(playerId);
    }

    /**
     * 设置玩家的命中反馈开关。
     *
     * @param playerId 玩家 UUID
     * @param enabled {@code true} 启用，{@code false} 禁用
     */
    public static void setEnabled(UUID playerId, boolean enabled) {
        if (enabled) {
            optedOut.remove(playerId);
        } else {
            optedOut.add(playerId);
        }
    }

    /**
     * 玩家下线时清理其状态。
     *
     * @param playerId 玩家 UUID
     */
    public static void onPlayerLogout(UUID playerId) {
        optedOut.remove(playerId);
    }

    /** 清理所有玩家偏好。在新存档启动或服务器停止时调用，避免跨存档泄漏。 */
    public static void clearAll() {
        optedOut.clear();
    }

    /**
     * 发送命中反馈消息给玩家。
     *
     * <p>如果玩家已关闭通知，则不发送。
     * 消息以系统消息形式显示，位于屏幕底部（类似字幕位置）。
     *
     * @param player 目标玩家
     * @param message 消息内容
     */
    public static void send(Player player, Component message) {
        if (!isEnabled(player.getUUID())) {
            return;
        }
        player.sendSystemMessage(message);
    }
}
