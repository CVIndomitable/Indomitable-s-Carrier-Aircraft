package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * GUI 菜单类型注册器
 * 用于注册控制面板 GUI 的菜单类型
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, IndomitableCarrierAircraft.MOD_ID);

    // TODO: Phase 3 - 添加控制面板菜单类型
}
