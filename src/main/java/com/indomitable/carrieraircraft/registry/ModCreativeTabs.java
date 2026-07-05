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
                        output.accept(ModItems.B25_SPAWNER.get());
                        output.accept(ModItems.BTD_SPAWNER.get());
                        output.accept(ModItems.BTD_TORPEDO_SPAWNER.get());
                        output.accept(ModItems.ROCKET_ATTACKER_SPAWNER.get());
                        output.accept(ModItems.ASW_PATROL_SPAWNER.get());
                        output.accept(ModItems.CONTROL_TERMINAL.get());
                        output.accept(ModItems.DEBUG_TOOL.get());
                        output.accept(ModItems.AIRCRAFT_MAGAZINE.get());
                        output.accept(ModItems.AERIAL_BOMB.get());
                        output.accept(ModItems.AERIAL_TORPEDO.get());
                        output.accept(ModItems.AIRCRAFT_ROCKET.get());
                    })
                    .build()
            );
}
