package dev.yeldos.echoprotocol.original;

import java.util.Locale;

public enum OriginalMovementMode {
    WALK,
    FAST_WALK,
    APPROACH,
    RETREAT,
    PATROL,
    DOORWAY,
    BED;

    public static OriginalMovementMode fromCommand(String value) {
        return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }

    public String commandName() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
