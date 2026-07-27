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
}
