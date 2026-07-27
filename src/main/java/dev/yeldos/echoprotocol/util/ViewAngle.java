package dev.yeldos.echoprotocol.util;

import net.minecraft.util.math.Vec3d;

public final class ViewAngle {
    private ViewAngle() {
    }

    public static boolean outsideCentralView(Vec3d lookDirection, Vec3d toCandidate, double centralCosine) {
        if (lookDirection == null || toCandidate == null || !Double.isFinite(centralCosine)
                || lookDirection.lengthSquared() < 1.0E-8D || toCandidate.lengthSquared() < 1.0E-8D) {
            return false;
        }
        double threshold = Math.max(-1.0D, Math.min(1.0D, centralCosine));
        return lookDirection.normalize().dotProduct(toCandidate.normalize()) < threshold;
    }
}
