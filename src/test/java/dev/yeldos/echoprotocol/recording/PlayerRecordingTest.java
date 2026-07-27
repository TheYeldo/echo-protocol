package dev.yeldos.echoprotocol.recording;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRecordingTest {
    @Test
    void emptyAndSingleFrameRecordingsAreHonest() {
        PlayerRecording recording = new PlayerRecording();

        assertEquals(0, recording.size());
        assertTrue(recording.randomSegment(2, 10).isEmpty());

        recording.add(frame(1));
        assertEquals(1, recording.size());
        assertTrue(recording.randomSegment(2, 10).isEmpty());
    }

    @Test
    void ringBufferKeepsNewestFramesInOrder() {
        PlayerRecording recording = new PlayerRecording();
        recording.resize(20);

        for (int tick = 0; tick < 25; tick++) {
            recording.add(frame(tick));
        }

        List<RecordedFrame> frames = recording.frames();
        assertEquals(20, frames.size());
        assertEquals(5, frames.getFirst().serverTick());
        assertEquals(24, frames.getLast().serverTick());
    }

    static RecordedFrame frame(long tick) {
        return new RecordedFrame(tick, tick, 64.0D, tick, 0.0F, 0.0F, 0.0F,
                0.0D, 0.0D, 0.0D, true, false, false, false, false,
                false, false, false, 0, null, true);
    }
}
