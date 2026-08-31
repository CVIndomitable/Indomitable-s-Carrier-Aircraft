package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.menu.DebugMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * 客户端 → 服务端：调试工具命令包。
 *
 * <p>携带飞机索引、目标状态、起始/目标坐标和循环标志。
 */
public record DebugCommandPayload(
        byte action,
        int aircraftIndex,
        byte stateOrdinal,
        double startX, double startY, double startZ,
        double targetX, double targetY, double targetZ,
        boolean loop
) implements CustomPacketPayload {

    public static final byte EXECUTE = 0;
    public static final byte STOP = 1;

    public static final Type<DebugCommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndomitableCarrierAircraft.MOD_ID, "debug_cmd"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugCommandPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeByte(pkt.action);
                        buf.writeVarInt(pkt.aircraftIndex);
                        buf.writeByte(pkt.stateOrdinal);
                        buf.writeDouble(pkt.startX);
                        buf.writeDouble(pkt.startY);
                        buf.writeDouble(pkt.startZ);
                        buf.writeDouble(pkt.targetX);
                        buf.writeDouble(pkt.targetY);
                        buf.writeDouble(pkt.targetZ);
                        buf.writeBoolean(pkt.loop);
                    },
                    buf -> new DebugCommandPayload(
                            buf.readByte(),
                            buf.readVarInt(),
                            buf.readByte(),
                            buf.readDouble(), buf.readDouble(), buf.readDouble(),
                            buf.readDouble(), buf.readDouble(), buf.readDouble(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DebugCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!canUseDebugCommands(player)) {
                return;
            }
            executeAction(player, payload);
        });
    }

    private static boolean canUseDebugCommands(ServerPlayer player) {
        return player.getAbilities().instabuild
                && player.containerMenu instanceof DebugMenu
                && player.containerMenu.stillValid(player);
    }

    private static void executeAction(ServerPlayer player, DebugCommandPayload pkt) {
        List<AircraftEntity> aircraft = FormationManager.getInstance()
                .getAircraft(player.serverLevel(), player.getUUID());

        if (pkt.action == STOP) {
            for (AircraftEntity a : aircraft) {
                a.debugStop();
            }
            player.sendSystemMessage(Component.literal("已停止所有调试").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (pkt.action != EXECUTE) {
            return;
        }

        if (pkt.aircraftIndex < 0 || pkt.aircraftIndex >= aircraft.size()) {
            player.sendSystemMessage(Component.literal("无效的飞机索引").withStyle(ChatFormatting.RED));
            return;
        }

        AircraftEntity target = aircraft.get(pkt.aircraftIndex);

        AircraftState[] states = AircraftState.values();
        if (pkt.stateOrdinal < 0 || pkt.stateOrdinal >= states.length) {
            player.sendSystemMessage(Component.literal("无效的状态").withStyle(ChatFormatting.RED));
            return;
        }

        AircraftState state = states[pkt.stateOrdinal];
        if (!Double.isFinite(pkt.startX) || !Double.isFinite(pkt.startY) || !Double.isFinite(pkt.startZ)
                || !Double.isFinite(pkt.targetX) || !Double.isFinite(pkt.targetY) || !Double.isFinite(pkt.targetZ)) {
            player.sendSystemMessage(Component.literal("无效的调试坐标").withStyle(ChatFormatting.RED));
            return;
        }
        Vec3 startPos = new Vec3(pkt.startX, pkt.startY, pkt.startZ);
        Vec3 targetPos = new Vec3(pkt.targetX, pkt.targetY, pkt.targetZ);

        target.debugExecuteState(state, startPos, targetPos, pkt.loop);
        player.sendSystemMessage(Component.literal("调试: " + target.getRole().displayName() + " → ")
                .append(Component.translatable(state.translationKey()))
                .append(Component.literal(String.format(" @ %.0f,%.0f,%.0f %s",
                        startPos.x, startPos.y, startPos.z,
                        pkt.loop ? "[循环]" : "[单次]")))
                .withStyle(ChatFormatting.AQUA));
    }
}
