package com.indomitable.carrieraircraft.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命中/击杀/未命中聊天通知的按玩家开关状态，默认开启
 * 从皮兰港项目移植
 */
public final class HitNotifier {
    private HitNotifier() {}

    private static final Set<UUID> optedOut = ConcurrentHashMap.newKeySet();

    public static boolean isEnabled(UUID playerId) {
        return !optedOut.contains(playerId);
    }

    public static void setEnabled(UUID playerId, boolean enabled) {
        if (enabled) optedOut.remove(playerId);
        else optedOut.add(playerId);
    }

    public static void onPlayerLogout(UUID playerId) {
        optedOut.remove(playerId);
    }

    public static void send(Player player, Component message) {
        if (!isEnabled(player.getUUID())) return;
        player.sendSystemMessage(message);
    }
}
