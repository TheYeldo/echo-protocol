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
    public static final int MAXIMUM_AUTHENTIC_FRAMES = 120 * 20;
    public static final int MAXIMUM_FABRICATED_FRAMES = 160;

    public FalseMemoryPlan {
        realPrefix = List.copyOf(realPrefix.subList(0, Math.min(realPrefix.size(), MAXIMUM_AUTHENTIC_FRAMES)));
        fabricatedFrames = List.copyOf(fabricatedFrames.subList(0,
                Math.min(fabricatedFrames.size(), MAXIMUM_FABRICATED_FRAMES)));
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

    public static boolean hasObservableDeviation(RecordedFrame branch, List<RecordedFrame> fabricated) {
        for (RecordedFrame frame : fabricated) {
            if (frame.pos().squaredDistanceTo(branch.pos()) > 0.01D
                    || Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(frame.bodyYaw() - branch.bodyYaw())) > 2.0F
                    || Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(frame.headYaw() - branch.headYaw())) > 2.0F
                    || Math.abs(frame.pitch() - branch.pitch()) > 2.0F
                    || frame.sneaking() != branch.sneaking()
                    || frame.swimming() != branch.swimming()
                    || frame.crawling() != branch.crawling()
                    || frame.mainHandSwing() != branch.mainHandSwing()
                    || frame.offHandSwing() != branch.offHandSwing()) {
                return true;
            }
        }
        return false;
    }
}
