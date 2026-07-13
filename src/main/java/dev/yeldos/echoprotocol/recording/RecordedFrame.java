package dev.yeldos.echoprotocol.recording;

import net.minecraft.entity.EntityPose;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public record RecordedFrame(
        long serverTick,
        double x,
        double y,
        double z,
        float bodyYaw,
        float headYaw,
        float pitch,
        double velocityX,
        double velocityY,
        double velocityZ,
        boolean walking,
        boolean sprinting,
        boolean sneaking,
        boolean swimming,
        boolean crawling,
        boolean jumping,
        boolean mainHandSwing,
        boolean offHandSwing,
        int selectedHotbarSlot,
        ItemStack heldItemVisual,
        boolean onGround
) {
    public static RecordedFrame capture(ServerPlayerEntity player, long serverTick, RecordedFrame previous) {
        Vec3d velocity = player.getVelocity();
        boolean walking = Math.abs(velocity.x) + Math.abs(velocity.z) > 0.025D;
        boolean jumping = !player.isOnGround() && velocity.y > 0.08D;
        boolean mainSwing = previous != null && player.handSwinging && player.preferredHand == net.minecraft.util.Hand.MAIN_HAND;
        boolean offSwing = previous != null && player.handSwinging && player.preferredHand == net.minecraft.util.Hand.OFF_HAND;
        ItemStack held = player.getMainHandStack().copyWithCount(Math.min(1, player.getMainHandStack().getCount()));
        return new RecordedFrame(
                serverTick,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.bodyYaw,
                player.headYaw,
                player.getPitch(),
                velocity.x,
                velocity.y,
                velocity.z,
                walking,
                player.isSprinting(),
                player.isSneaking(),
                player.isSwimming(),
                player.getPose() == EntityPose.SWIMMING && !player.isSwimming(),
                jumping,
                mainSwing,
                offSwing,
                player.getInventory().selectedSlot,
                held,
                player.isOnGround()
        );
    }

    public Vec3d pos() {
        return new Vec3d(x, y, z);
    }
}
