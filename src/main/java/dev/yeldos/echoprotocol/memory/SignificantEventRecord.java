package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.thread.MemoryObservationResult;

public record SignificantEventRecord(
        String eventType,
        String dimension,
        long roomNodeId,
        long tick,
        MemoryObservationResult observation,
        boolean strong
) {
    public SignificantEventRecord {
        eventType = eventType == null ? "unknown" : eventType.substring(0, Math.min(64, eventType.length()));
        dimension = dimension == null ? "" : dimension.substring(0, Math.min(128, dimension.length()));
        roomNodeId = Math.max(0L, roomNodeId);
        tick = Math.max(0L, tick);
        observation = observation == null ? MemoryObservationResult.NOT_PRESENT : observation;
    }
}
