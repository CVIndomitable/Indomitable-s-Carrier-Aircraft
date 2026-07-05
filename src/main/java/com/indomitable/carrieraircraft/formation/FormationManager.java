package com.indomitable.carrieraircraft.formation;

import com.indomitable.carrieraircraft.aircraft.SquadronType;
import com.indomitable.carrieraircraft.data.CarrierAircraftSavedData;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import com.indomitable.carrieraircraft.firecontrol.FireControlTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FormationManager {
    private static final FormationManager INSTANCE = new FormationManager();
    private static final int LEADER_CHUNK_RADIUS = 1;
    private static final double ASSEMBLY_RADIUS = 32.0;

    private final Map<UUID, List<UUID>> playerAircraft = new HashMap<>();
    private final Map<UUID, Vec3> rallyPoints = new HashMap<>();
    private final Map<UUID, UUID> leaderAircraft = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> aircraftGroups = new HashMap<>();
    private final Map<UUID, Set<String>> groupNames = new HashMap<>();
    private final Map<UUID, Set<Long>> forcedLeaderChunks = new HashMap<>();
    private final Set<UUID> loadedPlayers = new HashSet<>();

    private FormationManager() {}

    public static FormationManager getInstance() {
        return INSTANCE;
    }

    public void registerAircraft(UUID ownerId, AircraftEntity aircraft) {
        if (aircraft.level() instanceof ServerLevel level) {
            loadPlayerData(level, ownerId);
        }
        List<UUID> aircraftIds = playerAircraft.computeIfAbsent(ownerId, id -> new ArrayList<>());
        if (!aircraftIds.contains(aircraft.getUUID())) {
            aircraftIds.add(aircraft.getUUID());
        }
        leaderAircraft.putIfAbsent(ownerId, aircraft.getUUID());
    }

    public void unregisterAircraft(UUID ownerId, UUID aircraftId) {
        List<UUID> aircraftIds = playerAircraft.get(ownerId);
        if (aircraftIds != null) {
            aircraftIds.remove(aircraftId);
            if (aircraftIds.isEmpty()) {
                playerAircraft.remove(ownerId);
            }
        }
        if (aircraftId.equals(leaderAircraft.get(ownerId))) {
            leaderAircraft.remove(ownerId);
            if (aircraftIds != null && !aircraftIds.isEmpty()) {
                leaderAircraft.put(ownerId, aircraftIds.get(0));
            }
        }
        Map<UUID, String> groups = aircraftGroups.get(ownerId);
        if (groups != null) {
            groups.remove(aircraftId);
        }
    }

    public List<AircraftEntity> getAircraft(ServerLevel level, UUID ownerId) {
        loadPlayerData(level, ownerId);
        List<UUID> ids = playerAircraft.getOrDefault(ownerId, List.of());
        List<AircraftEntity> result = new ArrayList<>();
        for (UUID id : ids) {
            if (level.getEntity(id) instanceof AircraftEntity aircraft && aircraft.isAlive()) {
                result.add(aircraft);
            }
        }
        return result;
    }

    public int recall(ServerLevel level, UUID ownerId) {
        loadPlayerData(level, ownerId);
        int count = 0;
        for (AircraftEntity aircraft : getAircraft(level, ownerId)) {
            aircraft.recallToOwner();
            count++;
        }
        return count;
    }

    public int deployToRallyPoint(ServerLevel level, UUID ownerId, Vec3 rallyPoint) {
        loadPlayerData(level, ownerId);
        rallyPoints.put(ownerId, rallyPoint);
        int count = 0;
        for (AircraftEntity aircraft : getAircraft(level, ownerId)) {
            aircraft.setOrbitPoint(rallyPoint);
            count++;
        }
        savePlayerData(level, ownerId);
        return count;
    }

    @Nullable
    public Vec3 getRallyPoint(UUID ownerId) {
        return rallyPoints.get(ownerId);
    }

    @Nullable
    public AircraftEntity getLeader(ServerLevel level, UUID ownerId) {
        loadPlayerData(level, ownerId);
        UUID leaderId = leaderAircraft.get(ownerId);
        if (leaderId != null && level.getEntity(leaderId) instanceof AircraftEntity leader && leader.isAlive()) {
            return leader;
        }

        List<AircraftEntity> aircraft = getAircraft(level, ownerId);
        if (aircraft.isEmpty()) {
            return null;
        }

        AircraftEntity leader = aircraft.get(0);
        leaderAircraft.put(ownerId, leader.getUUID());
        return leader;
    }

    public List<AircraftEntity> getSquadron(ServerLevel level, UUID ownerId, SquadronType type) {
        loadPlayerData(level, ownerId);
        return getAircraft(level, ownerId).stream()
                .filter(aircraft -> aircraft.getRole().squadronType() == type)
                .toList();
    }

    // ── 编组管理 ──

    public void setLeader(UUID ownerId, @Nullable UUID aircraftUUID) {
        if (aircraftUUID == null) {
            leaderAircraft.remove(ownerId);
        } else {
            leaderAircraft.put(ownerId, aircraftUUID);
        }
    }

    @Nullable
    public UUID getLeaderUUID(UUID ownerId) {
        return leaderAircraft.get(ownerId);
    }

    public void createGroup(UUID ownerId, String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return;
        }
        aircraftGroups.computeIfAbsent(ownerId, id -> new java.util.LinkedHashMap<>());
        groupNames.computeIfAbsent(ownerId, id -> new java.util.LinkedHashSet<>()).add(groupName);
    }

    public void addToGroup(UUID ownerId, UUID aircraftUUID, String groupName) {
        createGroup(ownerId, groupName);
        aircraftGroups.computeIfAbsent(ownerId, id -> new java.util.LinkedHashMap<>())
                .put(aircraftUUID, groupName);
    }

    public void removeFromGroup(UUID ownerId, UUID aircraftUUID) {
        Map<UUID, String> groups = aircraftGroups.get(ownerId);
        if (groups != null) {
            groups.remove(aircraftUUID);
        }
    }

    @Nullable
    public String getGroup(UUID ownerId, UUID aircraftUUID) {
        Map<UUID, String> groups = aircraftGroups.get(ownerId);
        return groups != null ? groups.get(aircraftUUID) : null;
    }

    public java.util.List<String> getGroupNames(UUID ownerId) {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>(
                groupNames.getOrDefault(ownerId, java.util.Set.of()));
        Map<UUID, String> groups = aircraftGroups.get(ownerId);
        if (groups != null) {
            names.addAll(groups.values());
        }
        return new java.util.ArrayList<>(names);
    }

    /**
     * 编组出击门槛：同编组飞机必须都靠近盘旋点后才允许自动攻击。
     * 没有盘旋点或未编组飞机不等待，保持当前的快速玩法。
     */
    public boolean isReadyForSortie(ServerLevel level, UUID ownerId, UUID aircraftUUID) {
        loadPlayerData(level, ownerId);
        Vec3 rally = rallyPoints.get(ownerId);
        if (rally == null) {
            return true;
        }
        String group = getGroup(ownerId, aircraftUUID);
        if (group == null) {
            return true;
        }
        Map<UUID, String> groups = aircraftGroups.get(ownerId);
        if (groups == null) {
            return true;
        }
        int expected = 0;
        int ready = 0;
        for (Map.Entry<UUID, String> entry : groups.entrySet()) {
            if (!group.equals(entry.getValue())) {
                continue;
            }
            expected++;
            if (level.getEntity(entry.getKey()) instanceof AircraftEntity aircraft && aircraft.isAlive()
                    && aircraft.position().distanceToSqr(rally) <= ASSEMBLY_RADIUS * ASSEMBLY_RADIUS) {
                ready++;
            }
        }
        return expected <= 1 || expected == ready;
    }

    /** 按任务类型过滤：火控实体目标为水下时反潜中队参与，否则反潜中队不抢普通目标。 */
    public boolean shouldParticipate(AircraftEntity aircraft, @Nullable FireControlTarget target, ServerLevel level) {
        if (target == null || !target.isEntityTarget()) {
            return true;
        }
        var entity = target.resolveEntity(level);
        if (entity == null) {
            return true;
        }
        return aircraft.canAttackEntity(entity);
    }

    /** 给所有在线飞机补给，消耗玩家背包弹药。 */
    public int rearmAll(ServerLevel level, ServerPlayer owner) {
        loadPlayerData(level, owner.getUUID());
        int count = 0;
        for (AircraftEntity aircraft : getAircraft(level, owner.getUUID())) {
            if (aircraft.rearmFromInventory(owner)) {
                count++;
            }
        }
        return count;
    }

    /** 长机周围 3x3 区块强制加载。 */
    public void updateLeaderChunkLoading(ServerLevel level, UUID ownerId) {
        loadPlayerData(level, ownerId);
        AircraftEntity leader = getLeader(level, ownerId);
        if (leader == null || !leader.isAlive()) {
            releaseForcedChunks(level, ownerId);
            return;
        }

        ChunkPos center = leader.chunkPosition();
        Set<Long> desired = new HashSet<>();
        for (int dx = -LEADER_CHUNK_RADIUS; dx <= LEADER_CHUNK_RADIUS; dx++) {
            for (int dz = -LEADER_CHUNK_RADIUS; dz <= LEADER_CHUNK_RADIUS; dz++) {
                desired.add(ChunkPos.asLong(center.x + dx, center.z + dz));
            }
        }

        Set<Long> previous = forcedLeaderChunks.computeIfAbsent(ownerId, id -> new HashSet<>());
        for (Long packed : new HashSet<>(previous)) {
            if (!desired.contains(packed)) {
                level.setChunkForced(chunkX(packed), chunkZ(packed), false);
                previous.remove(packed);
            }
        }
        for (Long packed : desired) {
            if (previous.add(packed)) {
                level.setChunkForced(chunkX(packed), chunkZ(packed), true);
            }
        }
    }

    public int getForcedChunkCount(UUID ownerId) {
        return forcedLeaderChunks.getOrDefault(ownerId, Set.of()).size();
    }

    public void releaseForcedChunks(ServerLevel level, UUID ownerId) {
        Set<Long> chunks = forcedLeaderChunks.remove(ownerId);
        if (chunks == null) {
            return;
        }
        for (Long packed : chunks) {
            level.setChunkForced(chunkX(packed), chunkZ(packed), false);
        }
    }

    private static int chunkX(long packed) {
        return (int) packed;
    }

    private static int chunkZ(long packed) {
        return (int) (packed >> 32);
    }

    public void savePlayerData(ServerLevel level, UUID ownerId) {
        CarrierAircraftSavedData.get(level).saveFormation(
                ownerId,
                rallyPoints.get(ownerId),
                leaderAircraft.get(ownerId),
                aircraftGroups.getOrDefault(ownerId, Map.of()),
                groupNames.getOrDefault(ownerId, Set.of())
        );
    }

    private void loadPlayerData(ServerLevel level, UUID ownerId) {
        if (!loadedPlayers.add(ownerId)) {
            return;
        }
        CarrierAircraftSavedData.FormationRecord record =
                CarrierAircraftSavedData.get(level).loadFormation(ownerId);
        if (record.rallyPoint() != null) {
            rallyPoints.put(ownerId, record.rallyPoint());
        }
        if (record.leader() != null) {
            leaderAircraft.put(ownerId, record.leader());
        }
        if (!record.aircraftGroups().isEmpty()) {
            aircraftGroups.put(ownerId, new HashMap<>(record.aircraftGroups()));
        }
        if (!record.groupNames().isEmpty()) {
            groupNames.put(ownerId, new java.util.LinkedHashSet<>(record.groupNames()));
        }
    }

    /** 玩家下线时调用。在线飞机每 40 tick 会自动重新注册，不会因此失联。 */
    public void clearPlayer(UUID ownerId) {
        playerAircraft.remove(ownerId);
        rallyPoints.remove(ownerId);
        leaderAircraft.remove(ownerId);
        aircraftGroups.remove(ownerId);
        groupNames.remove(ownerId);
        forcedLeaderChunks.remove(ownerId);
        loadedPlayers.remove(ownerId);
    }

    public void clear() {
        playerAircraft.clear();
        rallyPoints.clear();
        leaderAircraft.clear();
        aircraftGroups.clear();
        groupNames.clear();
        forcedLeaderChunks.clear();
        loadedPlayers.clear();
    }
}
