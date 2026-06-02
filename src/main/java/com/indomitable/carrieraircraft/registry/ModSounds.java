package com.indomitable.carrieraircraft.registry;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 音效注册器
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, IndomitableCarrierAircraft.MOD_ID);

    // TODO: Phase 5 - 添加飞机引擎声、轰炸声等音效
}
