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
        boolean awardsProgress,
        Runnable observedCallback
) {
    public EchoEventContext(UUID targetUuid, EchoConfig config, StageManager stageManager, boolean forcedHostile) {
        this(targetUuid, config, stageManager, forcedHostile, true, () -> { });
    }

    public EchoEventContext(UUID targetUuid, EchoConfig config, StageManager stageManager, boolean forcedHostile,
                            boolean awardsProgress) {
        this(targetUuid, config, stageManager, forcedHostile, awardsProgress, () -> { });
    }

    public void markObserved() {
        observedCallback.run();
    }

    public ServerPlayerEntity target(net.minecraft.server.world.ServerWorld world) {
        return world.getPlayerByUuid(targetUuid) instanceof ServerPlayerEntity player ? player : null;
    }
}
