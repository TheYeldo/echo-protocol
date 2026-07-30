package dev.yeldos.echoprotocol.room;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class RoomMemoryNode {
    public static final int MAXIMUM_ASSOCIATED_BLOCKS = 8;

    private final long id;
    private final String dimension;
    private BlockPos center;
    private RoomMemoryType type;
    private float confidence;
    private int visits;
    private long lastVisitedTick;
    private String familiarLocationId;
    private final List<BlockPos> associatedBlocks = new ArrayList<>();
    private RoomObservationSource source;
    private boolean valid;

    public RoomMemoryNode(long id, String dimension, BlockPos center, RoomMemoryType type, float confidence,
                          int visits, long lastVisitedTick, String familiarLocationId,
                          List<BlockPos> associatedBlocks, RoomObservationSource source, boolean valid) {
        if (id <= 0L || dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("invalid room identity");
        }
        this.id = id;
        this.dimension = dimension;
        this.center = center.toImmutable();
        this.type = type == null ? RoomMemoryType.UNKNOWN : type;
        this.confidence = clamp(confidence);
        this.visits = Math.max(1, visits);
        this.lastVisitedTick = Math.max(0L, lastVisitedTick);
        this.familiarLocationId = familiarLocationId == null ? "" : familiarLocationId;
        this.source = source == null ? RoomObservationSource.LOCAL_PROBE : source;
        this.valid = valid;
        addAssociated(associatedBlocks);
    }

    static RoomMemoryNode from(long id, RoomObservation observation) {
        return new RoomMemoryNode(id, observation.dimension(), observation.center(), observation.type(),
                observation.confidence(), 1, observation.tick(), observation.familiarLocationId(),
                observation.associatedBlocks(), observation.source(), true);
    }

    public void reinforce(RoomObservation observation) {
        if (!dimension.equals(observation.dimension())) {
            throw new IllegalArgumentException("cannot reinforce a room across dimensions");
        }
        visits = Math.min(1_000_000, visits + 1);
        lastVisitedTick = Math.max(lastVisitedTick, observation.tick());
        float previousConfidence = confidence;
        float reinforcement = 0.08F + Math.min(0.12F, observation.confidence() * 0.12F);
        confidence = clamp(Math.max(confidence, observation.confidence() * 0.85F) + reinforcement);
        if (type == RoomMemoryType.UNKNOWN || observation.confidence() > previousConfidence + 0.10F) {
            type = observation.type();
        } else if (observation.type() == type) {
            confidence = clamp(confidence + 0.03F);
        }
        if (!observation.familiarLocationId().isBlank()) {
            familiarLocationId = observation.familiarLocationId();
        }
        source = observation.source();
        valid = true;
        addAssociated(observation.associatedBlocks());
    }

    public void degradeToUnknown(float amount) {
        float bounded = Float.isFinite(amount) ? Math.max(0.0F, Math.min(1.0F, amount)) : 0.0F;
        confidence = clamp(confidence - bounded);
        if (confidence < 0.25F) {
            type = RoomMemoryType.UNKNOWN;
        }
        valid = confidence > 0.05F;
    }

    public boolean canMerge(RoomObservation observation) {
        if (!dimension.equals(observation.dimension())) {
            return false;
        }
        boolean compatible = type == observation.type() || type == RoomMemoryType.UNKNOWN
                || observation.type() == RoomMemoryType.UNKNOWN;
        return compatible && center.getSquaredDistance(observation.center()) <= 25.0D;
    }

    private void addAssociated(List<BlockPos> positions) {
        if (positions == null) {
            return;
        }
        for (BlockPos position : positions) {
            BlockPos immutable = position.toImmutable();
            if (!associatedBlocks.contains(immutable)) {
                associatedBlocks.add(immutable);
            }
            if (associatedBlocks.size() >= MAXIMUM_ASSOCIATED_BLOCKS) {
                break;
            }
        }
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    public long id() { return id; }
    public String dimension() { return dimension; }
    public BlockPos center() { return center; }
    public RoomMemoryType type() { return type; }
    public float confidence() { return confidence; }
    public int visits() { return visits; }
    public long lastVisitedTick() { return lastVisitedTick; }
    public String familiarLocationId() { return familiarLocationId; }
    public List<BlockPos> associatedBlocks() { return List.copyOf(associatedBlocks); }
    public RoomObservationSource source() { return source; }
    public boolean valid() { return valid; }
}
