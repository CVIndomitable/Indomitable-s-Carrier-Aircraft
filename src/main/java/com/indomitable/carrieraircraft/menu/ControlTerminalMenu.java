package com.indomitable.carrieraircraft.menu;

import com.indomitable.carrieraircraft.aircraft.AircraftRole;
import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.PlayerAirControlSettings;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.network.TerminalSyncPayload;
import com.indomitable.carrieraircraft.registry.ModMenuTypes;
import com.indomitable.carrieraircraft.registry.ModItems;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 控制终端 GUI 菜单。
 *
 * <p>服务端构造写入初始数据（飞机列表 + 盘旋点），ContainerData 自动同步 5 项设置。
 * 客户端构造从 buffer 读取飞机列表和盘旋点，ContainerData 由框架自动填充。
 */
public class ControlTerminalMenu extends AbstractContainerMenu {
    private static final int MAX_AIRCRAFT = 1024;
    private static final int MAX_GROUPS = 256;

    // ── ContainerData 索引 ──
    private static final int IDX_AUTO_LOCK = 0;
    private static final int IDX_ASSIGNMENT = 1;
    private static final int IDX_AIR_DEFENSE = 2;
    private static final int IDX_BOMBS = 3;
    private static final int IDX_MIN_DMG = 4;
    private static final int DATA_SIZE = 5;

    // ── 客户端数据 ──
    public record AircraftInfo(UUID uuid, AircraftRole role, AircraftState state,
                               int seaAmmo, int airAmmo, Vec3 position) {}

    /** 火控目标（客户端只关心坐标，不区分实体/坐标目标）。 */
    public record TargetInfo(Vec3 position, boolean isEntity) {}

    /** 服务端同步间隔（tick）。 */
    private static final int SYNC_INTERVAL = 10; // 每 0.5 秒同步一次

    private final SimpleContainerData syncedData;
    private final List<AircraftInfo> aircraftList = new ArrayList<>();
    private final List<TargetInfo> targetList = new ArrayList<>();
    @Nullable
    private Vec3 rallyPoint;
    private Vec3 playerPosition = Vec3.ZERO;

    // 服务端用：tick 计数器与玩家引用
    private int tickCounter = 0;
    @Nullable
    private ServerPlayer serverPlayer;

    // 编组数据（客户端用）
    @Nullable
    private UUID leaderUUID;
    private final java.util.Map<UUID, String> aircraftGroupMap = new java.util.HashMap<>();
    private final List<String> groupNames = new ArrayList<>();
    private int forcedChunkCount = 0;

    // ── 服务端构造 ──

