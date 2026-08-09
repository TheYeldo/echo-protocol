package dev.yeldos.echoprotocol.director;

public final class EventDirectorPolicy {
    public enum Decision {
        BLOCKED_SAFETY,
        BLOCKED_ACTIVE_LOCK,
        BLOCKED_GRACE,
        ATTEMPT_THREAD,
        NORMAL_SELECTION
    }

    public record Inputs(boolean hardSafetyPassed, boolean activeEventLocked, boolean gracePassed,
                         boolean activeThread, boolean eligibleThreadStep) {
    }

    private EventDirectorPolicy() {
    }

    public static Decision decide(Inputs inputs) {
        if (inputs == null || !inputs.hardSafetyPassed()) return Decision.BLOCKED_SAFETY;
        if (inputs.activeEventLocked()) return Decision.BLOCKED_ACTIVE_LOCK;
        if (!inputs.gracePassed()) return Decision.BLOCKED_GRACE;
        if (inputs.activeThread() && inputs.eligibleThreadStep()) return Decision.ATTEMPT_THREAD;
        return Decision.NORMAL_SELECTION;
    }

    public static boolean lockAfterAttempt(boolean lockBeforeAttempt, boolean eventActuallyStarted) {
        return lockBeforeAttempt || eventActuallyStarted;
    }

    /**
     * A failed manifestation must not consume the full normal event interval. The retry remains bounded so a bad
     * location cannot create a hot loop, while the default 8-18 minute pacing is not restarted after every rejected
     * spawn.
     */
    public static int failedAttemptRetrySeconds(int minimumEventIntervalSeconds) {
        return Math.max(15, Math.min(60, Math.max(1, minimumEventIntervalSeconds) / 4));
    }

    public static boolean retainActiveEcho(boolean removed, boolean trackedInWorld) {
        return !removed && trackedInWorld;
    }
}
