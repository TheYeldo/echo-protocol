package dev.yeldos.echoprotocol.echo;

import java.util.Locale;

public enum OriginalEventKind {
    OCCUPIED_PLACE,
    ALREADY_HOME,
    YOUR_BED,
    WRONG_OWNER,
    EARLIER_THAN_YOU,
    FAMILIAR_ITEM,
    WAITING,
    EMPTY_ROOM,
    CONFRONTATION;

    public static OriginalEventKind fromCommand(String name) {
        String normalized = name.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return valueOf(normalized);
    }

    public String commandName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
