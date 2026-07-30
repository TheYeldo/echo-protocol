package dev.yeldos.echoprotocol.thread;

import dev.yeldos.echoprotocol.room.RoomMemoryType;

import java.util.Locale;

public enum MemoryThreadType {
    BEDROOM,
    STORAGE,
    ENTRANCE,
    PORTAL,
    PANIC,
    MISSING_ROUTE,
    EMPTY_ROOM,
    FOLLOWED_ECHO,
    IGNORED_SOUND;

    public static MemoryThreadType fromCommand(String name) {
        return valueOf(name.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }

    public static MemoryThreadType forRoom(RoomMemoryType type) {
        return switch (type) {
            case BEDROOM -> BEDROOM;
            case STORAGE -> STORAGE;
            case ENTRANCE, HALLWAY, SHELTER -> ENTRANCE;
            case PORTAL -> PORTAL;
            default -> EMPTY_ROOM;
        };
    }

    public String commandName() { return name().toLowerCase(Locale.ROOT); }
}
