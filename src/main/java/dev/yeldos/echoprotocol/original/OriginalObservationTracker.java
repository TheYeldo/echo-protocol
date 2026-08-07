package dev.yeldos.echoprotocol.original;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class OriginalObservationTracker {
    private OriginalObservationState state = OriginalObservationState.NOT_LOOKING;
    private OriginalObservationState rawState = OriginalObservationState.NOT_LOOKING;
    private int sameRawTicks;
    private int directlyObservedTicks;
    private int unobservedTicks;
    private double previousDistance = -1.0D;
    private boolean approaching;
    private boolean retreating;
    private boolean following;

    public void tick(EchoEntity echo, ServerPlayerEntity player, EchoConfig config, int eventAge) {
        if (eventAge % 3 != 0) {
            return;
        }
        OriginalObservationState sample = sample(echo, player);
        if (sample == rawState) {
            sameRawTicks += 3;
        } else {
            rawState = sample;
            sameRawTicks = 3;
        }
        int grace = sample == OriginalObservationState.DIRECT
                ? config.originalObservationGraceTicks() : config.originalUnobservedGraceTicks();
        if (sameRawTicks >= grace) {
            state = sample;
        }
        if (sample == OriginalObservationState.DIRECT) {
            directlyObservedTicks += 3;
            unobservedTicks = 0;
        } else {
            unobservedTicks += 3;
            directlyObservedTicks = 0;
        }

        double distance = Math.sqrt(player.squaredDistanceTo(echo));
        if (previousDistance >= 0.0D) {
            double change = distance - previousDistance;
            approaching = change < -0.12D;
            retreating = change > 0.12D;
            Vec3d toEcho = echo.getEntityPos().subtract(player.getEntityPos());
            following = approaching && distance < 10.0D && toEcho.lengthSquared() > 0.01D
                    && player.getRotationVec(1.0F).normalize().dotProduct(toEcho.normalize()) > 0.45D;
        }
        previousDistance = distance;
    }

    private static OriginalObservationState sample(EchoEntity echo, ServerPlayerEntity player) {
        Vec3d toEcho = echo.getEyePos().subtract(player.getEyePos());
        double distance = toEcho.length();
        if (distance < 0.001D) {
            return OriginalObservationState.DIRECT;
        }
        HitResult hit = player.getEntityWorld().raycast(new RaycastContext(player.getEyePos(), echo.getEyePos(),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            return OriginalObservationState.BLOCKED;
        }
        double dot = player.getRotationVec(1.0F).normalize().dotProduct(toEcho.normalize());
        if (dot >= 0.92D && distance <= 48.0D) {
            return OriginalObservationState.DIRECT;
        }
        if (dot >= 0.45D && distance <= 40.0D) {
            return OriginalObservationState.PERIPHERAL;
        }
        return OriginalObservationState.NOT_LOOKING;
    }

    public OriginalObservationState state() { return state; }
    public int directlyObservedTicks() { return directlyObservedTicks; }
    public int unobservedTicks() { return unobservedTicks; }
    public boolean approaching() { return approaching; }
    public boolean retreating() { return retreating; }
    public boolean following() { return following; }
}
