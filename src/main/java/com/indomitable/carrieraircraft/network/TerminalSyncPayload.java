package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.aircraft.AircraftRole;
import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务端 → 客户端：控制终端实时同步包。
 *
 * <p>每隔一段时间发送飞机位置、状态、火控目标位置的最新数据，
 * 确保小地图上的标记实时更新。
 */
public record TerminalSyncPayload(
        List<AircraftData> aircraft,
        List<TargetData> targets,
        Vec3 playerPos,
        int forcedChunkCount
) implements CustomPacketPayload {

    public record AircraftData(UUID uuid, AircraftRole role, AircraftState state,
                               int seaAmmo, int airAmmo, double x, double z) {}

    public record TargetData(double x, double z, boolean isEntity) {}

    public static final Type<TerminalSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    IndomitableCarrierAircraft.MOD_ID, "terminal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.playerPos.x);
                        buf.writeDouble(pkt.playerPos.z);
                        buf.writeVarInt(pkt.aircraft.size());
                        for (var a : pkt.aircraft) {
                            buf.writeUUID(a.uuid);
                            buf.writeUtf(a.role.id());
                            buf.writeUtf(a.state.name());
                            buf.writeVarInt(a.seaAmmo);
                            buf.writeVarInt(a.airAmmo);
                            buf.writeDouble(a.x);
                            buf.writeDouble(a.z);
                        }
                        buf.writeVarInt(pkt.targets.size());
                        for (var t : pkt.targets) {
                            buf.writeDouble(t.x);
                            buf.writeDouble(t.z);
                            buf.writeBoolean(t.isEntity);
                        }
                        buf.writeVarInt(pkt.forcedChunkCount);
                    },
                    buf -> {
                        double px = buf.readDouble();
                        double pz = buf.readDouble();
                        List<AircraftData> aircraft = new ArrayList<>();
                        int acCount = buf.readVarInt();
                        for (int i = 0; i < acCount; i++) {
                            UUID uuid = buf.readUUID();
                            AircraftRole role = AircraftRole.byId(buf.readUtf());
                            AircraftState state;
                            try { state = AircraftState.valueOf(buf.readUtf()); }
                            catch (IllegalArgumentException e) { state = AircraftState.STANDBY; }
                            int seaAmmo = buf.readVarInt();
                            int airAmmo = buf.readVarInt();
                            double ax = buf.readDouble();
                            double az = buf.readDouble();
                            aircraft.add(new AircraftData(uuid, role, state, seaAmmo, airAmmo, ax, az));
                        }
                        List<TargetData> targets = new ArrayList<>();
                        int tCount = buf.readVarInt();
                        for (int i = 0; i < tCount; i++) {
                            double tx = buf.readDouble();
                            double tz = buf.readDouble();
                            boolean isEntity = buf.readBoolean();
                            targets.add(new TargetData(tx, tz, isEntity));
                        }
                        int forcedChunkCount = buf.readVarInt();
                        return new TerminalSyncPayload(aircraft, targets, new Vec3(px, 0, pz), forcedChunkCount);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端调用：收集当前数据并发送给指定玩家。
     */
    public static void sendTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        FormationManager fm = FormationManager.getInstance();
        FireControlSystem fcs = FireControlSystem.getInstance();

        Vec3 pp = player.position();

        var aircraftEntities = fm.getAircraft(level, player.getUUID());
        List<AircraftData> aircraft = new ArrayList<>(aircraftEntities.size());
        for (var a : aircraftEntities) {
            aircraft.add(new AircraftData(
                    a.getUUID(), a.getRole(), a.getState(),
                    a.getAmmoCount(), a.getAirAmmoCount(),
                    a.getX(), a.getZ()
            ));
        }

        var fireTargets = fcs.getTargets(player.getUUID());
        List<TargetData> targets = new ArrayList<>(fireTargets.size());
        for (var t : fireTargets) {
            Vec3 pos = t.currentPosition(level);
            targets.add(new TargetData(pos.x, pos.z, t.isEntityTarget()));
        }

        int forcedChunkCount = fm.getForcedChunkCount(player.getUUID());
        PacketDistributor.sendToPlayer(player, new TerminalSyncPayload(aircraft, targets, pp, forcedChunkCount));
    }

    /**
     * 客户端处理：更新菜单中的飞机和目标数据。
     */
    public static void handle(TerminalSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ControlTerminalMenu menu) {
                menu.syncFromServer(payload);
            }
        });
    }
}
