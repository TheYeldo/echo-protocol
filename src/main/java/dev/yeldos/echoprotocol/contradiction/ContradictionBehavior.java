package dev.yeldos.echoprotocol.contradiction;

import dev.yeldos.echoprotocol.echo.EchoBehaviorController;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.util.EchoVisibility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class ContradictionBehavior implements EchoBehaviorController {
    private final ContradictionPlan plan;
    private final boolean pairedSecondary;
    private final ContradictionGroup group;
    private final Runnable observedCallback;
    private EchoState state = EchoState.REPLAYING;
    private int age;
    private int sequenceAge;
    private int delayAge;
    private int observationTicks;
    private int unobservedTicks;
    private boolean secondSequence;
    private boolean observationReported;

    public ContradictionBehavior(ContradictionPlan plan, boolean pairedSecondary, ContradictionGroup group,
                                 Runnable observedCallback) {
        this.plan = plan;
        this.pairedSecondary = pairedSecondary;
        this.group = group;
        this.observedCallback = observedCallback == null ? () -> { } : observedCallback;
    }

    @Override public EchoType type() { return EchoType.FALSE_MEMORY; }
    @Override public EchoState state() { return state; }

    @Override
    public void tick(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null || age++ >= ContradictionPlan.MAXIMUM_LIFETIME_TICKS
                || !target.getServerWorld().isChunkLoaded(echo.getBlockPos())) {
            finish(echo);
            return;
        }
        boolean looking = EchoVisibility.isLookingAt(target, echo, 0.82D);
        observationTicks = looking ? observationTicks + 1 : 0;
        unobservedTicks = looking ? 0 : unobservedTicks + 1;
        if (!observationReported && observationTicks >= 8) {
            observationReported = true;
            observedCallback.run();
        }

        List<RecordedFrame> frames = pairedSecondary ? plan.secondaryFrames()
                : secondSequence ? plan.secondaryFrames() : plan.primaryFrames();
        if (state == EchoState.FADING) {
            echo.setReplayOpacity(0.0F);
            delayAge++;
            boolean transitionAllowed = !plan.requiresUnobservedTransition() || unobservedTicks >= 8;
            if (delayAge >= plan.delayTicks() && transitionAllowed) {
                if (plan.secondaryFrames().isEmpty()) {
                    finish(echo);
                    return;
                }
                secondSequence = true;
                sequenceAge = 0;
                state = EchoState.DEVIATING;
                echo.applyFrame(plan.secondaryFrames().getFirst());
                echo.setEchoState(state);
            }
            return;
        }
        echo.setEchoState(secondSequence || pairedSecondary ? EchoState.DEVIATING : EchoState.REPLAYING);
        boolean applied = echo.applyFrameSequence(frames, sequenceAge++, true);
        if (applied) {
            return;
        }
        if (!pairedSecondary && !secondSequence && !plan.secondaryFrames().isEmpty()) {
            state = EchoState.FADING;
            delayAge = 0;
            echo.setEchoState(state);
            echo.setReplayOpacity(0.0F);
        } else {
            finish(echo);
        }
    }

    @Override
    public void onTargetUnavailable(EchoEntity echo) {
        finish(echo);
    }

    private void finish(EchoEntity echo) {
        if (group != null) {
            group.finishAll();
        } else if (!echo.isRemoved()) {
            echo.finishAndDiscard();
        }
    }
}
