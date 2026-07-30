package dev.yeldos.echoprotocol.stage;

import java.util.List;

public record PlayerEchoStateSnapshot(
        int stage,
        long playTicks,
        long nextEventDelay,
        long nextOriginalEventDelay,
        int stageOneEvents,
        int totalEvents,
        int memoryEvents,
        int corruptedEvents,
        int mimicEvents,
        int originalEvents,
        boolean mimicIndependentActionSeen,
        List<FamiliarLocationSnapshot> familiarLocations
) {
    public PlayerEchoStateSnapshot {
        stage = Math.max(0, Math.min(3, stage));
        playTicks = Math.max(0L, playTicks);
        nextEventDelay = Math.max(0L, nextEventDelay);
        nextOriginalEventDelay = Math.max(0L, nextOriginalEventDelay);
        stageOneEvents = Math.max(0, stageOneEvents);
        totalEvents = Math.max(0, totalEvents);
        memoryEvents = Math.max(0, memoryEvents);
        corruptedEvents = Math.max(0, corruptedEvents);
        mimicEvents = Math.max(0, mimicEvents);
        originalEvents = Math.max(0, originalEvents);
        familiarLocations = familiarLocations == null ? List.of()
                : List.copyOf(familiarLocations.subList(0, Math.min(64, familiarLocations.size())));
    }

    public static PlayerEchoStateSnapshot empty() {
        return new PlayerEchoStateSnapshot(0, 0L, 0L, 0L, 0, 0, 0, 0, 0, 0, false, List.of());
    }
}
