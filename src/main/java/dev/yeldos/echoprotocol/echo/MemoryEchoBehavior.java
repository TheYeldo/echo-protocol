package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.entity.EchoEntity;

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
            echo.finishAndDiscard();
            return;
        }
        age++;
    }
}
