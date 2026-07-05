package com.indomitable.carrieraircraft.firecontrol;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 火控目标。记录创建时所在的维度，跨维度时目标视为不可用。
 */
public record FireControlTarget(@Nullable UUID entityId, Vec3 fallbackPosition,
                                ResourceKey<Level> dimension, long createdGameTime) {
    public static FireControlTarget position(ServerLevel level, Vec3 position) {
        return new FireControlTarget(null, position, level.dimension(), level.getGameTime());
    }

    public static FireControlTarget entity(Entity entity) {
        return new FireControlTarget(entity.getUUID(), entity.position(),
                entity.level().dimension(), entity.level().getGameTime());
    }

    public boolean inDimension(ServerLevel level) {
        return level.dimension().equals(dimension);
    }

    @Nullable
    public Entity resolveEntity(ServerLevel level) {
        if (entityId == null || !inDimension(level)) {
            return null;
        }
        return level.getEntity(entityId);
    }

    public Vec3 currentPosition(ServerLevel level) {
        Entity entity = resolveEntity(level);
        if (entity != null && entity.isAlive()) {
            return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        }
        return fallbackPosition;
    }

    public boolean isEntityTarget() {
        return entityId != null;
    }
}
