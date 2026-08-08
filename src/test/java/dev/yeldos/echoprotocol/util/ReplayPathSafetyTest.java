package dev.yeldos.echoprotocol.util;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayPathSafetyTest {
    private static final Box ENTITY = new Box(-0.3D, 0.0D, -0.3D, 0.3D, 1.8D, 0.3D);
    private static final Box WALL = new Box(-1.0D, 0.0D, 1.5D, 1.0D, 3.0D, 1.7D);

    @Test
    void rejectsSegmentThatCrossesWallEvenWhenEndpointIsClear() {
        assertFalse(ReplayPathSafety.isSegmentClear(ENTITY, new Vec3d(0.0D, 0.0D, 3.0D),
                candidate -> !candidate.intersects(WALL)));
    }

    @Test
    void acceptsUnobstructedRecordedSegment() {
        assertTrue(ReplayPathSafety.isSegmentClear(ENTITY, new Vec3d(3.0D, 0.0D, 0.0D),
                candidate -> !candidate.intersects(WALL)));
    }
}
