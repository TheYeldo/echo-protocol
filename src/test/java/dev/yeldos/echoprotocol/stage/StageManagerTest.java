package dev.yeldos.echoprotocol.stage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StageManagerTest {
    @Test
    void persistenceStoresRemainingDelayInsteadOfProcessLocalTick() {
        assertEquals(250L, StageManager.remainingDelay(10_000L, 10_250L));
        assertEquals(250L, StageManager.restoreDeadline(0L, 250L));
    }

    @Test
    void inactiveDeadlinesRemainInactiveAndExpiredOnesAreRescheduledPromptly() {
        assertEquals(0L, StageManager.remainingDelay(100L, 0L));
        assertEquals(1L, StageManager.remainingDelay(100L, 90L));
        assertEquals(0L, StageManager.restoreDeadline(500L, 0L));
    }
}
