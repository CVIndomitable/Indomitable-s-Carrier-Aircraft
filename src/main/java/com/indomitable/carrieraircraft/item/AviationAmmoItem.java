package com.indomitable.carrieraircraft.item;

import com.indomitable.carrieraircraft.aircraft.AmmoType;
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.ammo_type", ammoType.name()));
        tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.nominal_damage", nominalDamage));
        if (rounds > 1) {
            tooltip.add(Component.translatable("tooltip.indomitablecarrieraircraft.rounds", rounds));
        }
    }
}
