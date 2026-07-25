package dev.yeldos.echoprotocol.falsememory;

import dev.yeldos.echoprotocol.recording.RecordedFrame;

import java.util.List;

public record FalseMemoryPlan(
        List<RecordedFrame> realPrefix,
        int branchPoint,
        List<RecordedFrame> fabricatedFrames,
        List<FalseMemoryDeviation> deviations,
        long seed,
        String relatedSignature,
        boolean panicImprint,
        boolean entersUnauthenticLocation
) {
    public FalseMemoryPlan {
        realPrefix = List.copyOf(realPrefix.subList(0, Math.min(realPrefix.size(), 120)));
        fabricatedFrames = List.copyOf(fabricatedFrames.subList(0, Math.min(fabricatedFrames.size(), 160)));
        deviations = List.copyOf(deviations.subList(0, Math.min(deviations.size(), 4)));
        branchPoint = Math.max(0, Math.min(branchPoint, Math.max(0, realPrefix.size() - 1)));
    }

    public boolean majorDeviation() {
        return panicImprint || deviations.contains(FalseMemoryDeviation.REVERSE_ROUTE)
                || deviations.contains(FalseMemoryDeviation.APPROACH_FAMILIAR_PLACE);
    }

    public boolean vanishWhenObserved() {
        return deviations.contains(FalseMemoryDeviation.VANISH_WHEN_OBSERVED);
    }
}
