package dev.yeldos.echoprotocol.privacy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoPrivacyTest {
    @Test
    void privateRecipientsAreTargetOnlyAndSharedVisualsNeverShareMetadata() {
        UUID first = UUID.fromString("74012643-8dfd-4ae8-aeb0-b1600c493709");
        UUID second = UUID.fromString("a675ba73-a0fd-47fa-b395-b33309225ac6");

        assertTrue(EchoPrivacy.mayReceiveVisual(false, first, first));
        assertFalse(EchoPrivacy.mayReceiveVisual(false, first, second));
        assertTrue(EchoPrivacy.mayReceiveVisual(true, first, second));
        assertTrue(EchoPrivacy.mayReceivePrivateMetadata(first, first));
        assertFalse(EchoPrivacy.mayReceivePrivateMetadata(first, second));
    }
}
