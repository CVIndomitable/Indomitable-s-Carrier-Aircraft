package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：目标管理命令（删除全部目标/删除指定目标）。
 */
public record ManageTargetsPayload(Action action, int targetIndex) implements CustomPacketPayload {

    public static final Type<ManageTargetsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    IndomitableCarrierAircraft.MOD_ID, "manage_targets"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManageTargetsPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeVarInt(pkt.action.ordinal());
                        buf.writeVarInt(pkt.targetIndex);
                    },
                    buf -> new ManageTargetsPayload(
                            Action.values()[buf.readVarInt()],
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManageTargetsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ControlTerminalMenu)
                    || !player.containerMenu.stillValid(player)) {
                return;
            }

            FireControlSystem fcs = FireControlSystem.getInstance();

            switch (payload.action) {
                case CLEAR_ALL -> {
                    fcs.clearTarget(player.getUUID());
                    player.sendSystemMessage(Component.literal("已清除所有目标")
                            .withStyle(ChatFormatting.GREEN));
                }
                case REMOVE_BY_INDEX -> {
                    if (payload.targetIndex < 0) {
                        player.sendSystemMessage(Component.literal("无效的目标索引")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                    boolean removed = fcs.removeTarget(player.getUUID(), payload.targetIndex);
                    if (removed) {
                        player.sendSystemMessage(Component.literal(
                                String.format("已删除目标 #%d", payload.targetIndex + 1))
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        player.sendSystemMessage(Component.literal("目标不存在")
                                .withStyle(ChatFormatting.RED));
                    }
                }
            }
        });
    }

    public enum Action {
        /** 清除所有目标 */
        CLEAR_ALL,
        /** 删除指定索引的目标 */
        REMOVE_BY_INDEX
    }
}
