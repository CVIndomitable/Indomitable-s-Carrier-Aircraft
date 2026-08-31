package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.aircraft.AirDefenseMode;
import com.indomitable.carrieraircraft.aircraft.AssignmentMode;
import com.indomitable.carrieraircraft.aircraft.AutoLockMode;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.firecontrol.CameraStateTracker;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.PlayerAirControlSettings;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

/**
 * 客户端 → 服务端：控制终端 GUI 命令包。
 *
 * <p>所有命令通过一个 {@link Action} 枚举分发，按「是否修改设置」拆成两组：
 * <ul>
 *   <li>{@link Kind#SETTING}：会改 {@link PlayerAirControlSettings}，需 save + syncMenu；</li>
 *   <li>{@link Kind#ACTION}：纯动作命令（召回/补给/视角切换等），不需要保存。</li>
 * </ul>
 * 这样取代原来的 byte-magic + 巨型 switch 写法，所有 action 编译期检查。
 */
public record CommandPayload(Action action) implements CustomPacketPayload {

    /**
     * 控制终端支持的命令。
     *
     * <p>字段命名保持与早期 byte 常量语义一致；ordinal 不再作为 wire 格式使用，
     * 改为通过 {@link StreamCodec} 显式编码枚举名，方便日后增删顺序调整。
     */
    public enum Action {
        // ── 设置：cycle ──
        CYCLE_AUTO_LOCK(Kind.SETTING),
        CYCLE_ASSIGNMENT(Kind.SETTING),
        CYCLE_AIR_DEFENSE(Kind.SETTING),
        CYCLE_BOMBS_PER_PASS(Kind.SETTING),
        CYCLE_MIN_EFF_DAMAGE(Kind.SETTING),
        // ── 设置：直接设置 ──
        SET_AUTO_LOCK_NEAREST(Kind.SETTING),
        SET_AUTO_LOCK_STRONGEST(Kind.SETTING),
        SET_AUTO_LOCK_FOCUS(Kind.SETTING),
        SET_AUTO_LOCK_SPREAD(Kind.SETTING),
        SET_AUTO_LOCK_TYPE_FILTER(Kind.SETTING),
        SET_ASSIGN_FOCUS(Kind.SETTING),
        SET_ASSIGN_SPREAD(Kind.SETTING),
        SET_AIR_DEFENSE_SELF(Kind.SETTING),
        SET_AIR_DEFENSE_ACTIVE(Kind.SETTING),
        SET_AIR_DEFENSE_LOW(Kind.SETTING),
        SET_BOMBS_1(Kind.SETTING),
        SET_BOMBS_2(Kind.SETTING),
        SET_BOMBS_3(Kind.SETTING),
        SET_BOMBS_4(Kind.SETTING),
        SET_MIN_DMG_0(Kind.SETTING),
        SET_MIN_DMG_20(Kind.SETTING),
        SET_MIN_DMG_40(Kind.SETTING),
        SET_MIN_DMG_80(Kind.SETTING),
        // ── 动作 ──
        SET_RALLY_POINT(Kind.ACTION),
        RECALL_ALL(Kind.ACTION),
        REARM_ALL(Kind.ACTION),
        ENTER_LEADER_CAMERA(Kind.ACTION),
        EXIT_LEADER_CAMERA(Kind.ACTION);

        public enum Kind { SETTING, ACTION }

        private final Kind kind;

        Action(Kind kind) { this.kind = kind; }

        public Kind kind() { return kind; }
    }

    public static final Type<CommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndomitableCarrierAircraft.MOD_ID, "terminal_cmd"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeUtf(pkt.action.name()),
                    buf -> new CommandPayload(Action.valueOf(buf.readUtf()))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── 服务端处理 ──

