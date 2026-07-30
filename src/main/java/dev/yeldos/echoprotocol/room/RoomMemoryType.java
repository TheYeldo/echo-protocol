package dev.yeldos.echoprotocol.room;

import dev.yeldos.echoprotocol.stage.FamiliarLocationType;

public enum RoomMemoryType {
    ENTRANCE,
    HALLWAY,
    BEDROOM,
    STORAGE,
    CRAFTING,
    FURNACE,
    PORTAL,
    IDLE,
    SHELTER,
    UNDERGROUND,
    UNKNOWN;

    public static RoomMemoryType fromFamiliarLocation(FamiliarLocationType type) {
        return switch (type) {
            case BED -> BEDROOM;
            case CHEST -> STORAGE;
            case CRAFTING -> CRAFTING;
            case FURNACE -> FURNACE;
            case DOORWAY, HOME, MINE_ENTRANCE -> ENTRANCE;
            case PORTAL -> PORTAL;
            case IDLE, MANUAL -> IDLE;
        };
    }
}
