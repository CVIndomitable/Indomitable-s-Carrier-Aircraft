package com.indomitable.carrieraircraft.targeting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

public final class ThreatEvaluator {
    private ThreatEvaluator() {}

    public static double score(Entity entity) {
        double score = 1.0;
        if (entity instanceof LivingEntity living) {
            score += living.getHealth() + living.getMaxHealth() * 0.25;
            if (living instanceof Monster) {
                score += 20.0;
            }
            if (!living.onGround()) {
                score += 8.0;
            }
        }
        score += entity.getDeltaMovement().length() * 8.0;
        return score;
    }
}
