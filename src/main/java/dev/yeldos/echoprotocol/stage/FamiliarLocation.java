package dev.yeldos.echoprotocol.stage;

import net.minecraft.util.math.BlockPos;

public final class FamiliarLocation {
    private final FamiliarLocationType type;
    private final String dimension;
    private final int x;
    private final int y;
    private final int z;
    private int visits;
    private long lastSeenTick;

    public FamiliarLocation(FamiliarLocationType type, String dimension, BlockPos pos, int visits, long lastSeenTick) {
        this.type = type;
        this.dimension = dimension;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.visits = Math.max(1, visits);
        this.lastSeenTick = Math.max(0L, lastSeenTick);
    }

    public FamiliarLocationType type() {
        return type;
    }

    public String dimension() {
        return dimension;
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    public int visits() {
        return visits;
    }

    public long lastSeenTick() {
        return lastSeenTick;
    }

    public void markSeen(long tick) {
        visits++;
        lastSeenTick = Math.max(lastSeenTick, tick);
    }

    public boolean canMerge(FamiliarLocationType otherType, String otherDimension, BlockPos otherPos) {
        return type == otherType
                && dimension.equals(otherDimension)
                && pos().getSquaredDistance(otherPos) <= 25.0D;
    }
}
