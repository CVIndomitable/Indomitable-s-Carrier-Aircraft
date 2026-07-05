package com.indomitable.carrieraircraft.formation;

import com.indomitable.carrieraircraft.aircraft.SquadronType;
import com.indomitable.carrieraircraft.entity.AircraftEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FormationManager {
    private static final FormationManager INSTANCE = new FormationManager();

    private final Map<UUID, List<UUID>> playerAircraft = new HashMap<>();
    private final Map<UUID, Vec3> rallyPoints = new HashMap<>();
    private final Map<UUID, UUID> leaderAircraft = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> aircraftGroups = new HashMap<>();

    private FormationManager() {}

    public static FormationManager getInstance() {
        return INSTANCE;
    }

    public void registerAircraft(UUID ownerId, AircraftEntity aircraft) {
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
    }

    public List<AircraftEntity> getAircraft(ServerLevel level, UUID ownerId) {
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
        int count = 0;
        for (AircraftEntity aircraft : getAircraft(level, ownerId)) {
            aircraft.recallToOwner();
            count++;
        }
        return count;
    }

    public int deployToRallyPoint(ServerLevel level, UUID ownerId, Vec3 rallyPoint) {
        rallyPoints.put(ownerId, rallyPoint);
        int count = 0;
        for (AircraftEntity aircraft : getAircraft(level, ownerId)) {
            aircraft.setOrbitPoint(rallyPoint);
            count++;
        }
        return count;
    }

    @Nullable
    public Vec3 getRallyPoint(UUID ownerId) {
        return rallyPoints.get(ownerId);
    }

    @Nullable
    public AircraftEntity getLeader(ServerLevel level, UUID ownerId) {
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
        aircraftGroups.computeIfAbsent(ownerId, id -> new java.util.LinkedHashMap<>());
    }

    public void addToGroup(UUID ownerId, UUID aircraftUUID, String groupName) {
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
        Map<UUID, String> groups = aircraftGroups.get(ownerId);
        if (groups == null) return java.util.List.of();
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>(groups.values());
        return new java.util.ArrayList<>(names);
    }

    /** 玩家下线时调用。在线飞机每 40 tick 会自动重新注册，不会因此失联。 */
    public void clearPlayer(UUID ownerId) {
        playerAircraft.remove(ownerId);
        rallyPoints.remove(ownerId);
        leaderAircraft.remove(ownerId);
        aircraftGroups.remove(ownerId);
    }

    public void clear() {
        playerAircraft.clear();
        rallyPoints.clear();
        leaderAircraft.clear();
        aircraftGroups.clear();
    }
}
