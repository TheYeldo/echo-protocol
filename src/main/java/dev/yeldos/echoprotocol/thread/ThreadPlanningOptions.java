package dev.yeldos.echoprotocol.thread;

import dev.yeldos.echoprotocol.config.EchoConfig;

public record ThreadPlanningOptions(
        int minimumSteps,
        int maximumSteps,
        boolean audioEnabled,
        boolean falseMemoryEnabled,
        boolean peripheralEnabled,
        boolean originalEnabled,
        boolean panicEnabled,
        boolean contradictionEnabled
) {
    public ThreadPlanningOptions {
        minimumSteps = Math.max(2, Math.min(4, minimumSteps));
        maximumSteps = Math.max(minimumSteps, Math.min(4, maximumSteps));
    }

    public static ThreadPlanningOptions from(EchoConfig config) {
        return new ThreadPlanningOptions(config.memoryThreadMinimumSteps(), config.memoryThreadMaximumSteps(),
                config.audioResidueEnabled() && config.threadAwareAudioResidueEnabled(),
                config.falseMemoriesEnabled() && config.threadAwareFalseMemoriesEnabled(),
                config.peripheralEchoesEnabled() && config.threadAwarePeripheralEchoesEnabled(),
                config.originalEnabled() && config.threadAwareOriginalEnabled(),
                config.panicImprintsEnabled(), config.contradictoryMemoriesEnabled());
    }
}
