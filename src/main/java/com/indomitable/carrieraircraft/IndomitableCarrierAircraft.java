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
 *   2. ModBlocks             — 方块注册（控制面板等；目前为空，由 ControlTerminalItem 提供 GUI 入口）
 *   3. ModItems              — 物品注册（飞机召唤物品等）
 *   4. ModCreativeTabs       — 创造模式标签页
 *   5. ModMenuTypes          — GUI 菜单类型（控制面板 GUI）
 *   6. ModEntityTypes        — 实体类型（飞机/弹药实体）
 *   7. ModSounds             — 音效注册
 *
 * 项目阶段：核心玩法版
 * 当前目标：多机型 + 完整状态机骨架 + 火控/编组/控制终端闭环
 */
@Mod(IndomitableCarrierAircraft.MOD_ID)
public class IndomitableCarrierAircraft {
    public static final String MOD_ID = "indomitablecarrieraircraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IndomitableCarrierAircraft(IEventBus modEventBus, ModContainer modContainer) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Indomitable's Carrier Aircraft initialization");
        }

        // 注册所有子系统
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
    }
}
