package dev.yeldos.echoprotocol.thread;

import dev.yeldos.echoprotocol.memory.PlayerMemoryState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryThreadTest {
    private static final UUID PLAYER = UUID.fromString("1d8516bc-bff3-4694-a17c-8d942c5149f6");

    @Test
    void deterministicPlannerBuildsBoundedFeatureAwareThread() {
        MemoryThreadPlanner planner = new MemoryThreadPlanner();
        ThreadPlanningOptions options = new ThreadPlanningOptions(3, 4, true, true, false, true, true, false);

        MemoryThread first = planner.plan(PLAYER, MemoryThreadType.BEDROOM, 7L, "minecraft:overworld", "", 10L,
                99L, options).orElseThrow();
        MemoryThread second = planner.plan(PLAYER, MemoryThreadType.BEDROOM, 7L, "minecraft:overworld", "", 10L,
                99L, options).orElseThrow();

        assertEquals(first.steps(), second.steps());
        assertTrue(first.totalSteps() >= 3 && first.totalSteps() <= 4);
        assertFalse(first.steps().stream().anyMatch(step -> step.eventType() == MemoryThreadEventType.PERIPHERAL_ECHO));
        assertFalse(first.steps().stream().anyMatch(step -> step.eventType() == MemoryThreadEventType.CONTRADICTION));
    }

    @Test
    void failedEventDoesNotAdvanceButObservedOutcomeDoes() {
        MemoryThread thread = new MemoryThreadPlanner().plan(PLAYER, MemoryThreadType.BEDROOM, 1L,
                "minecraft:overworld", "", 1L, 2L,
                new ThreadPlanningOptions(2, 2, true, true, true, true, true, true)).orElseThrow();
        assertTrue(thread.eventStarted(100L, 5L));
        assertFalse(thread.applyOutcome(MemoryObservationResult.EVENT_FAILED, 0.0F));
        assertEquals(0, thread.currentStep());

        assertTrue(thread.eventStarted(101L, 6L));
        assertTrue(thread.applyOutcome(MemoryObservationResult.INVESTIGATED_SOUND, 0.04F));
        assertEquals(1, thread.currentStep());
    }

    @Test
    void threadCancelsAndResumesSafelyAndHistoryRemainsBounded() {
        PlayerMemoryState state = new PlayerMemoryState();
        for (int index = 0; index < 12; index++) {
            MemoryThread thread = new MemoryThreadPlanner().plan(PLAYER, MemoryThreadType.values()[index % 9], 0L,
                    "minecraft:overworld", "", index, index,
                    new ThreadPlanningOptions(2, 2, true, true, true, true, true, true)).orElseThrow();
            state.setActiveThread(thread);
            for (MemoryThreadStep step : thread.steps()) {
                state.threadEventStarted(index * 10L + thread.currentStep(), index);
                MemoryObservationResult outcome = step.eventType() == MemoryThreadEventType.AUDIO_RESIDUE
                        ? MemoryObservationResult.INVESTIGATED_SOUND : MemoryObservationResult.DIRECT;
                assertTrue(state.applyThreadOutcome(outcome, 0.01F));
            }
            state.clearCompletedThread();
        }
        assertEquals(8, state.recentCompletedThreads().size());

        MemoryThread resumable = new MemoryThreadPlanner().plan(PLAYER, MemoryThreadType.STORAGE, 2L,
                "minecraft:overworld", "", 20L, 3L,
                new ThreadPlanningOptions(2, 3, true, true, true, true, true, true)).orElseThrow();
        resumable.pause();
        resumable.resume(true);
        assertEquals(MemoryThreadStage.ACTIVE, resumable.stage());
        assertTrue(resumable.resumedAfterRestart());
        resumable.cancel();
        assertEquals(MemoryThreadStage.CANCELLED, resumable.stage());
    }

    @Test
    void plannerCancelsWhenFeatureTogglesCannotProduceMinimumSteps() {
        ThreadPlanningOptions none = new ThreadPlanningOptions(2, 4, false, false, false, false, false, false);
        assertTrue(new MemoryThreadPlanner().plan(PLAYER, MemoryThreadType.BEDROOM, 0L, "", "", 0L, 0L, none).isEmpty());
    }

    @Test
    void repeatedMissesRemainBoundedAndDoNotResetWhenAnEventStarts() {
        MemoryThread thread = new MemoryThreadPlanner().plan(PLAYER, MemoryThreadType.BEDROOM, 1L,
                "minecraft:overworld", "", 1L, 2L,
                new ThreadPlanningOptions(2, 2, true, true, true, true, true, true)).orElseThrow();

        for (int attempt = 0; attempt < 3; attempt++) {
            assertTrue(thread.eventStarted(200L + attempt, 20L + attempt));
            assertFalse(thread.applyOutcome(MemoryObservationResult.MISSED, 0.0F));
        }

        assertEquals(0, thread.currentStep());
        assertEquals(3, thread.consecutiveFailures());
    }
}
