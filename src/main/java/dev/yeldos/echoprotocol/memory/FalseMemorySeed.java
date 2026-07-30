package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.contradiction.ContradictionVariant;

public record FalseMemorySeed(
        long seed,
        String routeSignature,
        ContradictionVariant preferredVariant,
        long roomNodeId,
        long createdTick
) {
    public FalseMemorySeed {
        routeSignature = routeSignature == null ? ""
                : routeSignature.substring(0, Math.min(160, routeSignature.length()));
        roomNodeId = Math.max(0L, roomNodeId);
        createdTick = Math.max(0L, createdTick);
    }
}
