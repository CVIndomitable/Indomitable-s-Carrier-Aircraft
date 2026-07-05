package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Comparator;
import java.util.List;

/**
 * 客户端 → 服务端：通过坐标添加火控打击目标。
 */
public record AddTargetPayload(double x, double y, double z) implements CustomPacketPayload {

    public static final Type<AddTargetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    IndomitableCarrierAircraft.MOD_ID, "add_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddTargetPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.x);
                        buf.writeDouble(pkt.y);
                        buf.writeDouble(pkt.z);
                    },
                    buf -> new AddTargetPayload(buf.readDouble(), buf.readDouble(), buf.readDouble())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AddTargetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            Vec3 pos = new Vec3(payload.x, payload.y, payload.z);

            // 检查方块目标附近 1 格内是否有实体
            FireControlTarget target = findNearbyEntityOrPosition(level, player, pos);

            FireControlSystem.getInstance().addTarget(player, target);
            if (target.isEntityTarget()) {
                player.sendSystemMessage(Component.literal(
                        String.format("已锁定实体目标 (%.0f, %.0f, %.0f)", payload.x, payload.y, payload.z))
                        .withStyle(ChatFormatting.GOLD));
            } else {
                player.sendSystemMessage(Component.literal(
                        String.format("已添加打击目标 (%.0f, %.0f, %.0f)", payload.x, payload.y, payload.z))
                        .withStyle(ChatFormatting.GREEN));
            }
        });
    }

    /**
     * 在方块坐标附近 1 格范围内查找实体。
     * 如果找到，返回实体目标；否则返回坐标目标。
     */
    private static FireControlTarget findNearbyEntityOrPosition(ServerLevel level, ServerPlayer player, Vec3 pos) {
        AABB searchBox = new AABB(pos.x - 1, pos.y - 1, pos.z - 1, pos.x + 1, pos.y + 1, pos.z + 1);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity.isAlive() && entity != player && !(entity instanceof Player));

        if (!entities.isEmpty()) {
            // 选择最近的实体
            LivingEntity nearest = entities.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(pos)))
                    .orElse(null);
            if (nearest != null) {
                return FireControlTarget.entity(nearest);
            }
        }
        return FireControlTarget.position(level, pos);
    }
}
