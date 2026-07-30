package dev.yeldos.echoprotocol.thread;

import dev.yeldos.echoprotocol.room.RoomMemoryNode;

public record MemoryThreadContext(
        MemoryThread thread,
        MemoryThreadStep step,
        RoomMemoryNode room,
        long eventReference
) {
}
