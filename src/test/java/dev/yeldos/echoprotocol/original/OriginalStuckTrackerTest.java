package dev.yeldos.echoprotocol.original;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginalStuckTrackerTest {
    @Test
    void measuresRealWindowProgressAndResetsBetweenWindows() {
        OriginalStuckTracker tracker = new OriginalStuckTracker();
        tracker.reset(Vec3d.ZERO);

        assertFalse(tracker.tick(new Vec3d(0.1D, 0.0D, 0.0D), 2, 0.2D));
        assertTrue(tracker.tick(new Vec3d(0.15D, 0.0D, 0.0D), 2, 0.2D));
        assertFalse(tracker.tick(new Vec3d(0.45D, 0.0D, 0.0D), 2, 0.2D));
        assertFalse(tracker.tick(new Vec3d(0.75D, 0.0D, 0.0D), 2, 0.2D));
    }
}
