package dev.yeldos.echoprotocol.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationProfileTest {
    @Test
    void requiresSamplesAndConfidenceBeforeClassification() {
        ObservationProfile profile = new ObservationProfile();
        profile.record(ObservationMetric.ECHO_FOLLOWED, 2.0F);
        assertEquals(ObservationStyle.UNCLASSIFIED, profile.style(6));
        for (int index = 0; index < 6; index++) {
            profile.record(ObservationMetric.ECHO_FOLLOWED, 1.0F);
            profile.recordDistance(5.0F);
        }
        assertEquals(ObservationStyle.FOLLOWER, profile.style(6));
        assertTrue(profile.confidence(6) > 0.0F);
    }

    @Test
    void countersDecayWithoutRawCameraHistory() {
        float[] counters = new float[ObservationMetric.values().length];
        counters[ObservationMetric.DIRECT_OBSERVATION.ordinal()] = 10.0F;
        ObservationProfile profile = new ObservationProfile(counters, 10, 10.0F, 5, 1L);
        assertTrue(profile.decay(1L + 20L * 60L * 60L, 0.10F));
        assertEquals(9.0F, profile.score(ObservationMetric.DIRECT_OBSERVATION), 0.001F);
    }
}
