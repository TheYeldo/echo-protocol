package dev.yeldos.echoprotocol.contamination;

public final class MemoryContaminationState {
    private float value;
    private long lastRecoveryTick;
    private long lastContributionId = Long.MIN_VALUE;

    public MemoryContaminationState(float value, long lastRecoveryTick, long lastContributionId) {
        this.value = clamp(value, 1.0F);
        this.lastRecoveryTick = Math.max(0L, lastRecoveryTick);
        this.lastContributionId = lastContributionId;
    }

    public MemoryContaminationState(float initial) {
        this(initial, 0L, Long.MIN_VALUE);
    }

    public boolean contribute(long contributionId, ContaminationSource source, float amount,
                              float growthMultiplier, float maximum, boolean adminAllowed) {
        if (source == ContaminationSource.ADMIN && !adminAllowed) {
            return false;
        }
        if (contributionId == lastContributionId) {
            return false;
        }
        float boundedMaximum = clampMaximum(maximum);
        float change = finite(amount) * Math.max(0.0F, finite(growthMultiplier));
        float updated = clamp(value + change, boundedMaximum);
        lastContributionId = contributionId;
        if (Math.abs(updated - value) < 0.0001F) {
            return false;
        }
        value = updated;
        return true;
    }

    public boolean recover(long currentTick, float perHour, float maximum) {
        currentTick = Math.max(0L, currentTick);
        if (lastRecoveryTick <= 0L) {
            lastRecoveryTick = currentTick;
            return false;
        }
        long elapsed = currentTick - lastRecoveryTick;
        if (elapsed < 20L * 60L) {
            return false;
        }
        float hours = elapsed / (20.0F * 60.0F * 60.0F);
        float updated = clamp(value - Math.max(0.0F, finite(perHour)) * hours, clampMaximum(maximum));
        lastRecoveryTick = currentTick;
        if (Math.abs(updated - value) < 0.0001F) {
            return false;
        }
        value = updated;
        return true;
    }

    public boolean set(float requested, float maximum) {
        float updated = clamp(requested, clampMaximum(maximum));
        if (Math.abs(updated - value) < 0.0001F) {
            return false;
        }
        value = updated;
        return true;
    }

    public boolean reduce(long contributionId, float amount, float maximum) {
        if (contributionId == lastContributionId) {
            return false;
        }
        float updated = clamp(value - Math.max(0.0F, finite(amount)), clampMaximum(maximum));
        lastContributionId = contributionId;
        if (Math.abs(updated - value) < 0.0001F) {
            return false;
        }
        value = updated;
        return true;
    }

    public float authenticMemoryMinimumWeight() {
        return Math.max(0.18F, 1.0F - value * 0.70F);
    }

    public float contradictionWeightMultiplier() {
        return 0.70F + value * 1.30F;
    }

    public float value() { return value; }
    public ContaminationTier tier() { return ContaminationTier.fromValue(value); }
    public long lastRecoveryTick() { return lastRecoveryTick; }
    public long lastContributionId() { return lastContributionId; }

    private static float clampMaximum(float maximum) {
        return Math.max(0.1F, Math.min(1.0F, finite(maximum)));
    }

    private static float clamp(float value, float maximum) {
        return Math.max(0.0F, Math.min(maximum, finite(value)));
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0.0F;
    }
}
