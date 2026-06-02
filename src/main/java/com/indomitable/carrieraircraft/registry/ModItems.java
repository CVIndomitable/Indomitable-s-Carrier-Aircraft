package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.item.AircraftSpawnerItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册器
 * 用于注册飞机召唤物品、弹药等
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.createItems(IndomitableCarrierAircraft.MOD_ID);

    // ==================== Phase 1: 飞机召唤物品 ====================

    public static final DeferredHolder<Item, AircraftSpawnerItem> BOMBER_SPAWNER =
            ITEMS.register("bomber_spawner", () -> new AircraftSpawnerItem(
                    new Item.Properties()
                            .stacksTo(1)
            ));
}
