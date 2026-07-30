package dev.yeldos.echoprotocol.contamination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryContaminationStateTest {
    @Test
    void contributionIsClampedAndCannotBeFarmedFromOneEvent() {
        MemoryContaminationState state = new MemoryContaminationState(0.0F);
        assertTrue(state.contribute(1L, ContaminationSource.FALSE_MEMORY_OBSERVED, 0.7F, 2.0F, 1.0F, false));
        assertEquals(1.0F, state.value(), 0.0001F);
        assertFalse(state.contribute(1L, ContaminationSource.FALSE_MEMORY_OBSERVED, 0.2F, 1.0F, 1.0F, false));
        assertFalse(state.contribute(2L, ContaminationSource.ADMIN, -0.5F, 1.0F, 1.0F, false));
    }

    @Test
    void decayUsesMeaningfulElapsedIntervals() {
        MemoryContaminationState state = new MemoryContaminationState(0.8F, 1L, Long.MIN_VALUE);
        assertFalse(state.recover(100L, 0.1F, 1.0F));
        long oneHourLater = 1L + 20L * 60L * 60L;
        assertTrue(state.recover(oneHourLater, 0.1F, 1.0F));
        assertEquals(0.7F, state.value(), 0.001F);
    }

    @Test
    void authenticMemoriesRemainAvailableAtMaximumContamination() {
        MemoryContaminationState state = new MemoryContaminationState(1.0F);
        assertTrue(state.authenticMemoryMinimumWeight() > 0.0F);
        assertTrue(state.reduce(9L, 0.15F, 1.0F));
        assertEquals(0.85F, state.value(), 0.0001F);
    }
}
