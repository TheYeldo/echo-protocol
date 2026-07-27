package dev.yeldos.echoprotocol.panic;

import dev.yeldos.echoprotocol.recording.RecordedFrame;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PanicImprintTest {
    @Test
    void keepsTheCompleteBoundedEightSecondCaptureAtFastestSampling() {
        List<RecordedFrame> frames = new ArrayList<>();
        for (int tick = 0; tick < 160; tick++) {
            frames.add(frame(tick));
        }

        PanicImprint imprint = new PanicImprint(frames, "minecraft:overworld",
                PanicImprint.HealthCategory.LOW, PanicTriggerType.LOW_HEALTH, 160L);

        assertEquals(160, imprint.frames().size());
        assertEquals(0L, imprint.frames().getFirst().serverTick());
        assertEquals(159L, imprint.frames().getLast().serverTick());
    }

    private static RecordedFrame frame(long tick) {
        return new RecordedFrame(tick, tick, 64.0D, tick, 0.0F, 0.0F, 0.0F,
                0.0D, 0.0D, 0.0D, true, false, false, false, false,
                false, false, false, 0, null, true);
    }
}
