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
    private final int nominalDamage;
    private final int rounds;

    public AviationAmmoItem(Properties properties, AmmoType ammoType, int nominalDamage, int rounds) {
        super(properties);
        this.ammoType = ammoType;
        this.nominalDamage = nominalDamage;
        this.rounds = rounds;
    }

    public AmmoType ammoType() {
        return ammoType;
    }

    public int nominalDamage() {
        return nominalDamage;
    }

    public int rounds() {
        return rounds;
    }

    public int rounds(ItemStack stack) {
        if (ammoType != AmmoType.MAGAZINE) {
            return rounds;
        }
        Integer stored = stack.get(ModDataComponents.MAGAZINE_ROUNDS);
        return stored == null ? rounds : Math.max(0, Math.min(rounds, stored));
    }

    public void setMagazineRounds(ItemStack stack, int value) {
        if (ammoType != AmmoType.MAGAZINE) {
            return;
        }
        int clamped = Math.max(0, Math.min(rounds, value));
        if (clamped >= rounds) {
            stack.remove(ModDataComponents.MAGAZINE_ROUNDS);
        } else {
            stack.set(ModDataComponents.MAGAZINE_ROUNDS, clamped);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.ammo_type", ammoType.name()));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.nominal_damage", nominalDamage));
        if (rounds > 1) {
            tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.rounds", rounds(stack)));
        }
    }
}
