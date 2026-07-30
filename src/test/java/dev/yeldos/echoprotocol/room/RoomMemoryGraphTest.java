package dev.yeldos.echoprotocol.room;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomMemoryGraphTest {
    @Test
    void mergesAndReinforcesSameLocalRoom() {
        RoomMemoryGraph graph = new RoomMemoryGraph();
        RoomMemoryNode first = graph.observe(observe("minecraft:overworld", 0, RoomMemoryType.STORAGE, 1), 20, 48);
        RoomMemoryNode second = graph.observe(observe("minecraft:overworld", 3, RoomMemoryType.STORAGE, 2), 20, 48);

        assertEquals(first.id(), second.id());
        assertEquals(2, second.visits());
        assertTrue(second.confidence() > 0.5F);
    }

    @Test
    void keepsDimensionsAndDifferentActivityRoomsSeparate() {
        RoomMemoryGraph graph = new RoomMemoryGraph();
        long overworld = graph.observe(observe("minecraft:overworld", 0, RoomMemoryType.STORAGE, 1), 20, 48).id();
        long nether = graph.observe(observe("minecraft:the_nether", 0, RoomMemoryType.STORAGE, 2), 20, 48).id();
        long bedroom = graph.observe(observe("minecraft:overworld", 1, RoomMemoryType.BEDROOM, 3), 20, 48).id();

        assertNotEquals(overworld, nether);
        assertNotEquals(overworld, bedroom);
        assertEquals(3, graph.nodes().size());
    }

    @Test
    void reinforcesEdgesAndEvictsWithinHardBounds() {
        RoomMemoryGraph graph = new RoomMemoryGraph();
        RoomMemoryNode left = graph.observe(observe("minecraft:overworld", 0, RoomMemoryType.ENTRANCE, 1), 20, 48);
        RoomMemoryNode right = graph.observe(observe("minecraft:overworld", 8, RoomMemoryType.BEDROOM, 2), 20, 48);
        graph.reinforceEdge(left.id(), right.id(), new BlockPos(4, 64, 0), 3, 48);
        graph.reinforceEdge(left.id(), right.id(), new BlockPos(4, 64, 0), 4, 48);
        assertEquals(2, graph.edges().getFirst().transitionCount());

        for (int index = 0; index < 30; index++) {
            graph.observe(observe("minecraft:overworld", 20 + index * 8, RoomMemoryType.IDLE, 10 + index), 5, 3);
        }
        assertTrue(graph.nodes().size() <= 5);
        assertTrue(graph.edges().size() <= 3);
        assertTrue(graph.validate().isEmpty());
    }

    @Test
    void disconnectCleanupDropsThePreviousTransitionNode() {
        RoomMemoryGraph graph = new RoomMemoryGraph();
        RoomTransitionTracker tracker = new RoomTransitionTracker();
        UUID player = UUID.fromString("c22da9af-5aa0-46e6-9fd0-7a64d0cb8f2d");
        RoomMemoryNode entrance = graph.observe(observe("minecraft:overworld", 0,
                RoomMemoryType.ENTRANCE, 1), 20, 48);
        RoomMemoryNode bedroom = graph.observe(observe("minecraft:overworld", 8,
                RoomMemoryType.BEDROOM, 2), 20, 48);

        assertFalse(tracker.observe(player, graph, entrance, null, 10, 48));
        tracker.clear(player);
        assertFalse(tracker.observe(player, graph, bedroom, null, 30, 48));
        assertTrue(graph.edges().isEmpty());
    }

    @Test
    void dimensionCleanupCannotCreateACrossDimensionTransition() {
        RoomMemoryGraph graph = new RoomMemoryGraph();
        RoomTransitionTracker tracker = new RoomTransitionTracker();
        UUID player = UUID.fromString("bd6e43b7-5717-4afe-ac55-5186fe7186fb");
        RoomMemoryNode overworld = graph.observe(observe("minecraft:overworld", 0,
                RoomMemoryType.PORTAL, 1), 20, 48);
        RoomMemoryNode nether = graph.observe(observe("minecraft:the_nether", 0,
                RoomMemoryType.PORTAL, 2), 20, 48);

        assertFalse(tracker.observe(player, graph, overworld, null, 10, 48));
        tracker.clear(player);
        assertFalse(tracker.observe(player, graph, nether, null, 30, 48));
        assertTrue(graph.edges().isEmpty());
    }

    private static RoomObservation observe(String dimension, int x, RoomMemoryType type, long tick) {
        return new RoomObservation(dimension, new BlockPos(x, 64, 0), type, 0.55F, tick, "", List.of(),
                RoomObservationSource.EXPLICIT_INTERACTION);
    }
}
