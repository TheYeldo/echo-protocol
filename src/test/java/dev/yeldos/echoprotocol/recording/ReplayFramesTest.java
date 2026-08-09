package dev.yeldos.echoprotocol.recording;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplayFramesTest {
    @Test
    void translatesTheWholeRouteToTheValidatedSpawnWithoutChangingShape() {
        RecordedFrame first = PlayerRecordingTest.frame(10);
        RecordedFrame second = PlayerRecordingTest.frame(14);

        List<RecordedFrame> translated = ReplayFrames.translated(List.of(first, second), new Vec3d(2.0D, 70.0D, 3.0D));

        assertEquals(new Vec3d(2.0D, 70.0D, 3.0D), translated.getFirst().pos());
        assertEquals(second.pos().subtract(first.pos()), translated.getLast().pos().subtract(translated.getFirst().pos()));
    }

    @Test
    void liveMimicMotionIsRelativeToItsManifestationInsteadOfTeleportingOntoThePlayer() {
        net.minecraft.util.math.Vec3d playerOrigin = new net.minecraft.util.math.Vec3d(100.0D, 64.0D, 100.0D);
        net.minecraft.util.math.Vec3d mimicOrigin = new net.minecraft.util.math.Vec3d(92.0D, 64.0D, 96.0D);
        net.minecraft.util.math.Vec3d laterPlayer = new net.minecraft.util.math.Vec3d(102.0D, 64.0D, 99.0D);

        assertEquals(new net.minecraft.util.math.Vec3d(94.0D, 64.0D, 95.0D),
                ReplayFrames.relativePosition(laterPlayer, playerOrigin, mimicOrigin));
    }
}
