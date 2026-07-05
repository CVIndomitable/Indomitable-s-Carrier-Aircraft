package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.aircraft.AircraftRole;
import com.indomitable.carrieraircraft.aircraft.AmmoType;
import com.indomitable.carrieraircraft.item.AircraftSpawnerItem;
import com.indomitable.carrieraircraft.item.AviationAmmoItem;
import com.indomitable.carrieraircraft.item.ControlTerminalItem;
import com.indomitable.carrieraircraft.item.DebugToolItem;
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

    // ==================== 飞机召唤与控制 ====================

    public static final DeferredHolder<Item, AircraftSpawnerItem> B25_SPAWNER =
            ITEMS.register("b25_spawner", () -> new AircraftSpawnerItem(
                    new Item.Properties()
                            .stacksTo(1),
                    AircraftRole.BOMBER
            ));

    public static final DeferredHolder<Item, AircraftSpawnerItem> BTD_SPAWNER =
            ITEMS.register("btd_spawner", () -> new AircraftSpawnerItem(
                    new Item.Properties()
                            .stacksTo(1),
                    AircraftRole.DIVE_BOMBER
            ));

    public static final DeferredHolder<Item, AircraftSpawnerItem> BTD_TORPEDO_SPAWNER =
            ITEMS.register("btd_torpedo_spawner", () -> new AircraftSpawnerItem(
                    new Item.Properties()
                            .stacksTo(1),
                    AircraftRole.TORPEDO_BOMBER,
                    AmmoType.AERIAL_TORPEDO
            ));

    public static final DeferredHolder<Item, AircraftSpawnerItem> ROCKET_ATTACKER_SPAWNER =
            ITEMS.register("rocket_attacker_spawner", () -> new AircraftSpawnerItem(
                    new Item.Properties()
                            .stacksTo(1),
                    AircraftRole.ROCKET_ATTACKER,
                    AmmoType.ROCKET
            ));

    public static final DeferredHolder<Item, AircraftSpawnerItem> ASW_PATROL_SPAWNER =
            ITEMS.register("asw_patrol_spawner", () -> new AircraftSpawnerItem(
                    new Item.Properties()
                            .stacksTo(1),
                    AircraftRole.ASW_PATROL
            ));

    public static final DeferredHolder<Item, ControlTerminalItem> CONTROL_TERMINAL =
            ITEMS.register("control_terminal", () -> new ControlTerminalItem(
                    new Item.Properties()
                            .stacksTo(1)
            ));

    public static final DeferredHolder<Item, DebugToolItem> DEBUG_TOOL =
            ITEMS.register("debug_tool", () -> new DebugToolItem(
                    new Item.Properties()
                            .stacksTo(1)
            ));

    // ==================== 航空弹药 ====================

    public static final DeferredHolder<Item, AviationAmmoItem> AIRCRAFT_MAGAZINE =
            ITEMS.register("aircraft_magazine", () -> new AviationAmmoItem(
                    new Item.Properties().stacksTo(16),
                    AmmoType.MAGAZINE,
                    4,
                    1000
            ));

    public static final DeferredHolder<Item, AviationAmmoItem> AERIAL_BOMB =
            ITEMS.register("aerial_bomb", () -> new AviationAmmoItem(
                    new Item.Properties().stacksTo(64),
                    AmmoType.AERIAL_BOMB,
                    30,
                    1
            ));

    public static final DeferredHolder<Item, AviationAmmoItem> AERIAL_TORPEDO =
            ITEMS.register("aerial_torpedo", () -> new AviationAmmoItem(
                    new Item.Properties().stacksTo(32),
                    AmmoType.AERIAL_TORPEDO,
                    50,
                    1
            ));

    public static final DeferredHolder<Item, AviationAmmoItem> AIRCRAFT_ROCKET =
            ITEMS.register("aircraft_rocket", () -> new AviationAmmoItem(
                    new Item.Properties().stacksTo(64),
                    AmmoType.ROCKET,
                    24,
                    1
            ));
}
