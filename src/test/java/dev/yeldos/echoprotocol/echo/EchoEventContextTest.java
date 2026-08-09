package dev.yeldos.echoprotocol.echo;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoEventContextTest {
    @Test
    void manifestationRemainsUnobservedUntilTheBehaviorConfirmsIt() {
        AtomicInteger callbackCount = new AtomicInteger();
        EchoEventContext context = new EchoEventContext(UUID.randomUUID(), null, null, false, true,
                callbackCount::incrementAndGet);

        assertFalse(context.wasObserved());
        context.markObserved();
        assertTrue(context.wasObserved());
        assertEquals(1, callbackCount.get());
    }
}
