package com.indomitable.carrieraircraft.firecontrol;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CameraStateTracker {
    private static final CameraStateTracker INSTANCE = new CameraStateTracker();

    private final Map<UUID, CameraState> trackedPlayers = new HashMap<>();

    private CameraStateTracker() {}

    public static CameraStateTracker getInstance() {
        return INSTANCE;
    }

    public void enterLeaderCamera(ServerPlayer player) {
        trackedPlayers.put(player.getUUID(), new CameraState(
                player.position(),
                player.getXRot(),
                player.getYRot()
        ));
    }

    public void exitLeaderCamera(ServerPlayer player) {
        CameraState state = trackedPlayers.remove(player.getUUID());
        if (state != null) {
            player.teleportTo(state.position.x, state.position.y, state.position.z);
            player.setXRot(state.xRot);
            player.setYRot(state.yRot);
        }
    }

    public void clearPlayer(UUID playerId) {
        trackedPlayers.remove(playerId);
    }

    private record CameraState(Vec3 position, float xRot, float yRot) {}
}
