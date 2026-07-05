package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import com.indomitable.carrieraircraft.menu.DebugMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * GUI 菜单类型注册器
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, IndomitableCarrierAircraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ControlTerminalMenu>> CONTROL_TERMINAL =
            MENU_TYPES.register("control_terminal",
                    () -> IMenuTypeExtension.create(ControlTerminalMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DebugMenu>> DEBUG =
            MENU_TYPES.register("debug",
                    () -> IMenuTypeExtension.create(DebugMenu::new));
}
