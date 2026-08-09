package dev.yeldos.echoprotocol.director;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDirectorPolicyTest {
    @Test
    void safetyLockAndGracePrecedeThreadSelection() {
        assertEquals(EventDirectorPolicy.Decision.BLOCKED_SAFETY,
                EventDirectorPolicy.decide(new EventDirectorPolicy.Inputs(false, false, true, true, true)));
        assertEquals(EventDirectorPolicy.Decision.BLOCKED_ACTIVE_LOCK,
                EventDirectorPolicy.decide(new EventDirectorPolicy.Inputs(true, true, true, true, true)));
        assertEquals(EventDirectorPolicy.Decision.BLOCKED_GRACE,
                EventDirectorPolicy.decide(new EventDirectorPolicy.Inputs(true, false, false, true, true)));
        assertEquals(EventDirectorPolicy.Decision.ATTEMPT_THREAD,
                EventDirectorPolicy.decide(new EventDirectorPolicy.Inputs(true, false, true, true, true)));
    }

    @Test
    void invalidThreadContextDoesNotStarveNormalSelectionAndFailedSpawnDoesNotLock() {
        assertEquals(EventDirectorPolicy.Decision.NORMAL_SELECTION,
                EventDirectorPolicy.decide(new EventDirectorPolicy.Inputs(true, false, true, true, false)));
        assertFalse(EventDirectorPolicy.lockAfterAttempt(false, false));
        assertTrue(EventDirectorPolicy.lockAfterAttempt(false, true));
    }

    @Test
    void failedManifestationRetriesSoonWithoutCreatingAHotLoop() {
        assertEquals(60, EventDirectorPolicy.failedAttemptRetrySeconds(480));
        assertEquals(15, EventDirectorPolicy.failedAttemptRetrySeconds(30));
        assertEquals(60, EventDirectorPolicy.failedAttemptRetrySeconds(3600));
    }

    @Test
    void unloadedOrOrphanedEntityCannotRetainTheEventLock() {
        assertTrue(EventDirectorPolicy.retainActiveEcho(false, true));
        assertFalse(EventDirectorPolicy.retainActiveEcho(true, true));
        assertFalse(EventDirectorPolicy.retainActiveEcho(false, false));
    }
}
