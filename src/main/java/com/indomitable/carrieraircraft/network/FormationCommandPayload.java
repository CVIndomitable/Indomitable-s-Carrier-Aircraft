package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.formation.FormationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * 客户端 → 服务端：编组管理命令。
 *
 * <p>操作类型：
 * <ul>
 *   <li>{@link #SET_LEADER} — 设置/取消长机（切换）</li>
 *   <li>{@link #ADD_TO_GROUP} — 将飞机加入指定编组</li>
 *   <li>{@link #REMOVE_FROM_GROUP} — 将飞机从编组中移除</li>
 *   <li>{@link #CREATE_GROUP} — 新建编组（参数为编组名称）</li>
 * </ul>
 */
public record FormationCommandPayload(byte action, UUID aircraftUUID,
                                      String parameter) implements CustomPacketPayload {

    public static final byte SET_LEADER = 0;
    public static final byte ADD_TO_GROUP = 1;
    public static final byte REMOVE_FROM_GROUP = 2;
    public static final byte CREATE_GROUP = 3;

    public static final Type<FormationCommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    IndomitableCarrierAircraft.MOD_ID, "formation_cmd"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FormationCommandPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeByte(pkt.action);
                        buf.writeUUID(pkt.aircraftUUID);
                        buf.writeUtf(pkt.parameter);
                    },
                    buf -> new FormationCommandPayload(
                            buf.readByte(), buf.readUUID(), buf.readUtf())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FormationCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            FormationManager fm = FormationManager.getInstance();

            switch (payload.action) {
                case SET_LEADER -> {
                    AircraftEntity aircraft = resolveAircraft(player, payload.aircraftUUID);
                    if (aircraft == null) return;
                    UUID currentLeader = fm.getLeaderUUID(player.getUUID());
                    if (payload.aircraftUUID.equals(currentLeader)) {
                        fm.setLeader(player.getUUID(), null);
                        fm.savePlayerData(player.serverLevel(), player.getUUID());
                        player.sendSystemMessage(
                                Component.literal("已取消长机").withStyle(ChatFormatting.YELLOW));
                    } else {
                        fm.setLeader(player.getUUID(), payload.aircraftUUID);
                        fm.savePlayerData(player.serverLevel(), player.getUUID());
                        player.sendSystemMessage(
                                Component.literal("已设置长机: " + aircraft.getRole().displayName())
                                        .withStyle(ChatFormatting.GREEN));
                    }
                }
                case ADD_TO_GROUP -> {
                    if (payload.parameter.isEmpty()) {
                        player.sendSystemMessage(
                                Component.literal("未指定编组").withStyle(ChatFormatting.RED));
                        return;
                    }
                    AircraftEntity aircraft = resolveAircraft(player, payload.aircraftUUID);
                    if (aircraft == null) return;
                    fm.addToGroup(player.getUUID(), payload.aircraftUUID, payload.parameter);
                    fm.savePlayerData(player.serverLevel(), player.getUUID());
                    player.sendSystemMessage(Component.literal(
                            String.format("已将 %s 加入编组 [%s]",
                                    aircraft.getRole().displayName(), payload.parameter))
                            .withStyle(ChatFormatting.GREEN));
                }
                case REMOVE_FROM_GROUP -> {
                    fm.removeFromGroup(player.getUUID(), payload.aircraftUUID);
                    fm.savePlayerData(player.serverLevel(), player.getUUID());
                    AircraftEntity aircraft = resolveAircraft(player, payload.aircraftUUID);
                    String name = aircraft != null ? aircraft.getRole().displayName() : "飞机";
                    player.sendSystemMessage(
                            Component.literal("已将 " + name + " 移出编组")
                                    .withStyle(ChatFormatting.YELLOW));
                }
                case CREATE_GROUP -> {
                    if (payload.parameter.isEmpty()) {
                        player.sendSystemMessage(
                                Component.literal("编组名称不能为空").withStyle(ChatFormatting.RED));
                        return;
                    }
                    fm.createGroup(player.getUUID(), payload.parameter);
                    fm.savePlayerData(player.serverLevel(), player.getUUID());
                    player.sendSystemMessage(
                            Component.literal("已创建编组: " + payload.parameter)
                                    .withStyle(ChatFormatting.GREEN));
                }
            }
        });
    }

    private static AircraftEntity resolveAircraft(ServerPlayer player, UUID aircraftUUID) {
        if (player.serverLevel().getEntity(aircraftUUID) instanceof AircraftEntity aircraft
                && aircraft.isAlive()) {
            return aircraft;
        }
        player.sendSystemMessage(
                Component.literal("找不到该飞机").withStyle(ChatFormatting.RED));
        return null;
    }
}
