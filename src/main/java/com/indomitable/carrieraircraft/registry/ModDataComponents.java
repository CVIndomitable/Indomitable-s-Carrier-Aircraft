package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DataComponent 注册器。
 *
 * <p>飞机物品回收后通过数据组件携带剩余弹药信息。
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,
                    IndomitableCarrierAircraft.MOD_ID);

    /** 对海弹药剩余量 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SEA_AMMO_COUNT =
            DATA_COMPONENTS.register("sea_ammo_count", () ->
                    DataComponentType.<Integer>builder().persistent(
                            net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT).build());

    /** 对空弹药剩余量 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AIR_AMMO_COUNT =
            DATA_COMPONENTS.register("air_ammo_count", () ->
                    DataComponentType.<Integer>builder().persistent(
                            net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT).build());

    /** 飞机机型 ID */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> AIRCRAFT_ROLE =
            DATA_COMPONENTS.register("aircraft_role", () ->
                    DataComponentType.<String>builder().persistent(
                            Codec.STRING).build());
}
