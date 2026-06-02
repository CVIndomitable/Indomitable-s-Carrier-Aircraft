package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页注册器
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IndomitableCarrierAircraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CARRIER_AIRCRAFT_TAB =
            CREATIVE_TABS.register("carrier_aircraft", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.indomitablecarrieraircraft.carrier_aircraft"))
                    .icon(() -> new ItemStack(ModItems.B25_SPAWNER.get()))
                    .displayItems((parameters, output) -> {
                        // Phase 1: B-25轰炸机召唤物品
                        output.accept(ModItems.B25_SPAWNER.get());
                    })
                    .build()
            );
}
