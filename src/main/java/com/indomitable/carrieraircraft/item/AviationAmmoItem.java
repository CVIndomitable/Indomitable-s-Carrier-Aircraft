package com.indomitable.carrieraircraft.item;

import com.indomitable.carrieraircraft.aircraft.AmmoType;
import com.indomitable.carrieraircraft.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AviationAmmoItem extends Item {
    private final AmmoType ammoType;
    private final int nominalRounds;
    private final int nominalDamage;

    public AviationAmmoItem(Properties properties, AmmoType ammoType, int nominalDamage, int nominalRounds) {
        super(properties);
        this.ammoType = ammoType;
        this.nominalDamage = nominalDamage;
        this.nominalRounds = nominalRounds;
    }

    public AmmoType ammoType() {
        return ammoType;
    }

    public int nominalDamage() {
        return nominalDamage;
    }

    /**
     * 弹匣构造时定义的"标称"装填量（满弹匣 = nominalRounds）。
     * <p>注意：单次 {@link ItemStack} 的"当前剩余"使用 {@link #currentRounds(ItemStack)}。
     */
    public int nominalRounds() {
        return nominalRounds;
    }

    /**
     * @deprecated 命名歧义，调用者实际想用"当前剩余"。改用 {@link #currentRounds} 或 {@link #nominalRounds}。
     */
    @Deprecated
    public int rounds() {
        return nominalRounds;
    }

    /**
     * 当前弹匣剩余弹药数（非弹匣类弹药直接返回标称值）。
     * <p>这是补给弹药时使用的语义：玩家的"剩余弹药"是这个值。
     */
    public int currentRounds(ItemStack stack) {
        if (ammoType != AmmoType.MAGAZINE) {
            return nominalRounds;
        }
        Integer stored = stack.get(ModDataComponents.MAGAZINE_ROUNDS);
        return stored == null ? nominalRounds : Math.max(0, Math.min(nominalRounds, stored));
    }

    /**
     * 兼容旧调用：{@link #currentRounds(ItemStack)} 的别名。
     */
    public int rounds(ItemStack stack) {
        return currentRounds(stack);
    }

    public void setMagazineRounds(ItemStack stack, int value) {
        if (ammoType != AmmoType.MAGAZINE) {
            return;
        }
        int clamped = Math.max(0, Math.min(nominalRounds, value));
        if (clamped >= nominalRounds) {
            stack.remove(ModDataComponents.MAGAZINE_ROUNDS);
        } else {
            stack.set(ModDataComponents.MAGAZINE_ROUNDS, clamped);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.ammo_type",
                Component.translatable(ammoType.translationKey())));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.nominal_damage", nominalDamage));
        if (nominalRounds > 1) {
            tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.rounds", currentRounds(stack)));
        }
    }
}
