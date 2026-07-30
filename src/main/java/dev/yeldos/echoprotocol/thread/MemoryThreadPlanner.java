package dev.yeldos.echoprotocol.thread;

import dev.yeldos.echoprotocol.contradiction.ContradictionVariant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class MemoryThreadPlanner {
    public Optional<MemoryThread> plan(UUID playerUuid, MemoryThreadType type, long roomNodeId,
                                       String dimension, String relatedItemId, long tick, long seed,
                                       ThreadPlanningOptions options) {
        return plan(playerUuid, type, roomNodeId, dimension, relatedItemId, tick, seed, options, true);
    }

    public Optional<MemoryThread> plan(UUID playerUuid, MemoryThreadType type, long roomNodeId,
                                       String dimension, String relatedItemId, long tick, long seed,
                                       ThreadPlanningOptions options, boolean awardsProgress) {
        List<MemoryThreadStep> template = template(type);
        List<MemoryThreadStep> available = new ArrayList<>();
        for (MemoryThreadStep step : template) {
            MemoryThreadStep adapted = adapt(step, options);
            if (adapted != null && (available.isEmpty() || available.getLast().eventType() != adapted.eventType())) {
                available.add(adapted);
            }
        }
        if (available.size() < options.minimumSteps()) {
            return Optional.empty();
        }
        int upper = Math.min(options.maximumSteps(), available.size());
        int length = options.minimumSteps();
        if (upper > length) {
            length += new Random(seed).nextInt(upper - length + 1);
        }
        List<MemoryThreadStep> selected = new ArrayList<>(available.subList(0, length));
        if (selected.stream().noneMatch(MemoryThreadStep::strong) && available.stream().anyMatch(MemoryThreadStep::strong)
                && length >= 3) {
            selected.set(selected.size() - 1, available.stream().filter(MemoryThreadStep::strong).findFirst().orElseThrow());
        }
        MemoryThread planned = new MemoryThread(playerUuid, type, roomNodeId, dimension, relatedItemId, selected, tick);
        if (!awardsProgress) {
            planned = new MemoryThread(playerUuid, type, roomNodeId, dimension, relatedItemId, selected, tick,
                    0, 0L, MemoryObservationResult.NOT_PRESENT, 0.0F, List.of(), MemoryThreadStage.ACTIVE,
                    0, false, false);
        }
        return Optional.of(planned);
    }

    private static MemoryThreadStep adapt(MemoryThreadStep step, ThreadPlanningOptions options) {
        return switch (step.eventType()) {
            case AUDIO_RESIDUE -> options.audioEnabled() ? step : null;
            case FALSE_MEMORY -> options.falseMemoryEnabled() ? step : null;
            case PERIPHERAL_ECHO -> options.peripheralEnabled() ? step : null;
            case ORIGINAL -> options.originalEnabled() ? step : null;
            case PANIC_IMPRINT -> options.panicEnabled() ? step : null;
            case CONTRADICTION -> options.contradictionEnabled() ? step
                    : options.falseMemoryEnabled() ? MemoryThreadStep.falseMemory(step.contradictionVariant()) : null;
        };
    }

    private static List<MemoryThreadStep> template(MemoryThreadType type) {
        return switch (type) {
            case BEDROOM -> List.of(MemoryThreadStep.audio(),
                    MemoryThreadStep.falseMemory(ContradictionVariant.WRONG_DESTINATION),
                    MemoryThreadStep.peripheral(), MemoryThreadStep.original());
            case STORAGE -> List.of(MemoryThreadStep.audio(),
                    MemoryThreadStep.contradiction(ContradictionVariant.CONFLICTING_ITEM, false),
                    MemoryThreadStep.falseMemory(ContradictionVariant.WRONG_DESTINATION), MemoryThreadStep.original());
            case ENTRANCE -> List.of(MemoryThreadStep.audio(), MemoryThreadStep.peripheral(),
                    MemoryThreadStep.contradiction(ContradictionVariant.MEMORY_ARRIVED_FIRST, true),
                    MemoryThreadStep.original());
            case PORTAL -> List.of(MemoryThreadStep.audio(),
                    MemoryThreadStep.falseMemory(ContradictionVariant.WRONG_DESTINATION),
                    MemoryThreadStep.contradiction(ContradictionVariant.MEMORY_ARRIVED_FIRST, true),
                    MemoryThreadStep.original());
            case PANIC -> List.of(MemoryThreadStep.panic(), MemoryThreadStep.audio(),
                    MemoryThreadStep.contradiction(ContradictionVariant.REPEATED_ENDING, true),
                    MemoryThreadStep.original());
            case MISSING_ROUTE -> List.of(MemoryThreadStep.falseMemory(ContradictionVariant.MISSING_SEGMENT),
                    MemoryThreadStep.audio(), MemoryThreadStep.contradiction(ContradictionVariant.REPEATED_ENDING, true));
            case EMPTY_ROOM -> List.of(MemoryThreadStep.audio(), MemoryThreadStep.peripheral(),
                    MemoryThreadStep.falseMemory(ContradictionVariant.WRONG_DESTINATION), MemoryThreadStep.original());
            case FOLLOWED_ECHO -> List.of(MemoryThreadStep.falseMemory(ContradictionVariant.WRONG_DESTINATION),
                    MemoryThreadStep.contradiction(ContradictionVariant.MEMORY_ARRIVED_FIRST, true),
                    MemoryThreadStep.original());
            case IGNORED_SOUND -> List.of(MemoryThreadStep.audio(), MemoryThreadStep.peripheral(),
                    MemoryThreadStep.falseMemory(ContradictionVariant.MISSING_SEGMENT), MemoryThreadStep.original());
        };
    }
}
