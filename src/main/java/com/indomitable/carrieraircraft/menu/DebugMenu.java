package com.indomitable.carrieraircraft.menu;

import com.indomitable.carrieraircraft.aircraft.AircraftRole;
import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.registry.ModItems;
import com.indomitable.carrieraircraft.registry.ModMenuTypes;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 调试工具 GUI 菜单。
 *
 * <p>服务端构造写入飞机列表，客户端构造从 buffer 读取。
 * 不使用 DataSlot，所有交互通过 DebugCommandPayload 网络包完成。
 */
public class DebugMenu extends AbstractContainerMenu {
    private static final int MAX_AIRCRAFT = 1024;

    public record AircraftInfo(UUID uuid, AircraftRole role, AircraftState state, int index) {}

    private final List<AircraftInfo> aircraftList = new ArrayList<>();

    // ── 服务端打开 ──

    public static void open(ServerPlayer player) {
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inv, p) -> new DebugMenu(id, inv),
                        Component.translatable("container.indomitablecarrieraircraft.debug")
                ),
                buf -> {
                    var aircraft = FormationManager.getInstance()
                            .getAircraft(player.serverLevel(), player.getUUID());
                    buf.writeVarInt(aircraft.size());
                    for (int i = 0; i < aircraft.size(); i++) {
                        var a = aircraft.get(i);
                        buf.writeUUID(a.getUUID());
                        buf.writeUtf(a.getRole().id());
                        buf.writeUtf(a.getState().name());
                        buf.writeVarInt(i);
                    }
                }
        );
    }

    // ── 服务端构造 ──

    public DebugMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEBUG.get(), containerId);
    }

    // ── 客户端构造 ──

    public DebugMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.DEBUG.get(), containerId);

        int count = buf.readVarInt();
        if (count < 0 || count > MAX_AIRCRAFT) {
            throw new DecoderException("Invalid debug aircraft count: " + count);
        }
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            AircraftRole role = AircraftRole.byId(buf.readUtf());
            AircraftState state;
            try {
                state = AircraftState.valueOf(buf.readUtf());
            } catch (IllegalArgumentException e) {
                state = AircraftState.STANDBY;
            }
            int index = buf.readVarInt();
            aircraftList.add(new AircraftInfo(uuid, role, state, index));
        }
    }

    // ── 数据访问 ──

    public List<AircraftInfo> aircraftList() { return aircraftList; }

    // ── 必需实现 ──

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive()
                && player.getAbilities().instabuild
                && (player.getMainHandItem().is(ModItems.DEBUG_TOOL.get())
                || player.getOffhandItem().is(ModItems.DEBUG_TOOL.get()));
    }
}
