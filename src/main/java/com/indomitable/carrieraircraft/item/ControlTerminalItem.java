package com.indomitable.carrieraircraft.item;

import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 控制终端物品。
 *
 * <p>右键打开 GUI 控制面板；丢弃时召回所有飞机。
 * <p>潜行 + 主手快速追加锁定目标（无需打开 GUI）。
 */
public class ControlTerminalItem extends Item {
    private static final double MAX_TARGET_DISTANCE = 240.0;

    public ControlTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            // 潜行 + 主手：快速追加锁定目标
            quickLockTarget(serverPlayer);
        } else {
            // 非潜行：打开 GUI
            ControlTerminalMenu.open(serverPlayer);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            int count = FormationManager.getInstance().recall(serverPlayer.serverLevel(), serverPlayer.getUUID());
            player.sendSystemMessage(Component.literal("已召回 " + count + " 架飞机").withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    private void quickLockTarget(ServerPlayer player) {
        FireControlTarget target = FireControlSystem.getInstance()
                .setTargetFromLookDirection(player, MAX_TARGET_DISTANCE, true);
        if (target == null) {
            player.sendSystemMessage(Component.literal("未找到可锁定目标").withStyle(ChatFormatting.RED));
            return;
        }

        var pos = target.currentPosition(player.serverLevel());
        player.sendSystemMessage(Component.literal(String.format(
                "火控目标 +1: %.1f / %.1f / %.1f", pos.x, pos.y, pos.z
        )).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.control_terminal.line1"));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.control_terminal.line2"));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.control_terminal.line3"));
    }
}
