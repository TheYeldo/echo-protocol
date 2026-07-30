package dev.yeldos.echoprotocol.contradiction;

import dev.yeldos.echoprotocol.recording.RecordedFrame;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContradictionPlannerTest {
    @Test
    void seededPlansAreDeterministicAndBoundedForEveryVariant() {
        ContradictionPlanner planner = new ContradictionPlanner();
        for (ContradictionVariant variant : ContradictionVariant.values()) {
            ContradictionPlan first = planner.plan(variant, frames(80), new Vec3d(8.0D, 64.0D, 3.0D),
                    null, 91L).orElseThrow();
            ContradictionPlan second = planner.plan(variant, frames(80), new Vec3d(8.0D, 64.0D, 3.0D),
                    null, 91L).orElseThrow();
            assertEquals(first, second);
            assertTrue(first.primaryFrames().size() <= ContradictionPlan.MAXIMUM_FRAMES_PER_SEQUENCE);
            assertTrue(first.secondaryFrames().size() <= ContradictionPlan.MAXIMUM_FRAMES_PER_SEQUENCE);
        }
    }

    @Test
    void splitMemoryDivergesAfterAnAuthenticSharedPrefix() {
        ContradictionPlan plan = new ContradictionPlanner().plan(ContradictionVariant.SPLIT_MEMORY, frames(60),
                null, null, 4L).orElseThrow();
        assertTrue(plan.paired());
        assertEquals(plan.primaryFrames().get(10).pos(), plan.secondaryFrames().get(10).pos());
        assertFalse(plan.primaryFrames().getLast().pos().equals(plan.secondaryFrames().getLast().pos()));
    }

    @Test
    void splitCleanupRemovesBothParticipantsExactlyOnce() {
        ContradictionGroup group = new ContradictionGroup();
        AtomicInteger cleaned = new AtomicInteger();
        group.register(cleaned::incrementAndGet);
        group.register(cleaned::incrementAndGet);

        group.finishAll();
        group.finishAll();

        assertEquals(2, cleaned.get());
        assertTrue(group.finished());
    }

    private static List<RecordedFrame> frames(int count) {
        List<RecordedFrame> result = new ArrayList<>();
        for (int tick = 0; tick < count; tick++) {
            result.add(new RecordedFrame(tick, tick * 0.1D, 64.0D, 0.0D,
                    0.0F, 0.0F, 0.0F, 0.1D, 0.0D, 0.0D,
                    true, false, false, false, false, false, false, false,
                    0, null, true));
        }
        return result;
    }
}
