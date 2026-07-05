package com.indomitable.carrieraircraft.network;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.aircraft.AirDefenseMode;
import com.indomitable.carrieraircraft.aircraft.AssignmentMode;
import com.indomitable.carrieraircraft.aircraft.AutoLockMode;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
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

/**
 * 客户端 → 服务端：控制终端 GUI 命令包。
 *
 * <p>所有命令通过一个 action 字节分发，避免注册多个 payload 类型。
 */
public record CommandPayload(byte action) implements CustomPacketPayload {

    // ── action 常量：循环 ──
    public static final byte CYCLE_AUTO_LOCK = 0;
    public static final byte CYCLE_ASSIGNMENT = 1;
    public static final byte CYCLE_AIR_DEFENSE = 2;
    public static final byte CYCLE_BOMBS_PER_PASS = 3;
    public static final byte CYCLE_MIN_EFF_DAMAGE = 4;
    public static final byte SET_RALLY_POINT = 5;
    public static final byte RECALL_ALL = 6;

    // ── action 常量：直接设置（下拉菜单用）──
    public static final byte SET_AUTO_LOCK_NEAREST = 7;
    public static final byte SET_AUTO_LOCK_STRONGEST = 8;
    public static final byte SET_AUTO_LOCK_FOCUS = 9;
    public static final byte SET_AUTO_LOCK_SPREAD = 10;
    public static final byte SET_AUTO_LOCK_TYPE_FILTER = 11;
    public static final byte SET_ASSIGN_FOCUS = 12;
    public static final byte SET_ASSIGN_SPREAD = 13;
    public static final byte SET_AIR_DEFENSE_SELF = 14;
    public static final byte SET_AIR_DEFENSE_ACTIVE = 15;
    public static final byte SET_AIR_DEFENSE_LOW = 16;
    public static final byte SET_BOMBS_1 = 17;
    public static final byte SET_BOMBS_2 = 18;
    public static final byte SET_BOMBS_3 = 19;
    public static final byte SET_BOMBS_4 = 20;
    public static final byte SET_MIN_DMG_0 = 21;
    public static final byte SET_MIN_DMG_20 = 22;
    public static final byte SET_MIN_DMG_40 = 23;
    public static final byte SET_MIN_DMG_80 = 24;
    public static final byte REARM_ALL = 25;
    public static final byte ENTER_LEADER_CAMERA = 26;
    public static final byte EXIT_LEADER_CAMERA = 27;

    public static final Type<CommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndomitableCarrierAircraft.MOD_ID, "terminal_cmd"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeByte(pkt.action),
                    buf -> new CommandPayload(buf.readByte())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── 服务端处理 ──

    public static void handle(CommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            executeAction(player, payload.action);
        });
    }

    private static void executeAction(ServerPlayer player, byte action) {
        FireControlSystem fireControl = FireControlSystem.getInstance();
        PlayerAirControlSettings settings = fireControl.settings(player.serverLevel(), player.getUUID());

        switch (action) {
            case CYCLE_AUTO_LOCK -> {
                AutoLockMode mode = settings.cycleAutoLockMode();
                player.sendSystemMessage(Component.literal("自动锁定: " + mode.displayName()).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_ASSIGNMENT -> {
                AssignmentMode mode = settings.cycleAssignmentMode();
                player.sendSystemMessage(Component.literal("目标分配: " + mode.displayName()).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_AIR_DEFENSE -> {
                AirDefenseMode mode = settings.cycleAirDefenseMode();
                player.sendSystemMessage(Component.literal("对空模式: " + mode.displayName()).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_BOMBS_PER_PASS -> {
                int val = settings.cycleBombsPerPass();
                player.sendSystemMessage(Component.literal("投弹数量: " + val).withStyle(ChatFormatting.GREEN));
            }
            case CYCLE_MIN_EFF_DAMAGE -> {
                float val = settings.cycleMinimumEffectiveDamage();
                player.sendSystemMessage(Component.literal("最小有效伤害: " + (int) val).withStyle(ChatFormatting.GREEN));
            }
            case SET_RALLY_POINT -> {
                ServerLevel level = player.serverLevel();
                Vec3 point = player.pick(240.0, 0, false).getLocation().add(0, 18.0, 0);
                int count = FormationManager.getInstance().deployToRallyPoint(level, player.getUUID(), point);
                player.sendSystemMessage(Component.literal(
                        String.format("盘旋点已设置 (%.0f, %.0f, %.0f)，%d 架飞机转入 ORBITING",
                                point.x, point.y, point.z, count)
                ).withStyle(ChatFormatting.AQUA));
            }
            case RECALL_ALL -> {
                int count = FormationManager.getInstance().recall(player.serverLevel(), player.getUUID());
                player.sendSystemMessage(Component.literal("已召回 " + count + " 架飞机").withStyle(ChatFormatting.YELLOW));
            }
            case REARM_ALL -> {
                int count = FormationManager.getInstance().rearmAll(player.serverLevel(), player);
                player.sendSystemMessage(Component.literal("已补给 " + count + " 架飞机").withStyle(ChatFormatting.AQUA));
            }
            case ENTER_LEADER_CAMERA -> enterLeaderCamera(player);
            case EXIT_LEADER_CAMERA -> {
                player.setCamera(player);
                player.sendSystemMessage(Component.literal("已切回玩家视角").withStyle(ChatFormatting.YELLOW));
            }
            // ── 直接设置（下拉菜单）──
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
        }

        // 同步设置到 ContainerData，客户端 GUI 实时刷新
        fireControl.saveSettings(player.serverLevel(), player.getUUID());
        syncMenu(player, settings);
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
        player.setCamera(leader);
        player.sendSystemMessage(Component.literal("已切入长机视角，使用终端切回玩家视角").withStyle(ChatFormatting.AQUA));
    }
}
