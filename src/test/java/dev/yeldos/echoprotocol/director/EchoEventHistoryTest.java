package dev.yeldos.echoprotocol.director;

import dev.yeldos.echoprotocol.config.EchoConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoEventHistoryTest {
    @Test
    void historyIsBoundedAndStrongSilenceIsActuallyEnforced() {
        EchoConfig config = EchoConfig.defaults();
        EchoEventHistory history = new EchoEventHistory();
        UUID player = UUID.randomUUID();

        for (int index = 0; index < config.recentEventHistorySize() + 5; index++) {
            history.record(player, index == 0 ? EventCategory.MIMIC : EventCategory.MEMORY,
                    index * 100L, true, config);
        }

        assertEquals(config.recentEventHistorySize(), history.entries(player).size());
        assertTrue(history.strongSilenceActive(player, 1000L, config));
        assertFalse(history.strongSilenceActive(player,
                (long) config.strongEventSilenceMinutes() * 60L * 20L + 1L, config));
    }

    @Test
    void selectorPreventsAnImmediateDuplicateWhenAnotherCandidateExists() {
        EchoConfig config = EchoConfig.defaults();
        EchoEventHistory history = new EchoEventHistory();
        UUID player = UUID.randomUUID();
        history.record(player, EventCategory.MEMORY, 10L, true, config);

        EventCategory selected = new AdaptiveEventSelector().select(player, 20L, List.of(
                new AdaptiveEventSelector.Candidate(EventCategory.MEMORY, 100),
                new AdaptiveEventSelector.Candidate(EventCategory.CORRUPTED, 1)), history, config);

        assertEquals(EventCategory.CORRUPTED, selected);
    }
}
