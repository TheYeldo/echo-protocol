package dev.yeldos.echoprotocol.config;

public final class EchoPresetManager {
    private EchoPresetManager() {
    }

    public static EchoPresetValues values(EchoConfig config) {
        return EchoPresetValues.forPreset(config.intensityPreset());
    }

    public static int eventIntervalSeconds(int configuredSeconds, EchoConfig config) {
        return Math.max(30, Math.round(configuredSeconds * values(config).eventIntervalMultiplier()));
    }

    public static int strongSilenceMinutes(int configuredMinutes, EchoConfig config) {
        return Math.max(1, Math.round(configuredMinutes * values(config).strongSilenceMultiplier()));
    }

    public static int adjustWeight(int weight, float multiplier) {
        return Math.max(0, Math.min(10_000, Math.round(Math.max(0, weight) * multiplier)));
    }
}
