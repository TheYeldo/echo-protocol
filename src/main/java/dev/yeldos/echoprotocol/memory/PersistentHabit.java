package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.habit.HabitType;
import net.minecraft.util.math.BlockPos;

public record PersistentHabit(
        HabitType type,
        String dimension,
        BlockPos position,
        String visualItemId,
        int observations,
        long lastSeenTick
) {
    public PersistentHabit {
        if (type == null || dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("invalid persistent habit");
        }
        position = position.toImmutable();
        visualItemId = visualItemId == null ? "" : visualItemId;
        observations = Math.max(1, observations);
        lastSeenTick = Math.max(0L, lastSeenTick);
    }

    public boolean matches(PersistentHabit other) {
        return type == other.type && dimension.equals(other.dimension)
                && position.getSquaredDistance(other.position) <= 25.0D;
    }

    public PersistentHabit reinforced(PersistentHabit other) {
        String item = other.visualItemId.isBlank() ? visualItemId : other.visualItemId;
        return new PersistentHabit(type, dimension, position, item,
                Math.min(1_000_000, observations + Math.max(1, other.observations)),
                Math.max(lastSeenTick, other.lastSeenTick));
    }
}
