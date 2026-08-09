package dev.yeldos.echoprotocol.recording;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaySegmentSelectorTest {
    @Test
    void prefersARealMovementWindowOverLongIdleHistory() {
        List<RecordedFrame> frames = new ArrayList<>();
        for (int index = 0; index < 160; index++) {
            frames.add(frame(index, 0.0D, false));
        }
        for (int index = 160; index < 240; index++) {
            frames.add(frame(index, (index - 160) * 0.12D, true));
        }

        List<RecordedFrame> selected = ReplaySegmentSelector.select(frames, 50, 100, new Random(7L));

        assertTrue(pathDistance(selected) >= 1.5D);
        assertTrue(selected.stream().anyMatch(RecordedFrame::walking));
    }

    @Test
    void neverSelectsAcrossATeleportBoundary() {
        List<RecordedFrame> frames = new ArrayList<>();
        for (int index = 0; index < 70; index++) {
            frames.add(frame(index, index * 0.1D, true));
        }
        for (int index = 70; index < 140; index++) {
            frames.add(frame(index, 100.0D + (index - 70) * 0.1D, true));
        }

        List<RecordedFrame> selected = ReplaySegmentSelector.select(frames, 40, 60, new Random(11L));

        for (int index = 1; index < selected.size(); index++) {
            assertTrue(selected.get(index - 1).pos().distanceTo(selected.get(index).pos()) <= 2.5D);
        }
    }

    @Test
    void preservesAnAuthenticIdleMemoryWhenNoMovementExists() {
        List<RecordedFrame> frames = new ArrayList<>();
        for (int index = 0; index < 80; index++) {
            frames.add(frame(index, 4.0D, false));
        }

        List<RecordedFrame> selected = ReplaySegmentSelector.select(frames, 40, 60, new Random(3L));

        assertTrue(selected.size() >= 40);
        assertEquals(0.0D, pathDistance(selected), 1.0E-9D);
    }

    private static RecordedFrame frame(long tick, double x, boolean walking) {
        return new RecordedFrame(tick, x, 64.0D, 0.0D, 0.0F, 0.0F, 0.0F,
                walking ? 0.1D : 0.0D, 0.0D, 0.0D, walking, false, false, false, false,
                false, false, false, 0, null, true);
    }

    private static double pathDistance(List<RecordedFrame> frames) {
        double distance = 0.0D;
        for (int index = 1; index < frames.size(); index++) {
            distance += frames.get(index - 1).pos().distanceTo(frames.get(index).pos());
        }
        return distance;
    }
}
