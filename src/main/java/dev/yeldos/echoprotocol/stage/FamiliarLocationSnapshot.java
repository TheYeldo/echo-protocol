package dev.yeldos.echoprotocol.stage;

import net.minecraft.util.math.BlockPos;

public record FamiliarLocationSnapshot(
        FamiliarLocationType type,
        String dimension,
        BlockPos position,
        int visits,
        long lastSeenTick
) {
    public FamiliarLocationSnapshot {
        if (type == null || dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("invalid familiar location");
        }
        position = position.toImmutable();
        visits = Math.max(1, visits);
        lastSeenTick = Math.max(0L, lastSeenTick);
    }

    public static FamiliarLocationSnapshot from(FamiliarLocation location) {
        return new FamiliarLocationSnapshot(location.type(), location.dimension(), location.pos(),
                location.visits(), location.lastSeenTick());
    }

    public FamiliarLocation toLocation() {
        return new FamiliarLocation(type, dimension, position, visits, lastSeenTick);
    }
}
