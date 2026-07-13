package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public interface EchoBehaviorController {
    EchoType type();

    EchoState state();

    void tick(EchoEntity echo);

    default void onStarted(EchoEntity echo) {
    }

    default void onTargetUnavailable(EchoEntity echo) {
        echo.discard();
    }

    default boolean isHostile() {
        return false;
    }

    default void forceHostile(boolean hostile) {
    }

    default void onTouchedTarget(EchoEntity echo, ServerPlayerEntity target) {
    }
}
