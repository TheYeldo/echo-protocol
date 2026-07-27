package dev.yeldos.echoprotocol.util;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewAngleTest {
    @Test
    void classifiesCentralAndPeripheralDirectionsDeterministically() {
        Vec3d forward = new Vec3d(0.0D, 0.0D, 1.0D);

        assertFalse(ViewAngle.outsideCentralView(forward, new Vec3d(0.0D, 0.0D, 8.0D), 0.65D));
        assertTrue(ViewAngle.outsideCentralView(forward, new Vec3d(8.0D, 0.0D, 1.0D), 0.65D));
        assertTrue(ViewAngle.outsideCentralView(forward, new Vec3d(0.0D, 0.0D, -8.0D), 0.65D));
    }

    @Test
    void rejectsDegenerateGeometryInsteadOfProducingAnAccidentalMatch() {
        assertFalse(ViewAngle.outsideCentralView(Vec3d.ZERO, new Vec3d(1.0D, 0.0D, 0.0D), 0.65D));
        assertFalse(ViewAngle.outsideCentralView(new Vec3d(0.0D, 0.0D, 1.0D), Vec3d.ZERO, 0.65D));
        assertFalse(ViewAngle.outsideCentralView(new Vec3d(0.0D, 0.0D, 1.0D),
                new Vec3d(1.0D, 0.0D, 0.0D), Double.NaN));
    }
}
