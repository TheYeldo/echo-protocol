package dev.yeldos.echoprotocol.contradiction;

import dev.yeldos.echoprotocol.recording.RecordedFrame;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public record ContradictionPlan(
        ContradictionVariant variant,
        List<RecordedFrame> primaryFrames,
        List<RecordedFrame> secondaryFrames,
        Vec3d destination,
        int delayTicks,
        boolean paired,
        boolean requiresUnobservedTransition,
        long seed
) {
    public static final int MAXIMUM_FRAMES_PER_SEQUENCE = 96;
    public static final int MAXIMUM_LIFETIME_TICKS = 420;

    public ContradictionPlan {
        if (variant == null || primaryFrames == null || primaryFrames.size() < 2) {
            throw new IllegalArgumentException("a contradiction needs a variant and observable frames");
        }
        primaryFrames = List.copyOf(primaryFrames.subList(0,
                Math.min(primaryFrames.size(), MAXIMUM_FRAMES_PER_SEQUENCE)));
        secondaryFrames = secondaryFrames == null ? List.of() : List.copyOf(secondaryFrames.subList(0,
                Math.min(secondaryFrames.size(), MAXIMUM_FRAMES_PER_SEQUENCE)));
        delayTicks = Math.max(0, Math.min(160, delayTicks));
        paired = variant == ContradictionVariant.SPLIT_MEMORY && paired && secondaryFrames.size() >= 2;
    }
}
