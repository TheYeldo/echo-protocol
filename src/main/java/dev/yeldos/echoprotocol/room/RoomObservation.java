package dev.yeldos.echoprotocol.room;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public record RoomObservation(
        String dimension,
        BlockPos center,
        RoomMemoryType type,
        float confidence,
        long tick,
        String familiarLocationId,
        List<BlockPos> associatedBlocks,
        RoomObservationSource source
) {
    public RoomObservation {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must be present");
        }
        center = center.toImmutable();
        type = type == null ? RoomMemoryType.UNKNOWN : type;
        confidence = clamp(confidence);
        tick = Math.max(0L, tick);
        familiarLocationId = familiarLocationId == null ? "" : familiarLocationId;
        associatedBlocks = associatedBlocks == null ? List.of() : associatedBlocks.stream()
                .limit(RoomMemoryNode.MAXIMUM_ASSOCIATED_BLOCKS).map(BlockPos::toImmutable).toList();
        source = source == null ? RoomObservationSource.LOCAL_PROBE : source;
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }
}
