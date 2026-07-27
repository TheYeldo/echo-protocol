package dev.yeldos.echoprotocol.falsememory;

import dev.yeldos.echoprotocol.recording.RecordedFrame;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FalseMemoryPlanTest {
    @Test
    void doesNotSilentlyShortenAConfiguredAuthenticPrefix() {
        List<RecordedFrame> prefix = frames(200);
        List<RecordedFrame> fabricated = frames(24);

        FalseMemoryPlan plan = new FalseMemoryPlan(prefix, prefix.size() - 1, fabricated,
                List.of(FalseMemoryDeviation.HEAD_SHAKE), 7L, "route", false, false);

        assertEquals(200, plan.realPrefix().size());
        assertEquals(199, plan.branchPoint());
    }

    @Test
    void onlyARealVisibleStateChangeCountsAsADeviation() {
        RecordedFrame branch = frames(1).getFirst();
        assertFalse(FalseMemoryPlan.hasObservableDeviation(branch, List.of(branch)));

        RecordedFrame changedHead = new RecordedFrame(2L, branch.x(), branch.y(), branch.z(),
                branch.bodyYaw(), 35.0F, branch.pitch(), 0.0D, 0.0D, 0.0D,
                false, false, false, false, false, false, false, false,
                0, null, true);
        assertTrue(FalseMemoryPlan.hasObservableDeviation(branch, List.of(changedHead)));
    }

    private static List<RecordedFrame> frames(int count) {
        List<RecordedFrame> frames = new ArrayList<>();
        for (int tick = 0; tick < count; tick++) {
            frames.add(new RecordedFrame(tick, tick * 0.1D, 64.0D, 0.0D,
                    0.0F, 0.0F, 0.0F, 0.0D, 0.0D, 0.0D,
                    true, false, false, false, false, false, false, false,
                    0, null, true));
        }
        return frames;
    }
}
