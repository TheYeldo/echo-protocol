package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public record EchoEventContext(
        UUID targetUuid,
        EchoConfig config,
        StageManager stageManager,
        boolean forcedHostile,
        Runnable observedCallback
) {
    public EchoEventContext(UUID targetUuid, EchoConfig config, StageManager stageManager, boolean forcedHostile) {
        this(targetUuid, config, stageManager, forcedHostile, () -> { });
    }

    public void markObserved() {
        observedCallback.run();
    }

    public ServerPlayerEntity target(net.minecraft.server.world.ServerWorld world) {
        return world.getPlayerByUuid(targetUuid) instanceof ServerPlayerEntity player ? player : null;
    }
}
