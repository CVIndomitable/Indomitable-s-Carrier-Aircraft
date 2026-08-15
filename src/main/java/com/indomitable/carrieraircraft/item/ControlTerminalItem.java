package com.indomitable.carrieraircraft.item;

import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import com.indomitable.carrieraircraft.formation.FormationManager;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * <p>左键锁定实体目标；右键打开 GUI 控制面板；Shift+右键锁定方块坐标；丢弃时召回所有飞机。
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
            // Shift + 右键（主手）：快速添加方块坐标目标
            quickLockBlockTarget(serverPlayer);
        } else {
            // 右键：打开 GUI
            ControlTerminalMenu.open(serverPlayer);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity target) {
        if (player instanceof ServerPlayer serverPlayer) {
            // 左键实体：锁定该实体目标
            if (com.indomitable.carrieraircraft.targeting.FriendlyFireFilter.canPlayerTarget(player.getUUID(), target)) {
                FireControlTarget fcTarget = FireControlTarget.entity(target);
                FireControlSystem.getInstance().addTarget(serverPlayer, fcTarget);
                var pos = fcTarget.currentPosition(serverPlayer.serverLevel());
                player.sendSystemMessage(Component.literal(String.format(
                        "已锁定实体目标: %.1f / %.1f / %.1f", pos.x, pos.y, pos.z
                )).withStyle(ChatFormatting.GOLD));
            } else {
                player.sendSystemMessage(Component.literal("无法锁定该实体").withStyle(ChatFormatting.RED));
            }
            return true; // 阻止默认攻击
        }
        return false;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            int count = FormationManager.getInstance().recall(serverPlayer.serverLevel(), serverPlayer.getUUID());
            player.sendSystemMessage(Component.literal("已召回 " + count + " 架飞机").withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    /**
     * 快速锁定方块坐标目标。
     */
    private void quickLockBlockTarget(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        net.minecraft.world.phys.HitResult blockHit = player.pick(MAX_TARGET_DISTANCE, 0, false);
        if (blockHit == null || blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            player.sendSystemMessage(Component.literal("未找到可锁定方块").withStyle(ChatFormatting.RED));
            return;
        }

        FireControlTarget target = FireControlTarget.position(level, blockHit.getLocation());
        FireControlSystem.getInstance().addTarget(player, target);
        var pos = blockHit.getLocation();
        player.sendSystemMessage(Component.literal(String.format(
                "已锁定方块坐标: %.1f / %.1f / %.1f", pos.x, pos.y, pos.z
        )).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.control_terminal.line1"));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.control_terminal.line2"));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.control_terminal.line3"));
    }
}
