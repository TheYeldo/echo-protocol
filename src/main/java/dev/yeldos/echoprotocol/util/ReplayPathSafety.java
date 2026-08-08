package dev.yeldos.echoprotocol.util;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

/**
 * Conservative swept collision check for recorded movement. Historical routes
 * can become blocked after recording, so checking only their next endpoint is
 * insufficient.
 */
public final class ReplayPathSafety {
    private static final double MAX_SAMPLE_DISTANCE = 0.20D;
    private static final int MAX_SAMPLES = 256;

    private ReplayPathSafety() {
    }

    public static boolean isSegmentClear(Box startBox, Vec3d movement, Predicate<Box> collisionFree) {
        if (movement.lengthSquared() <= 1.0E-10D) {
            return collisionFree.test(startBox);
        }
        int samples = Math.min(MAX_SAMPLES,
                Math.max(1, (int) Math.ceil(movement.length() / MAX_SAMPLE_DISTANCE)));
        for (int sample = 1; sample <= samples; sample++) {
            Box position = startBox.offset(movement.multiply(sample / (double) samples));
            if (!collisionFree.test(position)) {
                return false;
            }
        }
        return true;
    }
}
