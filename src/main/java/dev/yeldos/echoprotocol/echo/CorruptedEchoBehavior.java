package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.rendering.EchoVisualEffects;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import dev.yeldos.echoprotocol.util.EchoVisibility;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CorruptedEchoBehavior implements EchoBehaviorController {
    private final EchoEventContext context;
    private EchoState state = EchoState.REPLAYING;
    private int age;
    private int stateAge;
    private int replayCursor;
    private ItemStack wrongItem = ItemStack.EMPTY;
    private Vec3d hidingSpot;

    public CorruptedEchoBehavior(EchoEventContext context) {
        this.context = context;
    }

    @Override
    public EchoType type() {
        return EchoType.CORRUPTED;
    }

    @Override
    public EchoState state() {
        return state;
    }

    @Override
    public void onStarted(EchoEntity echo) {
        List<RecordedFrame> frames = echo.replayFrames();
        if (!frames.isEmpty()) {
            wrongItem = frames.get(ThreadLocalRandom.current().nextInt(frames.size())).heldItemVisual();
        }
    }

    @Override
    public void tick(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null) {
            echo.finishAndDiscard();
            return;
        }
        echo.setEchoState(state);
        switch (state) {
            case REPLAYING -> tickReplay(echo);
            case DESYNCHRONIZING -> tickDesync(echo, target);
            case WATCHING -> tickWatching(echo, target);
            case APPROACHING -> tickApproaching(echo, target);
            case HIDING -> tickHiding(echo, target);
            case FADING -> tickFading(echo);
            default -> transition(EchoState.FADING, echo);
        }
        age++;
        stateAge++;
    }

    private void tickReplay(EchoEntity echo) {
        replayCursor = age;
        int desyncAt = Math.min(Math.max(40, echo.replayDurationTicks() / 2), echo.replayDurationTicks() - 10);
        if (!echo.applyReplayFrame(replayCursor, false) || age >= desyncAt) {
            context.stageManager().grant(echo.getTargetPlayer(), "broken_memory");
            transition(EchoState.DESYNCHRONIZING, echo);
        }
    }

    private void tickDesync(EchoEntity echo, ServerPlayerEntity target) {
        int wrongCursor = Math.max(0, replayCursor - (stateAge % Math.max(1, echo.sampleIntervalTicks() * 8)));
        echo.applyReplayFrame(wrongCursor, true);
        if (!wrongItem.isEmpty()) {
            echo.setHeldItemVisual(wrongItem);
        }
        if (context.config().corruptionFlickerEnabled() && !context.config().reducedFlashing()) {
            float flicker = 0.32F + (float) Math.sin((echo.age + stateAge) * 0.55F) * 0.08F * context.config().corruptionVisualIntensity();
            echo.setReplayOpacity(flicker);
        }
        if (stateAge % 18 == 0) {
            EchoVisualEffects.corruptedAfterimage(target, context.config(), echo.getPos());
        }
        if (stateAge > 35) {
            context.stageManager().grant(target, "it_looked_back");
            transition(EchoState.WATCHING, echo);
        }
    }

    private void tickWatching(EchoEntity echo, ServerPlayerEntity target) {
        echo.lookAtTarget(0.2F);
        echo.setReplayOpacity(0.35F);
        boolean watched = EchoVisibility.isLookingAt(target, echo, 0.78D);
        if (watched && stateAge > 25) {
            transition(context.config().corruptedEchoCanApproach() ? EchoState.APPROACHING : EchoState.FADING, echo);
        } else if (stateAge > 60) {
            transition(context.config().corruptedEchoCanApproach() ? EchoState.APPROACHING : EchoState.FADING, echo);
        }
    }

    private void tickApproaching(EchoEntity echo, ServerPlayerEntity target) {
        echo.lookAtTarget(0.12F);
        boolean observed = EchoVisibility.isLookingAt(target, echo, 0.72D);
        if (!observed || context.config().echoMovesWhenUnobserved()) {
            Vec3d destination = target.getPos().subtract(target.getRotationVec(1.0F).multiply(2.8D));
            echo.moveToward(destination, observed ? 0.025D : 0.07D);
        }
        if (stateAge > 80 || echo.squaredDistanceTo(target) < 6.0D) {
            transition(ThreadLocalRandom.current().nextBoolean() ? EchoState.HIDING : EchoState.FADING, echo);
        }
    }

    private void tickHiding(EchoEntity echo, ServerPlayerEntity target) {
        echo.lookAtTarget(0.08F);
        if (stateAge == 1 && echo.getWorld() instanceof ServerWorld world) {
            hidingSpot = SafeEchoPositionFinder.findHidden(world, target, echo.getPos(), context.config()).orElse(null);
        }
        boolean observed = EchoVisibility.isLookingAt(target, echo, 0.70D);
        if (context.config().echoMovesWhenUnobserved() && !observed && hidingSpot != null) {
            if (echo.moveToward(hidingSpot, 0.11D)) {
                context.stageManager().grant(target, "behind_you");
            }
        }
        echo.setReplayOpacity(Math.max(0.05F, 0.35F - stateAge / 120.0F));
        if (stateAge > 70) {
            transition(EchoState.FADING, echo);
        }
    }

    private void tickFading(EchoEntity echo) {
        echo.lookAtTarget(0.05F);
        echo.setReplayOpacity(Math.max(0.0F, 0.35F - stateAge / 35.0F));
        if (stateAge > 35) {
            EchoSoundPlayer.playDisappear(echo.getTargetPlayer(), EchoType.CORRUPTED, context.config(), echo.getPos());
            EchoVisualEffects.disappear(echo.getTargetPlayer(), context.config(), echo.getPos());
            echo.finishAndDiscard();
        }
    }

    private void transition(EchoState next, EchoEntity echo) {
        state = next;
        stateAge = 0;
        echo.setEchoState(next);
    }
}
