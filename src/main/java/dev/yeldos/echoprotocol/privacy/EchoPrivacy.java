package dev.yeldos.echoprotocol.privacy;

import java.util.UUID;

public final class EchoPrivacy {
    private EchoPrivacy() {
    }

    public static boolean mayReceiveVisual(boolean sharedEchoes, UUID target, UUID recipient) {
        return target != null && recipient != null && (sharedEchoes || target.equals(recipient));
    }

    public static boolean mayReceivePrivateMetadata(UUID target, UUID recipient) {
        return target != null && target.equals(recipient);
    }
}
