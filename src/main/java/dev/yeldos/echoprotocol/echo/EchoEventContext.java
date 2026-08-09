package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class EchoEventContext {
    private final UUID targetUuid;
    private final EchoConfig config;
    private final StageManager stageManager;
    private final boolean forcedHostile;
    private final boolean awardsProgress;
    private final Runnable observedCallback;
    private boolean observed;

    public EchoEventContext(UUID targetUuid, EchoConfig config, StageManager stageManager, boolean forcedHostile,
                            boolean awardsProgress, Runnable observedCallback) {
        this.targetUuid = targetUuid;
        this.config = config;
        this.stageManager = stageManager;
        this.forcedHostile = forcedHostile;
        this.awardsProgress = awardsProgress;
        this.observedCallback = observedCallback == null ? () -> { } : observedCallback;
    }

    public EchoEventContext(UUID targetUuid, EchoConfig config, StageManager stageManager, boolean forcedHostile) {
        this(targetUuid, config, stageManager, forcedHostile, true, () -> { });
    }

    public EchoEventContext(UUID targetUuid, EchoConfig config, StageManager stageManager, boolean forcedHostile,
                            boolean awardsProgress) {
        this(targetUuid, config, stageManager, forcedHostile, awardsProgress, () -> { });
    }

    public void markObserved() {
        observed = true;
        observedCallback.run();
    }

    public boolean wasObserved() {
        return observed;
    }

    public UUID targetUuid() {
        return targetUuid;
    }

    public EchoConfig config() {
        return config;
    }

    public StageManager stageManager() {
        return stageManager;
    }

    public boolean forcedHostile() {
        return forcedHostile;
    }

    public boolean awardsProgress() {
        return awardsProgress;
    }

    public ServerPlayerEntity target(net.minecraft.server.world.ServerWorld world) {
        return world.getPlayerByUuid(targetUuid) instanceof ServerPlayerEntity player ? player : null;
    }
}
