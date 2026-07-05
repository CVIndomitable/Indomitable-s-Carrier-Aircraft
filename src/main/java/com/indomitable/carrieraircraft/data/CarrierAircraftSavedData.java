package com.indomitable.carrieraircraft.data;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.indomitable.carrieraircraft.aircraft.AirDefenseMode;
import com.indomitable.carrieraircraft.aircraft.AssignmentMode;
import com.indomitable.carrieraircraft.aircraft.AutoLockMode;
import com.indomitable.carrieraircraft.firecontrol.PlayerAirControlSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家舰载机控制数据。
 *
 * <p>实体自身状态仍由实体 NBT 保存；这里保存终端配置、盘旋点、长机与编组。
 */
public final class CarrierAircraftSavedData extends SavedData {
    private static final String DATA_NAME = IndomitableCarrierAircraft.MOD_ID + "_control";
    private static final SavedData.Factory<CarrierAircraftSavedData> FACTORY =
            new SavedData.Factory<>(CarrierAircraftSavedData::new, CarrierAircraftSavedData::load, DataFixTypes.LEVEL);

    private final Map<UUID, PlayerRecord> players = new HashMap<>();

    public static CarrierAircraftSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static CarrierAircraftSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CarrierAircraftSavedData data = new CarrierAircraftSavedData();
        ListTag list = tag.getList("Players", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag playerTag = list.getCompound(i);
            if (!playerTag.hasUUID("Player")) {
                continue;
            }
            UUID playerId = playerTag.getUUID("Player");
            data.players.put(playerId, PlayerRecord.load(playerTag));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerRecord> entry : players.entrySet()) {
            CompoundTag playerTag = entry.getValue().save();
            playerTag.putUUID("Player", entry.getKey());
            list.add(playerTag);
        }
        tag.put("Players", list);
        return tag;
    }

    public PlayerAirControlSettings loadSettings(UUID playerId) {
        PlayerRecord record = players.get(playerId);
        PlayerAirControlSettings settings = new PlayerAirControlSettings(playerId);
        if (record == null) {
            return settings;
        }
        settings.setAutoLockMode(record.autoLockMode);
        settings.setAssignmentMode(record.assignmentMode);
        settings.setAirDefenseMode(record.airDefenseMode);
        settings.setBombsPerPass(record.bombsPerPass);
        settings.setMinimumEffectiveDamage(record.minimumEffectiveDamage);
        return settings;
    }

    public void saveSettings(PlayerAirControlSettings settings) {
        PlayerRecord record = players.computeIfAbsent(settings.playerId(), id -> new PlayerRecord());
        record.autoLockMode = settings.autoLockMode();
        record.assignmentMode = settings.assignmentMode();
        record.airDefenseMode = settings.airDefenseMode();
        record.bombsPerPass = settings.bombsPerPass();
        record.minimumEffectiveDamage = settings.minimumEffectiveDamage();
        setDirty();
    }

    public FormationRecord loadFormation(UUID playerId) {
        PlayerRecord record = players.get(playerId);
        return record == null ? FormationRecord.empty() : record.toFormationRecord();
    }

    public void saveFormation(UUID playerId, @Nullable Vec3 rallyPoint, @Nullable UUID leader,
                              Map<UUID, String> aircraftGroups, Set<String> groupNames) {
        PlayerRecord record = players.computeIfAbsent(playerId, id -> new PlayerRecord());
        record.rallyPoint = rallyPoint;
        record.leader = leader;
        record.aircraftGroups = new HashMap<>(aircraftGroups);
        record.groupNames = new LinkedHashSet<>(groupNames);
        setDirty();
    }

    public record FormationRecord(@Nullable Vec3 rallyPoint, @Nullable UUID leader,
                                  Map<UUID, String> aircraftGroups, Set<String> groupNames) {
        public static FormationRecord empty() {
            return new FormationRecord(null, null, Map.of(), Set.of());
        }
    }

    private static final class PlayerRecord {
        private AutoLockMode autoLockMode = AutoLockMode.NEAREST;
        private AssignmentMode assignmentMode = AssignmentMode.FOCUS;
        private AirDefenseMode airDefenseMode = AirDefenseMode.SELF_DEFENSE;
        private int bombsPerPass = 1;
        private float minimumEffectiveDamage = 20.0F;
        @Nullable
        private Vec3 rallyPoint;
        @Nullable
        private UUID leader;
        private Map<UUID, String> aircraftGroups = new HashMap<>();
        private Set<String> groupNames = new LinkedHashSet<>();

        private FormationRecord toFormationRecord() {
            return new FormationRecord(rallyPoint, leader, Map.copyOf(aircraftGroups), Set.copyOf(groupNames));
        }

        private static PlayerRecord load(CompoundTag tag) {
            PlayerRecord record = new PlayerRecord();
            record.autoLockMode = loadEnum(tag, "AutoLockMode", AutoLockMode.class, AutoLockMode.NEAREST);
            record.assignmentMode = loadEnum(tag, "AssignmentMode", AssignmentMode.class, AssignmentMode.FOCUS);
            record.airDefenseMode = loadEnum(tag, "AirDefenseMode", AirDefenseMode.class, AirDefenseMode.SELF_DEFENSE);
            record.bombsPerPass = tag.contains("BombsPerPass") ? tag.getInt("BombsPerPass") : 1;
            record.minimumEffectiveDamage = tag.contains("MinimumEffectiveDamage")
                    ? tag.getFloat("MinimumEffectiveDamage") : 20.0F;

            if (tag.contains("RallyX")) {
                record.rallyPoint = new Vec3(tag.getDouble("RallyX"), tag.getDouble("RallyY"), tag.getDouble("RallyZ"));
            }
            if (tag.hasUUID("Leader")) {
                record.leader = tag.getUUID("Leader");
            }

            ListTag groups = tag.getList("AircraftGroups", 10);
            for (int i = 0; i < groups.size(); i++) {
                CompoundTag groupTag = groups.getCompound(i);
                if (groupTag.hasUUID("Aircraft") && groupTag.contains("Group")) {
                    record.aircraftGroups.put(groupTag.getUUID("Aircraft"), groupTag.getString("Group"));
                }
            }

            ListTag names = tag.getList("GroupNames", 8);
            for (int i = 0; i < names.size(); i++) {
                record.groupNames.add(names.getString(i));
            }
            record.groupNames.addAll(record.aircraftGroups.values());
            return record;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("AutoLockMode", autoLockMode.name());
            tag.putString("AssignmentMode", assignmentMode.name());
            tag.putString("AirDefenseMode", airDefenseMode.name());
            tag.putInt("BombsPerPass", bombsPerPass);
            tag.putFloat("MinimumEffectiveDamage", minimumEffectiveDamage);
            if (rallyPoint != null) {
                tag.putDouble("RallyX", rallyPoint.x);
                tag.putDouble("RallyY", rallyPoint.y);
                tag.putDouble("RallyZ", rallyPoint.z);
            }
            if (leader != null) {
                tag.putUUID("Leader", leader);
            }

            ListTag groups = new ListTag();
            for (Map.Entry<UUID, String> entry : aircraftGroups.entrySet()) {
                CompoundTag groupTag = new CompoundTag();
                groupTag.putUUID("Aircraft", entry.getKey());
                groupTag.putString("Group", entry.getValue());
                groups.add(groupTag);
            }
            tag.put("AircraftGroups", groups);

            ListTag names = new ListTag();
            for (String name : groupNames) {
                names.add(StringTag.valueOf(name));
            }
            tag.put("GroupNames", names);
            return tag;
        }

        private static <E extends Enum<E>> E loadEnum(CompoundTag tag, String key, Class<E> type, E fallback) {
            if (!tag.contains(key)) {
                return fallback;
            }
            try {
                return Enum.valueOf(type, tag.getString(key));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }
}
