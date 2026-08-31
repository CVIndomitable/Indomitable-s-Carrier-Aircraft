package com.indomitable.carrieraircraft.item;

import com.indomitable.carrieraircraft.aircraft.AircraftRole;
import com.indomitable.carrieraircraft.aircraft.AircraftSpec;
import com.indomitable.carrieraircraft.aircraft.AmmoType;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.firecontrol.FireControlSystem;
import com.indomitable.carrieraircraft.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 飞机物品。
 *
 * <p>右键放飞：在玩家头顶召唤飞机，物品消耗一个。
 * 回收的飞机会变成带弹药数据的物品，放飞时保留剩余弹药。
 */
public class AircraftSpawnerItem extends Item {
    /** 放飞高度偏移 */
    private static final double SPAWN_HEIGHT_OFFSET = 20.0;

    private final AircraftRole role;
    private final AmmoType preferredSeaAmmo;

    public AircraftSpawnerItem(Properties properties) {
        this(properties, AircraftRole.BOMBER);
    }

    public AircraftSpawnerItem(Properties properties, AircraftRole role) {
        this(properties, role, null);
    }

    public AircraftSpawnerItem(Properties properties, AircraftRole role, AmmoType preferredSeaAmmo) {
        super(properties);
        this.role = role;
        this.preferredSeaAmmo = preferredSeaAmmo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        boolean spawned = spawnAircraft(serverLevel, player, stack);

        if (spawned) {
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    /**
     * 放飞飞机，返回是否成功
     */
    private boolean spawnAircraft(ServerLevel level, Player player, ItemStack stack) {
        // M14：使用 MOTION_BLOCKING 高度图，避免在洞穴/水中固定 +20 时生成在方块内。
        int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                player.blockPosition().getX(), player.blockPosition().getZ());
        Vec3 spawnPos = new Vec3(player.getX(), groundY + SPAWN_HEIGHT_OFFSET, player.getZ());

        try {
            AircraftSpec spec = getSpecForRole(role);
            AmmoType defaultSeaAmmo = defaultSeaAmmoFor(spec);
            AircraftEntity aircraft = AircraftEntity.create(
                    level, player.getUUID(), spawnPos, spec, defaultSeaAmmo, preferredSeaAmmo);

            // 如果物品携带弹药数据（回收的飞机），恢复弹药量
            Integer seaAmmo = stack.get(ModDataComponents.SEA_AMMO_COUNT);
            Integer airAmmo = stack.get(ModDataComponents.AIR_AMMO_COUNT);
            if (seaAmmo != null) aircraft.setAmmoCount(seaAmmo);
            if (airAmmo != null) aircraft.setAirAmmoCount(airAmmo);

            // 如果玩家已有锁定目标，立即分配给飞机
            Vec3 target = FireControlSystem.getInstance().getTarget(player.getUUID());
            if (target != null) aircraft.setTarget(target);

            boolean success = level.addFreshEntity(aircraft);

            if (success) {
                String label = seaAmmo != null || airAmmo != null
                        ? String.format("已放飞 %s（对海 %d，对空 %d）",
                                role.displayName(), aircraft.getAmmoCount(), aircraft.getAirAmmoCount())
                        : "已放飞" + role.displayName();
                player.sendSystemMessage(Component.literal(label).withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal("放飞失败").withStyle(ChatFormatting.RED));
                com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.error("Failed to add aircraft entity to world");
            }

            return success;
        } catch (Exception e) {
            com.indomitable.carrieraircraft.IndomitableCarrierAircraft.LOGGER.error("Exception during aircraft launch", e);
            player.sendSystemMessage(Component.literal("放飞失败: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return false;
        }
    }

    /** 根据角色获取对应的飞机规格 */
    private static AircraftSpec getSpecForRole(AircraftRole role) {
        String specId = com.indomitable.carrieraircraft.aircraft.AircraftSpecLoader.getDefaultIdForRole(role);
        return com.indomitable.carrieraircraft.aircraft.AircraftSpecLoader.getInstance().getSpec(specId);
    }

    private static AmmoType defaultSeaAmmoFor(AircraftSpec spec) {
        if (spec.allowedSeaAmmo().contains(AmmoType.AERIAL_BOMB)) return AmmoType.AERIAL_BOMB;
        if (spec.allowedSeaAmmo().contains(AmmoType.AERIAL_TORPEDO)) return AmmoType.AERIAL_TORPEDO;
        if (spec.allowedSeaAmmo().contains(AmmoType.ROCKET)) return AmmoType.ROCKET;
        return AmmoType.AERIAL_BOMB;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Integer seaAmmo = stack.get(ModDataComponents.SEA_AMMO_COUNT);
        Integer airAmmo = stack.get(ModDataComponents.AIR_AMMO_COUNT);
        if (seaAmmo != null) {
            tooltip.add(Component.literal(String.format("对海弹药: %d", seaAmmo)).withStyle(ChatFormatting.GRAY));
        }
        if (airAmmo != null) {
            tooltip.add(Component.literal(String.format("对空弹药: %d", airAmmo)).withStyle(ChatFormatting.GRAY));
        }
    }
}
