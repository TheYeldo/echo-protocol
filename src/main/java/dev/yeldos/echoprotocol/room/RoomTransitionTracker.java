package dev.yeldos.echoprotocol.room;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RoomTransitionTracker {
    private final Map<UUID, Long> currentNodes = new HashMap<>();
    private final Map<UUID, Long> lastTransitionTicks = new HashMap<>();

    public boolean observe(UUID playerUuid, RoomMemoryGraph graph, RoomMemoryNode node, BlockPos transitionPosition,
                           long tick, int maximumEdges) {
        Long previous = currentNodes.put(playerUuid, node.id());
        if (previous == null || previous == node.id()) {
            return false;
        }
        long last = lastTransitionTicks.getOrDefault(playerUuid, Long.MIN_VALUE / 2);
        if (tick - last < 10L) {
            return false;
        }
        boolean reinforced = graph.reinforceEdge(previous, node.id(), transitionPosition, tick, maximumEdges);
        if (reinforced) {
            lastTransitionTicks.put(playerUuid, tick);
        }
        return reinforced;
    }

    public void clear(UUID playerUuid) {
        currentNodes.remove(playerUuid);
        lastTransitionTicks.remove(playerUuid);
    }

    public void clearAll() {
        currentNodes.clear();
        lastTransitionTicks.clear();
    }
}
