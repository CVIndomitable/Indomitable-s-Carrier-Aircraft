package com.indomitable.carrieraircraft.targeting;

import com.indomitable.carrieraircraft.entity.AircraftEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 敌我识别过滤器。
 *
 * <p>多层次识别规则（按优先级）：
 * <ol>
 *   <li>实体类型标签：白名单 {@code indomitablecarrieraircraft:friendly_entities} 或
 *       黑名单 {@code indomitablecarrieraircraft:hostile_entities}</li>
 *   <li>所有者识别：同所有者的召唤物为友方</li>
 *   <li>队伍系统：同队伍的玩家及其召唤物为友方</li>
 *   <li>默认规则：{@link Enemy} 为敌对，{@link Animal}/{@link Villager} 为友方</li>
 * </ol>
 *
 * <p>玩家数据包可通过标签覆盖任何默认规则，实现自定义友敌关系。
 */
public class FriendlyFireFilter {

    private static final TagKey<EntityType<?>> FRIENDLY_ENTITIES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("indomitablecarrieraircraft", "friendly_entities")
    );

    private static final TagKey<EntityType<?>> HOSTILE_ENTITIES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("indomitablecarrieraircraft", "hostile_entities")
    );

    /**
     * 判断实体是否可以被飞机攻击。
     *
     * @param aircraft 攻击方飞机
     * @param target   目标实体
     * @return {@code true} 如果目标是敌对且可以攻击
     */
    public static boolean canAttack(AircraftEntity aircraft, Entity target) {
        if (!target.isAlive() || target == aircraft) {
            return false;
        }

        // 飞机不攻击其他友军飞机
        if (target instanceof AircraftEntity otherAircraft) {
            return !isFriendly(aircraft, otherAircraft);
        }

        // 优先级 1: 实体类型标签
        Boolean tagResult = checkEntityTypeTags(target);
        if (tagResult != null) {
            return !tagResult; // tagResult=true 表示友方，返回 false（不可攻击）
        }

        // 优先级 2: 所有者识别
        UUID attackerOwner = aircraft.getOwnerUUID();
        if (attackerOwner != null) {
            // 不攻击自己的所有者
            if (target instanceof Player player && player.getUUID().equals(attackerOwner)) {
                return false;
            }

            // 不攻击同所有者的召唤物
            if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
                if (ownable.getOwnerUUID().equals(attackerOwner)) {
                    return false;
                }
            }
        }

        // 优先级 3: 队伍系统
        Boolean teamResult = checkTeamRelation(aircraft, target);
        if (teamResult != null) {
            return !teamResult; // teamResult=true 表示友方
        }

        // 优先级 4: 默认规则
        return isDefaultHostile(target);
    }

    /**
     * 判断两架飞机是否友军。
     */
    public static boolean isFriendly(AircraftEntity aircraft1, AircraftEntity aircraft2) {
        if (aircraft1 == aircraft2) {
            return true;
        }

        UUID owner1 = aircraft1.getOwnerUUID();
        UUID owner2 = aircraft2.getOwnerUUID();

        // 同所有者 = 友军
        if (owner1 != null && owner1.equals(owner2)) {
            return true;
        }

        // 同队伍 = 友军
        PlayerTeam team1 = aircraft1.getTeam();
        PlayerTeam team2 = aircraft2.getTeam();
        if (team1 != null && team1 == team2) {
            return true;
        }

        return false;
    }

    /**
     * 检查实体类型标签（数据包自定义规则）。
     *
     * @return {@code true} 友方，{@code false} 敌对，{@code null} 标签无定义
     */
    @Nullable
    private static Boolean checkEntityTypeTags(Entity entity) {
        EntityType<?> type = entity.getType();

        // 友方白名单优先
        if (type.is(FRIENDLY_ENTITIES)) {
            return true;
        }

        // 敌对黑名单次之
        if (type.is(HOSTILE_ENTITIES)) {
            return false;
        }

        return null; // 标签未定义，交由下游规则判断
    }

    /**
     * 检查队伍关系。
     *
     * @return {@code true} 同队伍友方，{@code false} 不同队伍敌对，{@code null} 无队伍或队伍设置不限制友伤
     */
    @Nullable
    private static Boolean checkTeamRelation(AircraftEntity aircraft, Entity target) {
        PlayerTeam aircraftTeam = aircraft.getTeam();
        PlayerTeam targetTeam = target.getTeam();

        // 双方都无队伍，无法判断
        if (aircraftTeam == null && targetTeam == null) {
            return null;
        }

        // 同队伍
        if (aircraftTeam != null && aircraftTeam == targetTeam) {
            // 检查队伍是否允许友伤
            if (aircraftTeam.isAllowFriendlyFire()) {
                return null; // 队伍允许友伤，交由默认规则判断
            }
            return true; // 同队且禁止友伤 = 友方
        }

        // 不同队伍或一方无队伍，交由默认规则
        return null;
    }

    /**
     * 默认敌对规则（原版行为）。
     */
    private static boolean isDefaultHostile(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        // Enemy 接口 = 敌对
        if (entity instanceof Enemy) {
            return true;
        }

        // 被动生物 = 友方
        if (entity instanceof Animal || entity instanceof Villager) {
            return false;
        }

        // 玩家默认不敌对（PvP 由队伍系统或标签控制）
        if (entity instanceof Player) {
            return false;
        }

        // 其他未定义实体默认不攻击
        return false;
    }

    /**
     * 玩家是否可以通过火控系统锁定该目标。
     *
     * <p>此方法用于手动锁定时的过滤，比自动攻击更宽松（允许玩家锁定坐标、中立生物等）。
     *
     * @param playerUUID 玩家 UUID
     * @param target     目标实体
     * @return {@code true} 可以锁定
     */
    public static boolean canPlayerTarget(UUID playerUUID, Entity target) {
        // 玩家不能锁定自己
        if (target instanceof Player player && player.getUUID().equals(playerUUID)) {
            return false;
        }

        // 玩家不能锁定自己的召唤物（除非是队伍允许友伤的情况）
        if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            if (ownable.getOwnerUUID().equals(playerUUID)) {
                // 检查队伍设置
                PlayerTeam team = target.getTeam();
                if (team == null || !team.isAllowFriendlyFire()) {
                    return false; // 禁止友伤，不可锁定
                }
            }
        }

        // 其他情况允许锁定（玩家可以手动锁定任何目标，包括坐标、中立生物等）
        return target.isAlive() && target.isPickable();
    }
}
