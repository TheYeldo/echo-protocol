package dev.yeldos.echoprotocol.original;

import net.minecraft.util.math.Vec3d;

public record OriginalMovementSegment(
        OriginalAction action,
        Vec3d target,
        int minimumTicks,
        int maximumTicks,
        OriginalPauseReason pauseReason,
        boolean interruptWhenApproached
) {
    public OriginalMovementSegment {
        minimumTicks = Math.max(0, minimumTicks);
        maximumTicks = Math.max(minimumTicks, maximumTicks);
    }

    public static OriginalMovementSegment move(OriginalAction action, Vec3d target, int maximumTicks) {
        return new OriginalMovementSegment(action, target, 0, maximumTicks, OriginalPauseReason.ORIENTING, false);
    }

    public static OriginalMovementSegment timed(OriginalAction action, Vec3d target, int minimumTicks, int maximumTicks,
                                                 OriginalPauseReason reason) {
        return new OriginalMovementSegment(action, target, minimumTicks, maximumTicks, reason, false);
    }

    public static OriginalMovementSegment pause(int minimumTicks, int maximumTicks, OriginalPauseReason reason,
                                                 boolean interruptWhenApproached) {
        return new OriginalMovementSegment(OriginalAction.PAUSE, null, minimumTicks, maximumTicks, reason,
                interruptWhenApproached);
    }
}
