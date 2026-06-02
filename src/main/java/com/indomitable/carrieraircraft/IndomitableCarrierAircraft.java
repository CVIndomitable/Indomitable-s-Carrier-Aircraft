package com.indomitable.carrieraircraft;

import com.mojang.logging.LogUtils;
import com.indomitable.carrieraircraft.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 不挠的舰载机 (Indomitable's Carrier Aircraft) — Minecraft 舰载机模组主入口 (NeoForge 1.21.1)
 *
 * 子系统注册清单（按构造顺序）：
 *   1. ModDataComponents     — 自定义 DataComponent（飞机状态/弹药/锁定目标等）
 *   2. ModBlocks             — 方块注册（控制面板等）
 *   3. ModItems              — 物品注册（飞机召唤物品等）
 *   4. ModCreativeTabs       — 创造模式标签页
 *   5. ModMenuTypes          — GUI 菜单类型（控制面板 GUI）
 *   6. ModEntityTypes        — 实体类型（飞机/弹药实体）
 *   7. ModSounds             — 音效注册
 *
 * 项目阶段：MVP - Phase 1
 * 当前目标：单一机型（水平轰炸机）+ 基础状态机 + 简单弹道 + 火控系统
 */
@Mod(IndomitableCarrierAircraft.MOD_ID)
public class IndomitableCarrierAircraft {
    public static final String MOD_ID = "indomitablecarrieraircraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IndomitableCarrierAircraft(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("=== Indomitable's Carrier Aircraft Initialization START ===");

        // 注册所有子系统
        LOGGER.info("Registering DataComponents...");
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        LOGGER.info("Registering Blocks...");
        ModBlocks.BLOCKS.register(modEventBus);

        LOGGER.info("Registering Items...");
        ModItems.ITEMS.register(modEventBus);

        LOGGER.info("Registering Creative Tabs...");
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        LOGGER.info("Registering Menu Types...");
        ModMenuTypes.MENU_TYPES.register(modEventBus);

        LOGGER.info("Registering Entity Types...");
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        LOGGER.info("Registering Sounds...");
        ModSounds.SOUND_EVENTS.register(modEventBus);

        LOGGER.info("=== Indomitable's Carrier Aircraft Initialization COMPLETE ===");
    }
}
