package dev.yeldos.echoprotocol.falsememory;

import dev.yeldos.echoprotocol.echo.EchoBehaviorController;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.rendering.EchoVisualEffects;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import dev.yeldos.echoprotocol.util.EchoVisibility;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FalseMemoryBehavior implements EchoBehaviorController {
    private final FalseMemoryContext context;
    private EchoState state = EchoState.REPLAYING;
    private int age;
    private int deviationAge;
    private boolean deviationWitnessed;

    public FalseMemoryBehavior(FalseMemoryContext context) {
        this.context = context;
    }

    @Override
    public EchoType type() {
        return EchoType.FALSE_MEMORY;
    }

    @Override
    public EchoState state() {
        return state;
    }

    @Override
    public void tick(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null || !target.getServerWorld().isChunkLoaded(echo.getBlockPos())) {
            echo.finishAndDiscard();
            return;
        }
        FalseMemoryPlan plan = context.plan();
        int prefixDuration = Math.max(1, (plan.realPrefix().size() - 1) * echo.sampleIntervalTicks());
        if (state == EchoState.REPLAYING) {
            echo.setEchoState(EchoState.REPLAYING);
            if (!echo.applyFrameSequence(plan.realPrefix(), age, true)) {
                disappear(echo, target);
                return;
            }
            if (age >= prefixDuration) {
                beginDeviation(echo);
            }
        } else if (state == EchoState.DEVIATING) {
            echo.setEchoState(EchoState.DEVIATING);
            boolean applied = echo.applyFrameSequence(plan.fabricatedFrames(), deviationAge, false);
            echo.setReplayOpacity(context.config().memoryEchoOpacity());
            boolean observed = EchoVisibility.isLookingAt(target, echo, 0.82D);
            if (observed && deviationAge >= 8) {
                witnessDeviation(target, echo);
            }
            if (!applied || (observed && plan.vanishWhenObserved() && deviationAge >= 12)) {
                disappear(echo, target);
                return;
            }
            deviationAge++;
        } else {
            disappear(echo, target);
            return;
        }
        age++;
    }

    private void beginDeviation(EchoEntity echo) {
        state = EchoState.DEVIATING;
        deviationAge = 0;
        echo.setEchoState(state);
    }

    private void witnessDeviation(ServerPlayerEntity target, EchoEntity echo) {
        if (deviationWitnessed) {
            return;
        }
        deviationWitnessed = true;
        context.markObserved();
        if (!context.awardsProgress()) {
            return;
        }
        context.stageManager().grant(target, "that_never_happened");
        boolean conflict = context.history().recordAndCheckConflict(target.getUuid(), context.plan(),
                context.eventTick(), context.config().recentEventHistorySize());
        if (conflict) {
            context.stageManager().grant(target, "i_remember_it_differently");
        }
        if (context.plan().panicImprint()) {
            context.stageManager().grant(target, "almost_lost_everything");
        }
        if (context.plan().entersUnauthenticLocation()) {
            context.stageManager().grant(target, "you_were_never_there");
        }
    }

    private void disappear(EchoEntity echo, ServerPlayerEntity target) {
        state = EchoState.FADING;
        echo.setEchoState(state);
        EchoSoundPlayer.playDisappear(target, EchoType.FALSE_MEMORY, context.config(), echo.getPos());
        EchoVisualEffects.disappear(target, context.config(), echo.getPos());
        echo.finishAndDiscard();
    }
}
