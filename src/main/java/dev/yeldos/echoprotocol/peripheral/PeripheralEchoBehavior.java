package dev.yeldos.echoprotocol.peripheral;

import dev.yeldos.echoprotocol.echo.EchoBehaviorController;
import dev.yeldos.echoprotocol.echo.EchoEventContext;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.rendering.EchoVisualEffects;
import dev.yeldos.echoprotocol.util.EchoVisibility;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class PeripheralEchoBehavior implements EchoBehaviorController {
    private static final int OBSERVATION_GRACE_TICKS = 8;
    private final EchoEventContext context;
    private final int durationTicks;
    private final int maximumUnobservedTicks;
    private int age;
    private int directObservationTicks;
    private boolean reappeared;

    public PeripheralEchoBehavior(EchoEventContext context, int durationTicks) {
        this.context = context;
        this.durationTicks = Math.max(40, durationTicks);
        this.maximumUnobservedTicks = this.durationTicks + 60;
    }

    @Override
    public EchoType type() {
        return EchoType.MEMORY;
    }

    @Override
    public EchoState state() {
        return EchoState.WATCHING;
    }

    @Override
    public void onStarted(EchoEntity echo) {
        echo.setEchoState(EchoState.WATCHING);
        echo.setReplayOpacity(0.0F);
    }

    @Override
    public void tick(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null || !(echo.getEntityWorld() instanceof ServerWorld world)
                || !world.isChunkLoaded(echo.getBlockPos())) {
            echo.finishAndDiscard();
            return;
        }
        age++;
        echo.resetOriginalHead(1.5F, 1.0F);
        echo.setReplayOpacity(Math.min(context.config().memoryEchoOpacity(), age / 16.0F * context.config().memoryEchoOpacity()));
        boolean directlyObserved = EchoVisibility.isLookingAt(target, echo, 0.90D);
        directObservationTicks = directlyObserved ? directObservationTicks + 1 : 0;
        if (directObservationTicks >= OBSERVATION_GRACE_TICKS) {
            context.markObserved();
            if (context.awardsProgress()) {
                context.stageManager().grant(target, "out_of_the_corner_of_my_eye");
            }
            if (!reappeared && age < durationTicks / 2) {
                Vec3d next = SafeEchoPositionFinder.findPeripheral(world, target, context.config()).orElse(null);
                if (next != null) {
                    reappeared = true;
                    directObservationTicks = 0;
                    echo.refreshPositionAndAngles(next.x, next.y, next.z, echo.getYaw(), echo.getPitch());
                    return;
                }
            }
            disappear(echo, target);
            return;
        }
        if (age >= durationTicks && (context.wasObserved() || age >= maximumUnobservedTicks)) {
            disappear(echo, target);
        }
    }

    private void disappear(EchoEntity echo, ServerPlayerEntity target) {
        EchoVisualEffects.disappear(target, context.config(), echo.getEntityPos());
        echo.finishAndDiscard();
    }
}