    /** 在服务端打开控制终端 GUI。 */
    public static void open(ServerPlayer player) {
        PlayerAirControlSettings settings = FireControlSystem.getInstance().settings(player.serverLevel(), player.getUUID());
        FormationManager fm = FormationManager.getInstance();
        FireControlSystem fcs = FireControlSystem.getInstance();

        player.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new ControlTerminalMenu(id, inv, settings),
                        Component.translatable("container.indomitablecarrieraircraft.control_terminal")
                ),
                buf -> {
                    // 玩家位置
                    Vec3 pp = player.position();
                    buf.writeDouble(pp.x);
                    buf.writeDouble(pp.z);

                    // 飞机列表（含位置）
                    var aircraft = fm.getAircraft(player.serverLevel(), player.getUUID());
                    buf.writeVarInt(aircraft.size());
                    for (var a : aircraft) {
                        buf.writeUUID(a.getUUID());
                        buf.writeUtf(a.getRole().id());
                        buf.writeUtf(a.getState().name());
                        buf.writeVarInt(a.getAmmoCount());
                        buf.writeVarInt(a.getAirAmmoCount());
                        buf.writeDouble(a.getX());
                        buf.writeDouble(a.getZ());
                    }
                    // 盘旋点
                    Vec3 rally = fm.getRallyPoint(player.serverLevel(), player.getUUID());
                    buf.writeBoolean(rally != null);
                    if (rally != null) {
                        buf.writeDouble(rally.x);
                        buf.writeDouble(rally.y);
                        buf.writeDouble(rally.z);
                    }
                    // 火控目标
                    var targets = fcs.getTargets(player.getUUID());
                    buf.writeVarInt(targets.size());
                    for (var t : targets) {
                        Vec3 pos = t.currentPosition(player.serverLevel());
                        buf.writeDouble(pos.x);
                        buf.writeDouble(pos.z);
                        buf.writeBoolean(t.isEntityTarget());
                    }
                    // 编组数据
                    UUID leaderId = fm.getLeaderUUID(player.getUUID());
                    buf.writeBoolean(leaderId != null);
                    if (leaderId != null) {
                        buf.writeUUID(leaderId);
                    }
                    buf.writeVarInt(aircraft.size());
                    for (var a : aircraft) {
                        String group = fm.getGroup(player.getUUID(), a.getUUID());
                        buf.writeUtf(group != null ? group : "");
                    }
                    var names = fm.getGroupNames(player.getUUID());
                    buf.writeVarInt(names.size());
                    for (String name : names) {
                        buf.writeUtf(name);
                    }
                    buf.writeVarInt(fm.getForcedChunkCount(player.getUUID()));
                }
        );
    }

    /** 服务端构造。 */
    public ControlTerminalMenu(int containerId, Inventory playerInventory, PlayerAirControlSettings settings) {
        super(ModMenuTypes.CONTROL_TERMINAL.get(), containerId);
        if (playerInventory.player instanceof ServerPlayer sp) {
            this.serverPlayer = sp;
        }
        this.syncedData = new SimpleContainerData(DATA_SIZE);
        syncedData.set(IDX_AUTO_LOCK, settings.autoLockMode().ordinal());
        syncedData.set(IDX_ASSIGNMENT, settings.assignmentMode().ordinal());
        syncedData.set(IDX_AIR_DEFENSE, settings.airDefenseMode().ordinal());
        syncedData.set(IDX_BOMBS, settings.bombsPerPass());
        syncedData.set(IDX_MIN_DMG, encodeMinEffDamage(settings.minimumEffectiveDamage()));
        addDataSlots(syncedData);
    }

    /** 客户端构造（IContainerFactory 回调）。 */
    public ControlTerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.CONTROL_TERMINAL.get(), containerId);
        this.syncedData = new SimpleContainerData(DATA_SIZE);
        addDataSlots(syncedData);

        // 玩家位置
        playerPosition = new Vec3(buf.readDouble(), 0, buf.readDouble());

        // 读取飞机列表（含位置）
        int count = readCount(buf, MAX_AIRCRAFT, "aircraft");
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            AircraftRole role = AircraftRole.byId(buf.readUtf());
            AircraftState state;
            try {
                state = AircraftState.valueOf(buf.readUtf());
            } catch (IllegalArgumentException e) {
                state = AircraftState.STANDBY;
            }
            int seaAmmo = buf.readVarInt();
            int airAmmo = buf.readVarInt();
            double px = buf.readDouble();
            double pz = buf.readDouble();
            aircraftList.add(new AircraftInfo(uuid, role, state, seaAmmo, airAmmo, new Vec3(px, 0, pz)));
        }

        // 读取盘旋点
        if (buf.readBoolean()) {
            rallyPoint = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        // 读取火控目标
        int targetCount = readCount(buf, FireControlSystem.MAX_TARGETS, "targets");
        for (int i = 0; i < targetCount; i++) {
            double tx = buf.readDouble();
            double tz = buf.readDouble();
            boolean isEntity = buf.readBoolean();
            targetList.add(new TargetInfo(new Vec3(tx, 0, tz), isEntity));
        }

        // 读取编组数据
        if (buf.readBoolean()) {
            leaderUUID = buf.readUUID();
        }
        int groupCount = readCount(buf, MAX_AIRCRAFT, "aircraft groups");
        for (int i = 0; i < groupCount; i++) {
            String group = buf.readUtf();
            if (!group.isEmpty() && i < aircraftList.size()) {
                aircraftGroupMap.put(aircraftList.get(i).uuid(), group);
            }
        }
        int nameCount = readCount(buf, MAX_GROUPS, "group names");
        for (int i = 0; i < nameCount; i++) {
            groupNames.add(buf.readUtf());
        }
        forcedChunkCount = buf.readVarInt();
    }

    private static int readCount(RegistryFriendlyByteBuf buf, int max, String field) {
        int count = buf.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException("Invalid terminal " + field + " count: " + count);
        }
        return count;
    }

    // ── 同步数据读取（客户端 GUI 用） ──

    /** 从服务端设置同步到 ContainerData，客户端 GUI 会自动刷新。 */
    public void syncFromSettings(PlayerAirControlSettings settings) {
        syncedData.set(IDX_AUTO_LOCK, settings.autoLockMode().ordinal());
        syncedData.set(IDX_ASSIGNMENT, settings.assignmentMode().ordinal());
        syncedData.set(IDX_AIR_DEFENSE, settings.airDefenseMode().ordinal());
        syncedData.set(IDX_BOMBS, settings.bombsPerPass());
        syncedData.set(IDX_MIN_DMG, encodeMinEffDamage(settings.minimumEffectiveDamage()));
    }

    public int autoLockOrdinal()   { return syncedData.get(IDX_AUTO_LOCK); }
    public int assignmentOrdinal() { return syncedData.get(IDX_ASSIGNMENT); }
    public int airDefenseOrdinal() { return syncedData.get(IDX_AIR_DEFENSE); }
    public int bombsPerPass()      { return syncedData.get(IDX_BOMBS); }
    public float minEffDamage()    { return decodeMinEffDamage(syncedData.get(IDX_MIN_DMG)); }

    /** 最小有效伤害：索引 ↔ 浮点 映射。 */
    private static int encodeMinEffDamage(float v) {
        if (v <= 0)  return 0;
        if (v <= 20) return 1;
        if (v <= 40) return 2;
        return 3;
    }

    private static float decodeMinEffDamage(int i) {
        return switch (i) {
            case 1 -> 20.0F;
            case 2 -> 40.0F;
            case 3 -> 80.0F;
            default -> 0.0F;
        };
    }

    // ── 客户端数据访问 ──

    public List<AircraftInfo> aircraftList() { return aircraftList; }
    public List<TargetInfo> targetList() { return targetList; }
    public Vec3 playerPosition() { return playerPosition; }

    // ── 实时同步 ──

    /**
     * 服务端每 tick 调用：定期向客户端发送最新数据。
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer != null && ++tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            TerminalSyncPayload.sendTo(serverPlayer);
        }
    }

    /**
     * 客户端调用：用服务端最新数据替换本地缓存。
     */
    public void syncFromServer(TerminalSyncPayload payload) {
        this.playerPosition = payload.playerPos();
        this.rallyPoint = payload.rallyPoint();
        this.aircraftList.clear();
        this.aircraftGroupMap.clear();
        for (var a : payload.aircraft()) {
            this.aircraftList.add(new AircraftInfo(
                    a.uuid(), a.role(), a.state(),
                    a.seaAmmo(), a.airAmmo(),
                    new Vec3(a.x(), 0, a.z())
            ));
            if (a.group() != null) {
                this.aircraftGroupMap.put(a.uuid(), a.group());
            }
        }
        this.targetList.clear();
        for (var t : payload.targets()) {
            this.targetList.add(new TargetInfo(new Vec3(t.x(), 0, t.z()), t.isEntity()));
        }
        this.forcedChunkCount = payload.forcedChunkCount();
        this.leaderUUID = payload.leaderUUID();
        this.groupNames.clear();
        this.groupNames.addAll(payload.groupNames());
    }

    @Nullable
    public UUID leaderUUID() { return leaderUUID; }
    @Nullable
    public String aircraftGroup(UUID aircraftUUID) { return aircraftGroupMap.get(aircraftUUID); }
    public List<String> groupNames() { return List.copyOf(groupNames); }
    public int forcedChunkCount() { return forcedChunkCount; }

    @Nullable
    public Vec3 rallyPoint() { return rallyPoint; }

    // ── AbstractContainerMenu 必需实现 ──

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive()
                && (player.getMainHandItem().is(ModItems.CONTROL_TERMINAL.get())
                || player.getOffhandItem().is(ModItems.CONTROL_TERMINAL.get()));
    }
}
