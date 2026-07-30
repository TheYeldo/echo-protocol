package dev.yeldos.echoprotocol.contradiction;

import java.util.Locale;

public enum ContradictionVariant {
    SPLIT_MEMORY,
    REPEATED_ENDING,
    WRONG_DESTINATION,
    MEMORY_ARRIVED_FIRST,
    CONFLICTING_ITEM,
    MISSING_SEGMENT,
    CONFLICTING_COPIES;

    public static ContradictionVariant fromCommand(String name) {
        return valueOf(name.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }

    public String commandName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
