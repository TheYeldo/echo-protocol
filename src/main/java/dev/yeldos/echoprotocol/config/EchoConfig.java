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
        boolean debugLogging,
        boolean memoryEchoEnabled,
        boolean corruptedEchoEnabled,
        boolean mimicEchoEnabled,
        int memoryEchoWeight,
        int corruptedEchoWeight,
        int mimicEchoWeight,
        int mimicMinimumStageTwoMinutes,
        int mimicSessionCooldownMinutes,
        int mimicMovementDelayMinTicks,
        int mimicMovementDelayMaxTicks,
        boolean mimicDamageEnabled,
        float mimicDamage,
        int mimicMaxHitsPerEvent,
        boolean mimicChaseEnabled,
        int mimicChaseMinSeconds,
        int mimicChaseMaxSeconds,
        boolean corruptedEchoCanApproach,
        boolean echoMovesWhenUnobserved,
        boolean echoLightEffects,
        boolean echoSoundEffects,
        boolean realPlayerSkins,
        boolean skinCacheEnabled,
        float memoryEchoOpacity,
        float corruptedEchoOpacity,
        float mimicEchoOpacity,
        float mimicThreateningOpacity,
        boolean corruptionFlickerEnabled,
        boolean corruptionAfterimagesEnabled,
        float corruptionVisualIntensity,
        boolean reducedVisualEffects,
        boolean reducedFlashing,
        float echoMasterVolume,
        float memoryEchoVolume,
        float corruptedEchoVolume,
        float mimicEchoVolume,
        boolean breathingEnabled,
        boolean staticEffectsEnabled,
        int safeSpawnAttempts,
        int minimumEchoSpawnDistance,
        int maximumEchoSpawnDistance
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("echo_protocol.json");

    public static EchoConfig defaults() {
        return new EchoConfig(true, 2, 10, 5, 15, 10, 60, 480, 1080, false, false, true, true, 0.45F, false,
                true, true, true, 70, 25, 5, 30, 60, 40, 80, true, 4.0F, 1, true, 5, 10, true, true, true, true,
                true, true, 0.42F, 0.48F, 0.58F, 0.90F, true, true, 0.65F, false, true,
                0.75F, 0.55F, 0.70F, 0.80F, true, true, 16, 8, 32);
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
                torchFlicker, soundEchoes, echoOpacity, debugLogging, memoryEchoEnabled, corruptedEchoEnabled,
                mimicEchoEnabled, memoryEchoWeight, corruptedEchoWeight, mimicEchoWeight, mimicMinimumStageTwoMinutes,
                mimicSessionCooldownMinutes, mimicMovementDelayMinTicks, mimicMovementDelayMaxTicks, mimicDamageEnabled,
                mimicDamage, mimicMaxHitsPerEvent, mimicChaseEnabled, mimicChaseMinSeconds, mimicChaseMaxSeconds,
                corruptedEchoCanApproach, echoMovesWhenUnobserved, echoLightEffects, echoSoundEffects,
                realPlayerSkins, skinCacheEnabled, memoryEchoOpacity, corruptedEchoOpacity, mimicEchoOpacity,
                mimicThreateningOpacity, corruptionFlickerEnabled, corruptionAfterimagesEnabled,
                corruptionVisualIntensity, reducedVisualEffects, reducedFlashing, echoMasterVolume,
                memoryEchoVolume, corruptedEchoVolume, mimicEchoVolume, breathingEnabled, staticEffectsEnabled,
                safeSpawnAttempts, minimumEchoSpawnDistance, maximumEchoSpawnDistance).validate();
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
        int memoryWeight = clamp(memoryEchoWeight, 0, 1000);
        int corruptedWeight = clamp(corruptedEchoWeight, 0, 1000);
        int mimicWeight = clamp(mimicEchoWeight, 0, 1000);
        int mimicStageMinutes = clamp(mimicMinimumStageTwoMinutes, 0, 1440);
        int mimicCooldown = clamp(mimicSessionCooldownMinutes, 1, 1440);
        int delayMin = clamp(mimicMovementDelayMinTicks, 10, 200);
        int delayMax = clamp(Math.max(mimicMovementDelayMaxTicks, delayMin), delayMin, 400);
        float damage = Math.max(0.0F, Math.min(20.0F, mimicDamage));
        int maxHits = clamp(mimicMaxHitsPerEvent, 0, 5);
        int chaseMin = clamp(mimicChaseMinSeconds, 1, 30);
        int chaseMax = clamp(Math.max(mimicChaseMaxSeconds, chaseMin), chaseMin, 60);
        float memoryOpacity = clampFloat(memoryEchoOpacity, 0.05F, 1.0F);
        float corruptedOpacity = clampFloat(corruptedEchoOpacity, 0.05F, 1.0F);
        float mimicOpacity = clampFloat(mimicEchoOpacity, 0.05F, 1.0F);
        float threatOpacity = clampFloat(mimicThreateningOpacity, mimicOpacity, 1.0F);
        float visualIntensity = clampFloat(corruptionVisualIntensity, 0.0F, 1.0F);
        float masterVolume = clampFloat(echoMasterVolume, 0.0F, 1.0F);
        float memoryVolume = clampFloat(memoryEchoVolume, 0.0F, 1.0F);
        float corruptedVolume = clampFloat(corruptedEchoVolume, 0.0F, 1.0F);
        float mimicVolume = clampFloat(mimicEchoVolume, 0.0F, 1.0F);
        int attempts = clamp(safeSpawnAttempts, 1, 64);
        int minDistance = clamp(minimumEchoSpawnDistance, 2, 64);
        int maxDistance = clamp(Math.max(maximumEchoSpawnDistance, minDistance), minDistance, 128);
        return new EchoConfig(enabled, sample, history, minReplay, maxReplay, stageZero, stageTwo, minInterval,
                maxInterval, sharedEchoes, chatEchoes, torchFlicker, soundEchoes, opacity, debugLogging,
                memoryEchoEnabled, corruptedEchoEnabled, mimicEchoEnabled, memoryWeight, corruptedWeight, mimicWeight,
                mimicStageMinutes, mimicCooldown, delayMin, delayMax, mimicDamageEnabled, damage, maxHits,
                mimicChaseEnabled, chaseMin, chaseMax, corruptedEchoCanApproach, echoMovesWhenUnobserved,
                echoLightEffects, echoSoundEffects, realPlayerSkins, skinCacheEnabled, memoryOpacity,
                corruptedOpacity, mimicOpacity, threatOpacity, corruptionFlickerEnabled, corruptionAfterimagesEnabled,
                visualIntensity, reducedVisualEffects, reducedFlashing, masterVolume, memoryVolume, corruptedVolume,
                mimicVolume, breathingEnabled, staticEffectsEnabled, attempts, minDistance, maxDistance);
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
        raw.memory_echo_enabled = memoryEchoEnabled;
        raw.corrupted_echo_enabled = corruptedEchoEnabled;
        raw.mimic_echo_enabled = mimicEchoEnabled;
        raw.memory_echo_weight = memoryEchoWeight;
        raw.corrupted_echo_weight = corruptedEchoWeight;
        raw.mimic_echo_weight = mimicEchoWeight;
        raw.mimic_minimum_stage_two_minutes = mimicMinimumStageTwoMinutes;
        raw.mimic_session_cooldown_minutes = mimicSessionCooldownMinutes;
        raw.mimic_movement_delay_min_ticks = mimicMovementDelayMinTicks;
        raw.mimic_movement_delay_max_ticks = mimicMovementDelayMaxTicks;
        raw.mimic_damage_enabled = mimicDamageEnabled;
        raw.mimic_damage = mimicDamage;
        raw.mimic_max_hits_per_event = mimicMaxHitsPerEvent;
        raw.mimic_chase_enabled = mimicChaseEnabled;
        raw.mimic_chase_min_seconds = mimicChaseMinSeconds;
        raw.mimic_chase_max_seconds = mimicChaseMaxSeconds;
        raw.corrupted_echo_can_approach = corruptedEchoCanApproach;
        raw.echo_moves_when_unobserved = echoMovesWhenUnobserved;
        raw.echo_light_effects = echoLightEffects;
        raw.echo_sound_effects = echoSoundEffects;
        raw.real_player_skins = realPlayerSkins;
        raw.skin_cache_enabled = skinCacheEnabled;
        raw.memory_echo_opacity = memoryEchoOpacity;
        raw.corrupted_echo_opacity = corruptedEchoOpacity;
        raw.mimic_echo_opacity = mimicEchoOpacity;
        raw.mimic_threatening_opacity = mimicThreateningOpacity;
        raw.corruption_flicker_enabled = corruptionFlickerEnabled;
        raw.corruption_afterimages_enabled = corruptionAfterimagesEnabled;
        raw.corruption_visual_intensity = corruptionVisualIntensity;
        raw.reduced_visual_effects = reducedVisualEffects;
        raw.reduced_flashing = reducedFlashing;
        raw.echo_master_volume = echoMasterVolume;
        raw.memory_echo_volume = memoryEchoVolume;
        raw.corrupted_echo_volume = corruptedEchoVolume;
        raw.mimic_echo_volume = mimicEchoVolume;
        raw.breathing_enabled = breathingEnabled;
        raw.static_effects_enabled = staticEffectsEnabled;
        raw.safe_spawn_attempts = safeSpawnAttempts;
        raw.minimum_echo_spawn_distance = minimumEchoSpawnDistance;
        raw.maximum_echo_spawn_distance = maximumEchoSpawnDistance;
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
                raw.debug_logging == null ? defaults.debugLogging : raw.debug_logging,
                raw.memory_echo_enabled == null ? defaults.memoryEchoEnabled : raw.memory_echo_enabled,
                raw.corrupted_echo_enabled == null ? defaults.corruptedEchoEnabled : raw.corrupted_echo_enabled,
                raw.mimic_echo_enabled == null ? defaults.mimicEchoEnabled : raw.mimic_echo_enabled,
                raw.memory_echo_weight == null ? defaults.memoryEchoWeight : raw.memory_echo_weight,
                raw.corrupted_echo_weight == null ? defaults.corruptedEchoWeight : raw.corrupted_echo_weight,
                raw.mimic_echo_weight == null ? defaults.mimicEchoWeight : raw.mimic_echo_weight,
                raw.mimic_minimum_stage_two_minutes == null ? defaults.mimicMinimumStageTwoMinutes : raw.mimic_minimum_stage_two_minutes,
                raw.mimic_session_cooldown_minutes == null ? defaults.mimicSessionCooldownMinutes : raw.mimic_session_cooldown_minutes,
                raw.mimic_movement_delay_min_ticks == null ? defaults.mimicMovementDelayMinTicks : raw.mimic_movement_delay_min_ticks,
                raw.mimic_movement_delay_max_ticks == null ? defaults.mimicMovementDelayMaxTicks : raw.mimic_movement_delay_max_ticks,
                raw.mimic_damage_enabled == null ? defaults.mimicDamageEnabled : raw.mimic_damage_enabled,
                raw.mimic_damage == null ? defaults.mimicDamage : raw.mimic_damage,
                raw.mimic_max_hits_per_event == null ? defaults.mimicMaxHitsPerEvent : raw.mimic_max_hits_per_event,
                raw.mimic_chase_enabled == null ? defaults.mimicChaseEnabled : raw.mimic_chase_enabled,
                raw.mimic_chase_min_seconds == null ? defaults.mimicChaseMinSeconds : raw.mimic_chase_min_seconds,
                raw.mimic_chase_max_seconds == null ? defaults.mimicChaseMaxSeconds : raw.mimic_chase_max_seconds,
                raw.corrupted_echo_can_approach == null ? defaults.corruptedEchoCanApproach : raw.corrupted_echo_can_approach,
                raw.echo_moves_when_unobserved == null ? defaults.echoMovesWhenUnobserved : raw.echo_moves_when_unobserved,
                raw.echo_light_effects == null ? defaults.echoLightEffects : raw.echo_light_effects,
                raw.echo_sound_effects == null ? defaults.echoSoundEffects : raw.echo_sound_effects,
                raw.real_player_skins == null ? defaults.realPlayerSkins : raw.real_player_skins,
                raw.skin_cache_enabled == null ? defaults.skinCacheEnabled : raw.skin_cache_enabled,
                raw.memory_echo_opacity == null ? defaults.memoryEchoOpacity : raw.memory_echo_opacity,
                raw.corrupted_echo_opacity == null ? defaults.corruptedEchoOpacity : raw.corrupted_echo_opacity,
                raw.mimic_echo_opacity == null ? defaults.mimicEchoOpacity : raw.mimic_echo_opacity,
                raw.mimic_threatening_opacity == null ? defaults.mimicThreateningOpacity : raw.mimic_threatening_opacity,
                raw.corruption_flicker_enabled == null ? defaults.corruptionFlickerEnabled : raw.corruption_flicker_enabled,
                raw.corruption_afterimages_enabled == null ? defaults.corruptionAfterimagesEnabled : raw.corruption_afterimages_enabled,
                raw.corruption_visual_intensity == null ? defaults.corruptionVisualIntensity : raw.corruption_visual_intensity,
                raw.reduced_visual_effects == null ? defaults.reducedVisualEffects : raw.reduced_visual_effects,
                raw.reduced_flashing == null ? defaults.reducedFlashing : raw.reduced_flashing,
                raw.echo_master_volume == null ? defaults.echoMasterVolume : raw.echo_master_volume,
                raw.memory_echo_volume == null ? defaults.memoryEchoVolume : raw.memory_echo_volume,
                raw.corrupted_echo_volume == null ? defaults.corruptedEchoVolume : raw.corrupted_echo_volume,
                raw.mimic_echo_volume == null ? defaults.mimicEchoVolume : raw.mimic_echo_volume,
                raw.breathing_enabled == null ? defaults.breathingEnabled : raw.breathing_enabled,
                raw.static_effects_enabled == null ? defaults.staticEffectsEnabled : raw.static_effects_enabled,
                raw.safe_spawn_attempts == null ? defaults.safeSpawnAttempts : raw.safe_spawn_attempts,
                raw.minimum_echo_spawn_distance == null ? defaults.minimumEchoSpawnDistance : raw.minimum_echo_spawn_distance,
                raw.maximum_echo_spawn_distance == null ? defaults.maximumEchoSpawnDistance : raw.maximum_echo_spawn_distance
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
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
        Boolean memory_echo_enabled;
        Boolean corrupted_echo_enabled;
        Boolean mimic_echo_enabled;
        Integer memory_echo_weight;
        Integer corrupted_echo_weight;
        Integer mimic_echo_weight;
        Integer mimic_minimum_stage_two_minutes;
        Integer mimic_session_cooldown_minutes;
        Integer mimic_movement_delay_min_ticks;
        Integer mimic_movement_delay_max_ticks;
        Boolean mimic_damage_enabled;
        Float mimic_damage;
        Integer mimic_max_hits_per_event;
        Boolean mimic_chase_enabled;
        Integer mimic_chase_min_seconds;
        Integer mimic_chase_max_seconds;
        Boolean corrupted_echo_can_approach;
        Boolean echo_moves_when_unobserved;
        Boolean echo_light_effects;
        Boolean echo_sound_effects;
        Boolean real_player_skins;
        Boolean skin_cache_enabled;
        Float memory_echo_opacity;
        Float corrupted_echo_opacity;
        Float mimic_echo_opacity;
        Float mimic_threatening_opacity;
        Boolean corruption_flicker_enabled;
        Boolean corruption_afterimages_enabled;
        Float corruption_visual_intensity;
        Boolean reduced_visual_effects;
        Boolean reduced_flashing;
        Float echo_master_volume;
        Float memory_echo_volume;
        Float corrupted_echo_volume;
        Float mimic_echo_volume;
        Boolean breathing_enabled;
        Boolean static_effects_enabled;
        Integer safe_spawn_attempts;
        Integer minimum_echo_spawn_distance;
        Integer maximum_echo_spawn_distance;
    }
}
