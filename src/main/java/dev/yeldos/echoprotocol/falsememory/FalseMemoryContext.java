package dev.yeldos.echoprotocol.falsememory;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.stage.StageManager;

import java.util.UUID;

public record FalseMemoryContext(
        UUID targetUuid,
        EchoConfig config,
        StageManager stageManager,
        FalseMemoryHistory history,
        FalseMemoryPlan plan,
        long eventTick,
        Runnable observedCallback
) {
    public void markObserved() {
        observedCallback.run();
    }
}
