package dev.yeldos.echoprotocol.profile;

import java.util.Arrays;

public final class ObservationProfile {
    public static final float MAXIMUM_COUNTER = 100.0F;

    private final float[] counters = new float[ObservationMetric.values().length];
    private int samples;
    private float preferredDistance;
    private int distanceSamples;
    private long lastDecayTick;

    public ObservationProfile() {
    }

    public ObservationProfile(float[] restoredCounters, int samples, float preferredDistance,
                              int distanceSamples, long lastDecayTick) {
        if (restoredCounters != null) {
            for (int index = 0; index < Math.min(counters.length, restoredCounters.length); index++) {
                counters[index] = clamp(restoredCounters[index]);
            }
        }
        this.samples = Math.max(0, Math.min(1_000_000, samples));
        this.preferredDistance = finiteDistance(preferredDistance);
        this.distanceSamples = Math.max(0, Math.min(1_000_000, distanceSamples));
        this.lastDecayTick = Math.max(0L, lastDecayTick);
    }

    public void record(ObservationMetric metric, float amount) {
        if (metric == null) {
            return;
        }
        counters[metric.ordinal()] = clamp(counters[metric.ordinal()] + Math.max(0.0F, finite(amount)));
        samples = Math.min(1_000_000, samples + 1);
    }

    public void recordDistance(float distance) {
        float bounded = finiteDistance(distance);
        preferredDistance = distanceSamples == 0 ? bounded
                : preferredDistance + (bounded - preferredDistance) / Math.min(64, distanceSamples + 1);
        distanceSamples = Math.min(1_000_000, distanceSamples + 1);
    }

    public boolean decay(long currentTick, float perHour) {
        currentTick = Math.max(0L, currentTick);
        if (lastDecayTick <= 0L) {
            lastDecayTick = currentTick;
            return false;
        }
        long elapsed = currentTick - lastDecayTick;
        if (elapsed < 20L * 60L) {
            return false;
        }
        float hours = elapsed / (20.0F * 60.0F * 60.0F);
        float factor = Math.max(0.0F, 1.0F - Math.max(0.0F, finite(perHour)) * hours);
        boolean changed = false;
        for (int index = 0; index < counters.length; index++) {
            float updated = counters[index] * factor;
            changed |= Math.abs(updated - counters[index]) >= 0.0001F;
            counters[index] = updated;
        }
        samples = Math.max(0, Math.round(samples * factor));
        distanceSamples = Math.max(0, Math.round(distanceSamples * factor));
        lastDecayTick = currentTick;
        return changed;
    }

    public ObservationStyle style(int minimumSamples) {
        if (samples < Math.max(3, minimumSamples)) {
            return ObservationStyle.UNCLASSIFIED;
        }
        float follower = score(ObservationMetric.ECHO_FOLLOWED) + score(ObservationMetric.APPROACH) * 0.7F;
        float avoidant = score(ObservationMetric.ECHO_AVOIDED) + score(ObservationMetric.RETREAT) * 0.7F;
        float observer = score(ObservationMetric.DIRECT_OBSERVATION) + score(ObservationMetric.RAPID_TURNAROUND) * 0.2F;
        float investigator = score(ObservationMetric.SOUND_INVESTIGATED) + score(ObservationMetric.ROOM_RECHECKED) * 0.8F;
        float distant = preferredDistance >= 14.0F && distanceSamples >= 3 ? 2.0F + preferredDistance / 12.0F : 0.0F;
        float[] scores = {follower, avoidant, observer, investigator, distant};
        int best = 0;
        int second = 1;
        for (int index = 1; index < scores.length; index++) {
            if (scores[index] > scores[best]) {
                second = best;
                best = index;
            } else if (index == best || scores[index] > scores[second]) {
                second = index;
            }
        }
        if (scores[best] < 2.0F || scores[best] - scores[second] < 0.75F) {
            return ObservationStyle.UNCLASSIFIED;
        }
        return switch (best) {
            case 0 -> ObservationStyle.FOLLOWER;
            case 1 -> ObservationStyle.AVOIDANT;
            case 2 -> ObservationStyle.OBSERVER;
            case 3 -> ObservationStyle.INVESTIGATOR;
            default -> ObservationStyle.DISTANT;
        };
    }

    public float confidence(int minimumSamples) {
        if (style(minimumSamples) == ObservationStyle.UNCLASSIFIED) {
            return 0.0F;
        }
        return Math.min(1.0F, samples / (float) Math.max(minimumSamples * 3, 1));
    }

    public float score(ObservationMetric metric) { return counters[metric.ordinal()]; }
    public float[] counters() { return Arrays.copyOf(counters, counters.length); }
    public int samples() { return samples; }
    public float preferredDistance() { return preferredDistance; }
    public int distanceSamples() { return distanceSamples; }
    public long lastDecayTick() { return lastDecayTick; }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(MAXIMUM_COUNTER, finite(value)));
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0.0F;
    }

    private static float finiteDistance(float value) {
        return Math.max(0.0F, Math.min(64.0F, finite(value)));
    }
}
