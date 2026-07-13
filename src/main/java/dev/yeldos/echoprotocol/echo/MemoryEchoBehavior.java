package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.rendering.EchoVisualEffects;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;

public final class MemoryEchoBehavior implements EchoBehaviorController {
    private int age;
    private EchoState state = EchoState.REPLAYING;

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
            EchoSoundPlayer.playDisappear(echo.getTargetPlayer(), EchoType.MEMORY, dev.yeldos.echoprotocol.EchoProtocol.config(), echo.getPos());
            EchoVisualEffects.disappear(echo.getTargetPlayer(), dev.yeldos.echoprotocol.EchoProtocol.config(), echo.getPos());
            echo.finishAndDiscard();
            return;
        }
        age++;
    }
}
