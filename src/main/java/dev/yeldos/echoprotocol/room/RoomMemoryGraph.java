package dev.yeldos.echoprotocol.room;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class RoomMemoryGraph {
    public static final int HARD_MAXIMUM_NODES = 20;
    public static final int HARD_MAXIMUM_EDGES = 48;

    private final List<RoomMemoryNode> nodes = new ArrayList<>();
    private final List<RoomMemoryEdge> edges = new ArrayList<>();
    private long nextNodeId = 1L;
    private Runnable mutationListener = () -> { };

    public void setMutationListener(Runnable listener) {
        mutationListener = listener == null ? () -> { } : listener;
    }

    public RoomMemoryNode observe(RoomObservation observation, int maximumNodes, int maximumEdges) {
        RoomMemoryNode node = nodes.stream().filter(candidate -> candidate.canMerge(observation)).findFirst()
                .orElse(null);
        if (node == null) {
            node = RoomMemoryNode.from(nextNodeId++, observation);
            nodes.add(node);
        } else {
            node.reinforce(observation);
        }
        trim(maximumNodes, maximumEdges);
        mutationListener.run();
        return node;
    }

    public boolean reinforceEdge(long sourceNodeId, long destinationNodeId, BlockPos transitionPosition,
                                 long tick, int maximumEdges) {
        RoomMemoryNode source = node(sourceNodeId).orElse(null);
        RoomMemoryNode destination = node(destinationNodeId).orElse(null);
        if (source == null || destination == null || sourceNodeId == destinationNodeId
                || !source.dimension().equals(destination.dimension())) {
            return false;
        }
        RoomMemoryEdge edge = edges.stream()
                .filter(candidate -> candidate.connects(sourceNodeId, destinationNodeId)).findFirst().orElse(null);
        if (edge == null) {
            double distance = Math.sqrt(source.center().getSquaredDistance(destination.center()));
            edges.add(new RoomMemoryEdge(sourceNodeId, destinationNodeId, transitionPosition, 1, tick, 0.35F,
                    distance));
        } else {
            edge.reinforce(transitionPosition, tick);
        }
        trimEdges(maximumEdges);
        mutationListener.run();
        return true;
    }

    public void restore(RoomMemoryNode node) {
        if (node == null || nodes.size() >= HARD_MAXIMUM_NODES || node(node.id()).isPresent()) {
            return;
        }
        nodes.add(node);
        nextNodeId = Math.max(nextNodeId, node.id() + 1L);
    }

    public void restore(RoomMemoryEdge edge) {
        if (edge == null || edges.size() >= HARD_MAXIMUM_EDGES
                || node(edge.sourceNodeId()).isEmpty() || node(edge.destinationNodeId()).isEmpty()
                || edge.sourceNodeId() == edge.destinationNodeId()) {
            return;
        }
        RoomMemoryNode source = node(edge.sourceNodeId()).orElseThrow();
        RoomMemoryNode destination = node(edge.destinationNodeId()).orElseThrow();
        if (source.dimension().equals(destination.dimension())) {
            edges.add(edge);
        }
    }

    public void trim(int maximumNodes, int maximumEdges) {
        int nodeLimit = Math.max(1, Math.min(HARD_MAXIMUM_NODES, maximumNodes));
        while (nodes.size() > nodeLimit) {
            RoomMemoryNode evicted = nodes.stream().min(Comparator
                    .comparingDouble(RoomMemoryGraph::relevance)
                    .thenComparingLong(RoomMemoryNode::lastVisitedTick)).orElseThrow();
            nodes.remove(evicted);
            edges.removeIf(edge -> edge.sourceNodeId() == evicted.id() || edge.destinationNodeId() == evicted.id());
        }
        trimEdges(maximumEdges);
    }

    private void trimEdges(int maximumEdges) {
        int edgeLimit = Math.max(0, Math.min(HARD_MAXIMUM_EDGES, maximumEdges));
        while (edges.size() > edgeLimit) {
            RoomMemoryEdge evicted = edges.stream().min(Comparator
                    .comparingDouble(RoomMemoryEdge::confidence)
                    .thenComparingInt(RoomMemoryEdge::transitionCount)
                    .thenComparingLong(RoomMemoryEdge::lastUsedTick)).orElseThrow();
            edges.remove(evicted);
        }
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        for (RoomMemoryNode node : nodes) {
            if (!ids.add(node.id())) {
                errors.add("duplicate room node " + node.id());
            }
            if (node.dimension().isBlank()) {
                errors.add("room " + node.id() + " has no dimension");
            }
        }
        for (RoomMemoryEdge edge : edges) {
            if (!ids.contains(edge.sourceNodeId()) || !ids.contains(edge.destinationNodeId())) {
                errors.add("edge references missing room");
            }
        }
        if (nodes.size() > HARD_MAXIMUM_NODES || edges.size() > HARD_MAXIMUM_EDGES) {
            errors.add("room graph exceeds hard bounds");
        }
        return List.copyOf(errors);
    }

    public Optional<RoomMemoryNode> node(long id) {
        return nodes.stream().filter(node -> node.id() == id).findFirst();
    }

    public Optional<RoomMemoryNode> nearest(String dimension, BlockPos position, double maximumDistance) {
        double maximumSquared = maximumDistance * maximumDistance;
        return nodes.stream().filter(RoomMemoryNode::valid)
                .filter(node -> node.dimension().equals(dimension))
                .filter(node -> node.center().getSquaredDistance(position) <= maximumSquared)
                .min(Comparator.comparingDouble(node -> node.center().getSquaredDistance(position)));
    }

    public List<RoomMemoryNode> nodes() { return List.copyOf(nodes); }
    public List<RoomMemoryEdge> edges() { return List.copyOf(edges); }
    public long nextNodeId() { return nextNodeId; }

    public void clear() {
        nodes.clear();
        edges.clear();
        nextNodeId = 1L;
        mutationListener.run();
    }

    private static double relevance(RoomMemoryNode node) {
        return node.confidence() * 4.0D + Math.log1p(node.visits());
    }
}
