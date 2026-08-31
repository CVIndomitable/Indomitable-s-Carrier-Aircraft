package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.aircraft.AircraftRole;
import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import io.netty.handler.codec.DecoderException;
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
        Vec3 rallyPoint,
        int forcedChunkCount,
        UUID leaderUUID,
        List<String> groupNames
) implements CustomPacketPayload {
    private static final int MAX_AIRCRAFT = 1024;
    private static final int MAX_TARGETS = FireControlSystem.MAX_TARGETS;
    private static final int MAX_GROUPS = 256;

    public record AircraftData(UUID uuid, AircraftRole role, AircraftState state,
                               int seaAmmo, int airAmmo, double x, double z,
                               String group) {}

    public record TargetData(double x, double z, boolean isEntity) {}

    public static final Type<TerminalSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    IndomitableCarrierAircraft.MOD_ID, "terminal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.playerPos.x);
                        buf.writeDouble(pkt.playerPos.z);
                        buf.writeBoolean(pkt.rallyPoint != null);
                        if (pkt.rallyPoint != null) {
                            buf.writeDouble(pkt.rallyPoint.x);
                            buf.writeDouble(pkt.rallyPoint.y);
                            buf.writeDouble(pkt.rallyPoint.z);
                        }
                        buf.writeVarInt(pkt.aircraft.size());
                        for (var a : pkt.aircraft) {
                            buf.writeUUID(a.uuid);
                            buf.writeUtf(a.role.id());
                            buf.writeUtf(a.state.name());
                            buf.writeVarInt(a.seaAmmo);
                            buf.writeVarInt(a.airAmmo);
                            buf.writeDouble(a.x);
                            buf.writeDouble(a.z);
                            buf.writeUtf(a.group != null ? a.group : "");
                        }
                        buf.writeVarInt(pkt.targets.size());
                        for (var t : pkt.targets) {
                            buf.writeDouble(t.x);
                            buf.writeDouble(t.z);
                            buf.writeBoolean(t.isEntity);
                        }
                        buf.writeVarInt(pkt.forcedChunkCount);
                        buf.writeBoolean(pkt.leaderUUID != null);
                        if (pkt.leaderUUID != null) {
                            buf.writeUUID(pkt.leaderUUID);
                        }
                        buf.writeVarInt(pkt.groupNames.size());
                        for (String name : pkt.groupNames) {
                            buf.writeUtf(name);
                        }
                    },
                    buf -> {
                        double px = buf.readDouble();
                        double pz = buf.readDouble();
                        Vec3 rallyPoint = buf.readBoolean()
                                ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
                        List<AircraftData> aircraft = new ArrayList<>();
                        int acCount = readCount(buf, MAX_AIRCRAFT, "aircraft");
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
                            String group = buf.readUtf();
                            aircraft.add(new AircraftData(uuid, role, state, seaAmmo, airAmmo, ax, az,
                                    group.isEmpty() ? null : group));
                        }
                        List<TargetData> targets = new ArrayList<>();
                        int tCount = readCount(buf, MAX_TARGETS, "targets");
                        for (int i = 0; i < tCount; i++) {
                            double tx = buf.readDouble();
                            double tz = buf.readDouble();
                            boolean isEntity = buf.readBoolean();
                            targets.add(new TargetData(tx, tz, isEntity));
                        }
                        int forcedChunkCount = buf.readVarInt();
                        UUID leaderUUID = buf.readBoolean() ? buf.readUUID() : null;
                        int gnCount = readCount(buf, MAX_GROUPS, "groups");
                        List<String> groupNames = new ArrayList<>(gnCount);
                        for (int i = 0; i < gnCount; i++) {
                            groupNames.add(buf.readUtf());
                        }
                        return new TerminalSyncPayload(aircraft, targets, new Vec3(px, 0, pz), rallyPoint,
                                forcedChunkCount, leaderUUID, groupNames);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static int readCount(RegistryFriendlyByteBuf buf, int max, String field) {
        int count = buf.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException("Invalid terminal " + field + " count: " + count);
        }
        return count;
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
            String group = fm.getGroup(player.getUUID(), a.getUUID());
            aircraft.add(new AircraftData(
                    a.getUUID(), a.getRole(), a.getState(),
                    a.getAmmoCount(), a.getAirAmmoCount(),
                    a.getX(), a.getZ(), group
            ));
        }

        var fireTargets = fcs.getTargets(player.getUUID());
        List<TargetData> targets = new ArrayList<>(fireTargets.size());
        for (var t : fireTargets) {
            Vec3 pos = t.currentPosition(level);
            targets.add(new TargetData(pos.x, pos.z, t.isEntityTarget()));
        }

        int forcedChunkCount = fm.getForcedChunkCount(player.getUUID());
        UUID leaderUUID = fm.getLeaderUUID(player.getUUID());
        List<String> groupNames = fm.getGroupNames(player.getUUID());
        Vec3 rallyPoint = fm.getRallyPoint(level, player.getUUID());

        PacketDistributor.sendToPlayer(player, new TerminalSyncPayload(
                aircraft, targets, pp, rallyPoint, forcedChunkCount, leaderUUID, groupNames));
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
