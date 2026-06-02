package com.indomitable.carrieraircraft.item;

import com.indomitable.carrieraircraft.entity.BomberAircraftEntity;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 飞机召唤物品 - Phase 1 MVP
 *
 * 右键使用：
 * - 左键（主手）：在玩家头顶召唤飞机
 * - 右键（副手）：设置锁定目标（基于视线方向）
 */
public class AircraftSpawnerItem extends Item {
    /** 召唤高度偏移 */
    private static final double SPAWN_HEIGHT_OFFSET = 20.0;

    /** 目标锁定最大距离 */
    private static final double MAX_TARGET_DISTANCE = 200.0;

    public AircraftSpawnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;

        if (hand == InteractionHand.MAIN_HAND) {
            // 主手：召唤飞机
            spawnAircraft(serverLevel, player);
        } else {
            // 副手：设置目标
            setTarget(serverLevel, player);
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * 召唤飞机
     */
    private void spawnAircraft(ServerLevel level, Player player) {
        com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.info("=== AIRCRAFT SPAWN REQUESTED ===");

        Vec3 spawnPos = player.position().add(0, SPAWN_HEIGHT_OFFSET, 0);
        com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.info("Spawn position: {}", spawnPos);

        try {
            BomberAircraftEntity aircraft = BomberAircraftEntity.create(
                    level,
                    player.getUUID(),
                    spawnPos
            );
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.info("Aircraft entity created successfully: {}", aircraft);

            // 如果玩家已有锁定目标，立即分配给飞机
            Vec3 target = FireControlSystem.getInstance().getTarget(player.getUUID());
            if (target != null) {
                aircraft.setTarget(target);
                com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.info("Target set: {}", target);
            }

            boolean success = level.addFreshEntity(aircraft);
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.info("addFreshEntity result: {}", success);

            player.sendSystemMessage(Component.literal("§a已召唤B-25轰炸机 (实体ID: " + aircraft.getId() + ", 添加成功: " + success + ")"));

            if (success) {
                com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.info("✅ Aircraft spawned successfully at {} for player {}", spawnPos, player.getName().getString());
            } else {
                com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.error("❌ Failed to add aircraft entity to world");
            }
        } catch (Exception e) {
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.error("❌ Exception during aircraft spawn", e);
            player.sendSystemMessage(Component.literal("§c召唤失败: " + e.getMessage()));
        }
    }

    /**
     * 设置锁定目标
     */
    private void setTarget(ServerLevel level, Player player) {
        var hitResult = player.pick(MAX_TARGET_DISTANCE, 0, false);

        if (hitResult != null) {
            Vec3 target = hitResult.getLocation();
            FireControlSystem.getInstance().setTarget((net.minecraft.server.level.ServerPlayer) player, target);

            player.sendSystemMessage(Component.literal(
                    String.format("§e已锁定目标: (%.1f, %.1f, %.1f)",
                            target.x, target.y, target.z)
            ));
        } else {
            player.sendSystemMessage(Component.literal("§c未找到目标"));
        }
    }
}
