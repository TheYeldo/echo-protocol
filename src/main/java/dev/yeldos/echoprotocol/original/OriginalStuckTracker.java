package dev.yeldos.echoprotocol.original;

import net.minecraft.util.math.Vec3d;

public final class OriginalStuckTracker {
    private Vec3d windowStart;
    private int activeTicks;
    private int stuckCount;

    public void reset(Vec3d position) {
        windowStart = position;
        activeTicks = 0;
    }

    public boolean tick(Vec3d position, int windowTicks, double minimumProgress) {
        if (windowStart == null) {
            reset(position);
            return false;
        }
        activeTicks++;
        if (activeTicks < windowTicks) {
            return false;
        }
        boolean stuck = horizontalDistance(position, windowStart) < minimumProgress;
        reset(position);
        if (stuck) {
            stuckCount++;
        }
        return stuck;
    }

    public int stuckCount() {
        return stuckCount;
    }

    private static double horizontalDistance(Vec3d left, Vec3d right) {
        double x = left.x - right.x;
        double z = left.z - right.z;
        return Math.sqrt(x * x + z * z);
    }
}
