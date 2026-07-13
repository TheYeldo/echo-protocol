package dev.yeldos.echoprotocol.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class EchoVisibility {
    private EchoVisibility() {
    }

    public static boolean hasLineOfSight(ServerPlayerEntity player, Entity entity) {
        return player.canSee(entity);
    }

    public static boolean isLookingAt(ServerPlayerEntity player, Entity entity, double cosineThreshold) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toEcho = entity.getEyePos().subtract(player.getEyePos());
        if (toEcho.lengthSquared() < 1.0E-5D) {
            return true;
        }
        return look.dotProduct(toEcho.normalize()) >= cosineThreshold && hasLineOfSight(player, entity);
    }

    public static boolean isBehindPlayer(ServerPlayerEntity player, Entity entity) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toEcho = entity.getEyePos().subtract(player.getEyePos());
        if (toEcho.lengthSquared() < 1.0E-5D) {
            return false;
        }
        return look.dotProduct(toEcho.normalize()) < -0.15D;
    }
}
