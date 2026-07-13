package dev.yeldos.echoprotocol.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.yeldos.echoprotocol.EchoProtocol;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record EchoConfig(
        boolean enabled,
        int recordingSampleIntervalTicks,
        int maximumHistoryMinutes,
        int minimumReplaySeconds,
        int maximumReplaySeconds,
        int stageZeroMinutes,
        int stageTwoMinutes,
        int minimumEventIntervalSeconds,
        int maximumEventIntervalSeconds,
        boolean sharedEchoes,
        boolean chatEchoes,
        boolean torchFlicker,
        boolean soundEchoes,
        float echoOpacity,
        boolean debugLogging
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("echo_protocol.json");

    public static EchoConfig defaults() {
        return new EchoConfig(true, 2, 10, 5, 15, 10, 60, 480, 1080, false, false, true, true, 0.45F, false);
    }

    public static EchoConfig load() {
        EchoConfig config = defaults();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Raw raw = GSON.fromJson(reader, Raw.class);
                config = fromRaw(raw).validate();
            } catch (IOException | JsonSyntaxException | NullPointerException exception) {
                EchoProtocol.LOGGER.warn("Failed to load echo_protocol.json; using safe defaults.", exception);
            }
        }
        config.save();
        return config;
    }

    public EchoConfig withDebugLogging(boolean debugLogging) {
        EchoConfig updated = new EchoConfig(enabled, recordingSampleIntervalTicks, maximumHistoryMinutes,
                minimumReplaySeconds, maximumReplaySeconds, stageZeroMinutes, stageTwoMinutes,
                minimumEventIntervalSeconds, maximumEventIntervalSeconds, sharedEchoes, chatEchoes,
                torchFlicker, soundEchoes, echoOpacity, debugLogging).validate();
        updated.save();
        return updated;
    }

    public int maxFrames() {
        int totalTicks = maximumHistoryMinutes * 60 * 20;
        return Math.max(20, totalTicks / recordingSampleIntervalTicks);
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(toRaw(), writer);
            }
        } catch (IOException exception) {
            EchoProtocol.LOGGER.warn("Failed to save echo_protocol.json.", exception);
        }
    }

    private EchoConfig validate() {
        int sample = clamp(recordingSampleIntervalTicks, 1, 20);
        int history = clamp(maximumHistoryMinutes, 1, 60);
        int minReplay = clamp(minimumReplaySeconds, 2, 120);
        int maxReplay = clamp(Math.max(maximumReplaySeconds, minReplay), minReplay, 180);
        int stageZero = clamp(stageZeroMinutes, 0, 240);
        int stageTwo = clamp(Math.max(stageTwoMinutes, stageZero), stageZero, 1440);
        int minInterval = clamp(minimumEventIntervalSeconds, 30, 86400);
        int maxInterval = clamp(Math.max(maximumEventIntervalSeconds, minInterval), minInterval, 86400);
        float opacity = Math.max(0.05F, Math.min(1.0F, echoOpacity));
        return new EchoConfig(enabled, sample, history, minReplay, maxReplay, stageZero, stageTwo, minInterval,
                maxInterval, sharedEchoes, chatEchoes, torchFlicker, soundEchoes, opacity, debugLogging);
    }

    private Raw toRaw() {
        Raw raw = new Raw();
        raw.enabled = enabled;
        raw.recording_sample_interval_ticks = recordingSampleIntervalTicks;
        raw.maximum_history_minutes = maximumHistoryMinutes;
        raw.minimum_replay_seconds = minimumReplaySeconds;
        raw.maximum_replay_seconds = maximumReplaySeconds;
        raw.stage_zero_minutes = stageZeroMinutes;
        raw.stage_two_minutes = stageTwoMinutes;
        raw.minimum_event_interval_seconds = minimumEventIntervalSeconds;
        raw.maximum_event_interval_seconds = maximumEventIntervalSeconds;
        raw.shared_echoes = sharedEchoes;
        raw.chat_echoes = chatEchoes;
        raw.torch_flicker = torchFlicker;
        raw.sound_echoes = soundEchoes;
        raw.echo_opacity = echoOpacity;
        raw.debug_logging = debugLogging;
        return raw;
    }

    private static EchoConfig fromRaw(Raw raw) {
        EchoConfig defaults = defaults();
        if (raw == null) {
            return defaults;
        }
        return new EchoConfig(
                raw.enabled == null ? defaults.enabled : raw.enabled,
                raw.recording_sample_interval_ticks == null ? defaults.recordingSampleIntervalTicks : raw.recording_sample_interval_ticks,
                raw.maximum_history_minutes == null ? defaults.maximumHistoryMinutes : raw.maximum_history_minutes,
                raw.minimum_replay_seconds == null ? defaults.minimumReplaySeconds : raw.minimum_replay_seconds,
                raw.maximum_replay_seconds == null ? defaults.maximumReplaySeconds : raw.maximum_replay_seconds,
                raw.stage_zero_minutes == null ? defaults.stageZeroMinutes : raw.stage_zero_minutes,
                raw.stage_two_minutes == null ? defaults.stageTwoMinutes : raw.stage_two_minutes,
                raw.minimum_event_interval_seconds == null ? defaults.minimumEventIntervalSeconds : raw.minimum_event_interval_seconds,
                raw.maximum_event_interval_seconds == null ? defaults.maximumEventIntervalSeconds : raw.maximum_event_interval_seconds,
                raw.shared_echoes == null ? defaults.sharedEchoes : raw.shared_echoes,
                raw.chat_echoes == null ? defaults.chatEchoes : raw.chat_echoes,
                raw.torch_flicker == null ? defaults.torchFlicker : raw.torch_flicker,
                raw.sound_echoes == null ? defaults.soundEchoes : raw.sound_echoes,
                raw.echo_opacity == null ? defaults.echoOpacity : raw.echo_opacity,
                raw.debug_logging == null ? defaults.debugLogging : raw.debug_logging
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Raw {
        Boolean enabled;
        Integer recording_sample_interval_ticks;
        Integer maximum_history_minutes;
        Integer minimum_replay_seconds;
        Integer maximum_replay_seconds;
        Integer stage_zero_minutes;
        Integer stage_two_minutes;
        Integer minimum_event_interval_seconds;
        Integer maximum_event_interval_seconds;
        Boolean shared_echoes;
        Boolean chat_echoes;
        Boolean torch_flicker;
        Boolean sound_echoes;
        Float echo_opacity;
        Boolean debug_logging;
    }
}
