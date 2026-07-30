package dev.yeldos.echoprotocol.room;

import net.minecraft.util.math.BlockPos;

public final class RoomMemoryEdge {
    private final long sourceNodeId;
    private final long destinationNodeId;
    private BlockPos transitionPosition;
    private int transitionCount;
    private long lastUsedTick;
    private float confidence;
    private final double approximateTravelDistance;

    public RoomMemoryEdge(long sourceNodeId, long destinationNodeId, BlockPos transitionPosition,
                          int transitionCount, long lastUsedTick, float confidence,
                          double approximateTravelDistance) {
        if (sourceNodeId <= 0L || destinationNodeId <= 0L || sourceNodeId == destinationNodeId) {
            throw new IllegalArgumentException("invalid room edge");
        }
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.transitionPosition = transitionPosition == null ? null : transitionPosition.toImmutable();
        this.transitionCount = Math.max(1, transitionCount);
        this.lastUsedTick = Math.max(0L, lastUsedTick);
        this.confidence = clamp(confidence);
        this.approximateTravelDistance = Double.isFinite(approximateTravelDistance)
                ? Math.max(0.0D, Math.min(256.0D, approximateTravelDistance)) : 0.0D;
    }

    public void reinforce(BlockPos transition, long tick) {
        transitionCount = Math.min(1_000_000, transitionCount + 1);
        lastUsedTick = Math.max(lastUsedTick, tick);
        confidence = clamp(confidence + 0.10F);
        if (transition != null) {
            transitionPosition = transition.toImmutable();
        }
    }

    public boolean connects(long source, long destination) {
        return sourceNodeId == source && destinationNodeId == destination;
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    public long sourceNodeId() { return sourceNodeId; }
    public long destinationNodeId() { return destinationNodeId; }
    public BlockPos transitionPosition() { return transitionPosition; }
    public int transitionCount() { return transitionCount; }
    public long lastUsedTick() { return lastUsedTick; }
    public float confidence() { return confidence; }
    public double approximateTravelDistance() { return approximateTravelDistance; }
}
