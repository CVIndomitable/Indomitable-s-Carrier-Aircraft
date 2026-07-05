package com.indomitable.carrieraircraft.item;

import com.indomitable.carrieraircraft.menu.DebugMenu;
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
 * 调试工具物品。
 *
 * <p>仅创造模式可用。右键打开飞机状态调试面板。
 */
public class DebugToolItem extends Item {

    public DebugToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.getAbilities().instabuild) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("仅创造模式可用")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            DebugMenu.open((ServerPlayer) player);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.debug_tool.line1"));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.debug_tool.line2"));
    }
}