    public static void handle(CommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!hasOpenTerminal(player)) {
                return;
            }
            executeAction(player, payload.action);
        });
    }

    private static boolean hasOpenTerminal(ServerPlayer player) {
        return player.containerMenu instanceof ControlTerminalMenu
                && player.containerMenu.stillValid(player);
    }

    /** 旧 byte 常量索引 → 新枚举，方便客户端 payload 构造处继续兼容。 */
    private static final Map<Byte, Action> LEGACY_BYTE_MAP = buildLegacyByteMap();

    private static Map<Byte, Action> buildLegacyByteMap() {
        // 旧 byte 序号（0-27）与新枚举的 1:1 映射。顺序与旧 CommandPayload 完全一致。
        Map<Byte, Action> m = new java.util.HashMap<>();
        Action[] values = Action.values();
        for (byte b = 0; b < values.length && b <= 27; b++) {
            m.put(b, values[b]);
        }
        return m;
    }

    /** 旧 byte 兼容：客户端若按老格式发 byte 过来，可走这条解码路径。 */
    public static CommandPayload fromLegacyByte(byte legacy) {
        Action mapped = LEGACY_BYTE_MAP.get(legacy);
        return new CommandPayload(mapped != null ? mapped : Action.RECALL_ALL);
    }

    private static void executeAction(ServerPlayer player, Action action) {
        FireControlSystem fireControl = FireControlSystem.getInstance();
        PlayerAirControlSettings settings = fireControl.settings(player.serverLevel(), player.getUUID());

        switch (action) {
            case CYCLE_AUTO_LOCK -> {
                AutoLockMode m = settings.cycleAutoLockMode();
                player.sendSystemMessage(Component.literal("自动锁定: " + m.displayName()).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_ASSIGNMENT -> {
                AssignmentMode m = settings.cycleAssignmentMode();
                player.sendSystemMessage(Component.literal("目标分配: " + m.displayName()).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_AIR_DEFENSE -> {
                AirDefenseMode m = settings.cycleAirDefenseMode();
                player.sendSystemMessage(Component.literal("对空模式: " + m.displayName()).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_BOMBS_PER_PASS -> {
                int v = settings.cycleBombsPerPass();
                player.sendSystemMessage(Component.literal("投弹数量: " + v).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_MIN_EFF_DAMAGE -> {
                float v = settings.cycleMinimumEffectiveDamage();
                player.sendSystemMessage(Component.literal("最小有效伤害: " + (int) v).withStyle(ChatFormatting.GREEN));
            }
            case SET_AUTO_LOCK_NEAREST -> setAutoLock(player, settings, AutoLockMode.NEAREST);
            case SET_AUTO_LOCK_STRONGEST -> setAutoLock(player, settings, AutoLockMode.STRONGEST);
            case SET_AUTO_LOCK_FOCUS -> setAutoLock(player, settings, AutoLockMode.FOCUS);
            case SET_AUTO_LOCK_SPREAD -> setAutoLock(player, settings, AutoLockMode.SPREAD);
            case SET_AUTO_LOCK_TYPE_FILTER -> setAutoLock(player, settings, AutoLockMode.TYPE_FILTER);
            case SET_ASSIGN_FOCUS -> setAssignment(player, settings, AssignmentMode.FOCUS);
            case SET_ASSIGN_SPREAD -> setAssignment(player, settings, AssignmentMode.BALANCED);
            case SET_AIR_DEFENSE_SELF -> setAirDefense(player, settings, AirDefenseMode.SELF_DEFENSE);
            case SET_AIR_DEFENSE_ACTIVE -> setAirDefense(player, settings, AirDefenseMode.ACTIVE);
            case SET_AIR_DEFENSE_LOW -> setAirDefense(player, settings, AirDefenseMode.LOW_AGGRESSION);
            case SET_BOMBS_1 -> { settings.setBombsPerPass(1); player.sendSystemMessage(Component.literal("投弹数量: 1").withStyle(ChatFormatting.GREEN)); }
            case SET_BOMBS_2 -> { settings.setBombsPerPass(2); player.sendSystemMessage(Component.literal("投弹数量: 2").withStyle(ChatFormatting.GREEN)); }
            case SET_BOMBS_3 -> { settings.setBombsPerPass(3); player.sendSystemMessage(Component.literal("投弹数量: 3").withStyle(ChatFormatting.GREEN)); }
            case SET_BOMBS_4 -> { settings.setBombsPerPass(4); player.sendSystemMessage(Component.literal("投弹数量: 4").withStyle(ChatFormatting.GREEN)); }
            case SET_MIN_DMG_0  -> { settings.setMinimumEffectiveDamage(0.0F);  player.sendSystemMessage(Component.literal("最小有效伤害: 0").withStyle(ChatFormatting.GREEN)); }
            case SET_MIN_DMG_20 -> { settings.setMinimumEffectiveDamage(20.0F); player.sendSystemMessage(Component.literal("最小有效伤害: 20").withStyle(ChatFormatting.GREEN)); }
            case SET_MIN_DMG_40 -> { settings.setMinimumEffectiveDamage(40.0F); player.sendSystemMessage(Component.literal("最小有效伤害: 40").withStyle(ChatFormatting.GREEN)); }
            case SET_MIN_DMG_80 -> { settings.setMinimumEffectiveDamage(80.0F); player.sendSystemMessage(Component.literal("最小有效伤害: 80").withStyle(ChatFormatting.GREEN)); }
            // ── 动作命令 ──
            case SET_RALLY_POINT -> {
                ServerLevel lvl = player.serverLevel();
                Vec3 point = player.pick(FireControlSystem.RALLY_POINT_LOOK_DISTANCE, 0, false).getLocation()
                        .add(0, FireControlSystem.RALLY_POINT_HEIGHT_OFFSET, 0);
                int n = FormationManager.getInstance().deployToRallyPoint(lvl, player.getUUID(), point);
                player.sendSystemMessage(Component.literal(String.format(
                                "盘旋点已设置 (%.0f, %.0f, %.0f)，%d 架飞机转入 ",
                                point.x, point.y, point.z, n))
                        .append(Component.translatable(AircraftState.ORBITING.translationKey()))
                        .withStyle(ChatFormatting.AQUA));
            }
            case RECALL_ALL -> {
                int n = FormationManager.getInstance().recall(player.serverLevel(), player.getUUID());
                player.sendSystemMessage(Component.literal("已召回 " + n + " 架飞机").withStyle(ChatFormatting.YELLOW));
            }
            case REARM_ALL -> {
                int n = FormationManager.getInstance().rearmAll(player.serverLevel(), player);
                player.sendSystemMessage(Component.literal("已补给 " + n + " 架飞机").withStyle(ChatFormatting.AQUA));
            }
            case ENTER_LEADER_CAMERA -> enterLeaderCamera(player);
            case EXIT_LEADER_CAMERA -> exitLeaderCamera(player);
        }

        // I5/M3：仅 SETTING 命令需要 save + syncMenu；动作命令不再做无意义的 settings 同步
        if (action.kind() == Action.Kind.SETTING) {
            fireControl.saveSettings(player.serverLevel(), player.getUUID());
            syncMenu(player, settings);
        }
    }

    private static void syncMenu(ServerPlayer player, PlayerAirControlSettings settings) {
        if (player.containerMenu instanceof ControlTerminalMenu menu) {
            menu.syncFromSettings(settings);
        }
    }

    private static void setAutoLock(ServerPlayer player, PlayerAirControlSettings settings, AutoLockMode mode) {
        settings.setAutoLockMode(mode);
        player.sendSystemMessage(Component.literal("自动锁定: " + mode.displayName()).withStyle(ChatFormatting.GREEN));
    }

    private static void setAssignment(ServerPlayer player, PlayerAirControlSettings settings, AssignmentMode mode) {
        settings.setAssignmentMode(mode);
        player.sendSystemMessage(Component.literal("目标分配: " + mode.displayName()).withStyle(ChatFormatting.GREEN));
    }

    private static void setAirDefense(ServerPlayer player, PlayerAirControlSettings settings, AirDefenseMode mode) {
        settings.setAirDefenseMode(mode);
        player.sendSystemMessage(Component.literal("对空模式: " + mode.displayName()).withStyle(ChatFormatting.GREEN));
    }

    private static void enterLeaderCamera(ServerPlayer player) {
        AircraftEntity leader = FormationManager.getInstance().getLeader(player.serverLevel(), player.getUUID());
        if (leader == null) {
            player.sendSystemMessage(Component.literal("当前没有可用长机").withStyle(ChatFormatting.RED));
            return;
        }
        CameraStateTracker.getInstance().enterLeaderCamera(player);
        player.setCamera(leader);
        player.sendSystemMessage(Component.literal("已切入长机视角，使用终端切回玩家视角").withStyle(ChatFormatting.AQUA));
    }

    private static void exitLeaderCamera(ServerPlayer player) {
        CameraStateTracker.getInstance().exitLeaderCamera(player);
        player.setCamera(player);
        player.sendSystemMessage(Component.literal("已切回玩家视角").withStyle(ChatFormatting.YELLOW));
    }
}
