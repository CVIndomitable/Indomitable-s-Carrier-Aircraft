package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册器
 * 用于注册控制面板等方块
 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.createBlocks(IndomitableCarrierAircraft.MOD_ID);

    // TODO: Phase 3 - 添加控制面板方块
}
