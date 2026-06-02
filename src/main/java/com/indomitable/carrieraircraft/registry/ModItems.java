package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册器
 * 用于注册飞机召唤物品、弹药等
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.createItems(IndomitableCarrierAircraft.MOD_ID);

    // TODO: Phase 1 - 添加水平轰炸机召唤物品和炸弹物品
}
