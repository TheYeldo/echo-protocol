package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.rendering.EchoVisualEffects;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import dev.yeldos.echoprotocol.util.EchoVisibility;

public final class MemoryEchoBehavior implements EchoBehaviorController {
    private final EchoEventContext context;
    private int age;
    private EchoState state = EchoState.REPLAYING;
    private boolean observed;

    public MemoryEchoBehavior(EchoEventContext context) {
        this.context = context;
    }

    @Override
    public EchoType type() {
        return EchoType.MEMORY;
    }

    @Override
    public EchoState state() {
        return state;
    }

    @Override
    public void tick(EchoEntity echo) {
        echo.setEchoState(state);
        if (!echo.applyReplayFrame(age, false)) {
            state = EchoState.FADING;
            echo.setEchoState(state);
            EchoSoundPlayer.playDisappear(echo.getTargetPlayer(), EchoType.MEMORY, dev.yeldos.echoprotocol.EchoProtocol.config(), echo.getEntityPos());
            EchoVisualEffects.disappear(echo.getTargetPlayer(), dev.yeldos.echoprotocol.EchoProtocol.config(), echo.getEntityPos());
            echo.finishAndDiscard();
            return;
        }
        if (!observed && age >= 10 && echo.getTargetPlayer() != null
                && EchoVisibility.isLookingAt(echo.getTargetPlayer(), echo, 0.72D)) {
            observed = true;
            context.markObserved();
            if (context.awardsProgress()) {
                context.stageManager().grant(echo.getTargetPlayer(), "that_was_me");
            }
        }
        age++;
    }
}
