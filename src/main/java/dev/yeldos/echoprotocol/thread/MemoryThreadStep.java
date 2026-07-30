package dev.yeldos.echoprotocol.thread;

import dev.yeldos.echoprotocol.contradiction.ContradictionVariant;

import java.util.EnumSet;
import java.util.Set;

public record MemoryThreadStep(
        MemoryThreadEventType eventType,
        ContradictionVariant contradictionVariant,
        Set<MemoryObservationResult> acceptedOutcomes,
        boolean strong
) {
    public MemoryThreadStep {
        if (eventType == null) {
            throw new IllegalArgumentException("thread step event type is required");
        }
        contradictionVariant = eventType == MemoryThreadEventType.CONTRADICTION
                || eventType == MemoryThreadEventType.FALSE_MEMORY ? contradictionVariant : null;
        acceptedOutcomes = acceptedOutcomes == null || acceptedOutcomes.isEmpty()
                ? Set.of(MemoryObservationResult.COMPLETED)
                : Set.copyOf(acceptedOutcomes);
    }

    public boolean accepts(MemoryObservationResult result) {
        return result != null && result != MemoryObservationResult.EVENT_FAILED && acceptedOutcomes.contains(result);
    }

    public static MemoryThreadStep audio() {
        return new MemoryThreadStep(MemoryThreadEventType.AUDIO_RESIDUE, null,
                EnumSet.of(MemoryObservationResult.IGNORED_SOUND, MemoryObservationResult.INVESTIGATED_SOUND), false);
    }

    public static MemoryThreadStep falseMemory(ContradictionVariant preferred) {
        return new MemoryThreadStep(MemoryThreadEventType.FALSE_MEMORY, preferred,
                EnumSet.of(MemoryObservationResult.DIRECT, MemoryObservationResult.FOLLOWED,
                        MemoryObservationResult.APPROACHED, MemoryObservationResult.RETREATED), false);
    }

    public static MemoryThreadStep peripheral() {
        return new MemoryThreadStep(MemoryThreadEventType.PERIPHERAL_ECHO, null,
                EnumSet.of(MemoryObservationResult.PERIPHERAL, MemoryObservationResult.DIRECT), false);
    }

    public static MemoryThreadStep original() {
        return new MemoryThreadStep(MemoryThreadEventType.ORIGINAL, null,
                EnumSet.of(MemoryObservationResult.DIRECT, MemoryObservationResult.FOLLOWED,
                        MemoryObservationResult.APPROACHED, MemoryObservationResult.RETREATED), true);
    }

    public static MemoryThreadStep panic() {
        return new MemoryThreadStep(MemoryThreadEventType.PANIC_IMPRINT, null,
                EnumSet.of(MemoryObservationResult.DIRECT, MemoryObservationResult.FOLLOWED), false);
    }

    public static MemoryThreadStep contradiction(ContradictionVariant variant, boolean strong) {
        return new MemoryThreadStep(MemoryThreadEventType.CONTRADICTION, variant,
                EnumSet.of(MemoryObservationResult.DIRECT, MemoryObservationResult.FOLLOWED,
                        MemoryObservationResult.APPROACHED), strong);
    }
}
