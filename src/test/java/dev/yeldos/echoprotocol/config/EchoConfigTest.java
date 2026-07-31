package dev.yeldos.echoprotocol.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoConfigTest {
    @TempDir
    Path directory;

    @Test
    void missingAndPartialOldConfigsReceiveDefaultsWithoutLosingValidValues() throws IOException {
        Path missing = directory.resolve("missing.json");
        EchoConfig defaults = EchoConfig.load(missing);
        assertTrue(defaults.enabled());
        assertTrue(Files.exists(missing));

        Path old = directory.resolve("old.json");
        Files.writeString(old, "{\"shared_echoes\":true,\"maximum_history_minutes\":7}");
        EchoConfig migrated = EchoConfig.load(old);
        assertTrue(migrated.sharedEchoes());
        assertEquals(7, migrated.maximumHistoryMinutes());
        assertTrue(read(old).has("false_memories_enabled"));
    }

    @Test
    void representativePointTwoPointThreeAndPointFourValuesSurviveMigration() throws IOException {
        Path pointTwo = directory.resolve("point-two.json");
        Files.writeString(pointTwo, "{\"shared_echoes\":true,\"memory_echo_weight\":55}");
        EchoConfig two = EchoConfig.load(pointTwo);
        assertTrue(two.sharedEchoes());
        assertEquals(55, two.memoryEchoWeight());

        Path pointThree = directory.resolve("point-three.json");
        Files.writeString(pointThree, "{\"original_enabled\":false,\"original_maximum_familiar_locations\":9}");
        EchoConfig three = EchoConfig.load(pointThree);
        assertFalse(three.originalEnabled());
        assertEquals(9, three.originalMaximumFamiliarLocations());

        Path pointFour = directory.resolve("point-four.json");
        Files.writeString(pointFour, "{\"false_memories_enabled\":false,\"maximum_tracked_habits\":7}");
        EchoConfig four = EchoConfig.load(pointFour);
        assertFalse(four.falseMemoriesEnabled());
        assertEquals(7, four.maximumTrackedHabits());
    }

    @Test
    void unsafeRangesAreClampedAndUnknownFieldsSurviveMigration() throws IOException {
        Path path = directory.resolve("range.json");
        Files.writeString(path, "{\"recording_sample_interval_ticks\":0,"
                + "\"maximum_history_minutes\":999,\"future_option\":{\"keep\":true}}");

        EchoConfig config = EchoConfig.load(path);

        assertEquals(1, config.recordingSampleIntervalTicks());
        assertEquals(60, config.maximumHistoryMinutes());
        assertTrue(read(path).getAsJsonObject("future_option").get("keep").getAsBoolean());
    }

    @Test
    void malformedConfigIsNotErased() throws IOException {
        Path path = directory.resolve("broken.json");
        String malformed = "{ this is not json";
        Files.writeString(path, malformed);

        EchoConfig config = EchoConfig.load(path);

        assertTrue(config.enabled());
        assertEquals(malformed, Files.readString(path));
    }

    @Test
    void zeroSessionLimitsRemainDisabledAfterValidation() throws IOException {
        Path path = directory.resolve("limits.json");
        Files.writeString(path, "{\"peripheral_echo_maximum_per_session\":0,"
                + "\"audio_residue_maximum_per_session\":0,\"false_memories_enabled\":false}");

        EchoConfig config = EchoConfig.load(path);

        assertEquals(0, config.peripheralEchoMaximumPerSession());
        assertEquals(0, config.audioResidueMaximumPerSession());
        assertFalse(config.falseMemoriesEnabled());
    }

    @Test
    void betaDefaultsAndUnsafeBoundsAreMigratedAndClamped() throws IOException {
        Path path = directory.resolve("beta.json");
        Files.writeString(path, "{\"room_memory_maximum_nodes\":999,"
                + "\"room_memory_maximum_edges\":999,\"memory_thread_minimum_steps\":4,"
                + "\"memory_thread_maximum_steps\":2,\"memory_contamination_initial\":8.0,"
                + "\"observation_profile_adaptation_strength\":9.0,\"intensity_preset\":\"invalid\"}");

        EchoConfig config = EchoConfig.load(path);

        assertEquals(20, config.roomMemoryMaximumNodes());
        assertEquals(48, config.roomMemoryMaximumEdges());
        assertEquals(4, config.memoryThreadMinimumSteps());
        assertEquals(4, config.memoryThreadMaximumSteps());
        assertEquals(1.0F, config.memoryContaminationInitial());
        assertEquals(0.50F, config.observationProfileAdaptationStrength());
        assertEquals(EchoIntensityPreset.STANDARD, config.intensityPreset());
    }

    @Test
    void presetsChangeRuntimeValuesWithoutResettingUnrelatedConfig() throws IOException {
        Path path = directory.resolve("preset.json");
        Files.writeString(path, "{\"maximum_history_minutes\":7,\"future_option\":true}");
        EchoConfig config = EchoConfig.load(path);
        EchoPresetValues standard = EchoPresetManager.values(config);
        EchoPresetValues subtle = EchoPresetValues.forPreset(EchoIntensityPreset.SUBTLE);
        EchoPresetValues intense = EchoPresetValues.forPreset(EchoIntensityPreset.INTENSE);

        assertEquals(1.0F, standard.eventIntervalMultiplier());
        assertTrue(subtle.eventIntervalMultiplier() > 1.0F);
        assertTrue(intense.eventIntervalMultiplier() < 1.0F);
        assertEquals(7, config.maximumHistoryMinutes());
        assertTrue(read(path).get("future_option").getAsBoolean());
    }

    @Test
    void legacyOriginalDefaultSpeedsUpgradeButCustomSpeedsRemainIntentional() throws IOException {
        Path legacy = directory.resolve("legacy-speeds.json");
        Files.writeString(legacy, "{\"original_walk_speed\":0.09,\"original_slow_walk_speed\":0.065,"
                + "\"original_fast_walk_speed\":0.13,\"original_maximum_speed\":0.16,"
                + "\"original_acceleration\":0.012,\"original_deceleration\":0.018}");

        EchoConfig upgraded = EchoConfig.load(legacy);

        assertEquals(0.12F, upgraded.originalWalkSpeed());
        assertEquals(0.075F, upgraded.originalSlowWalkSpeed());
        assertEquals(0.235F, upgraded.originalFastWalkSpeed());
        assertEquals(0.27F, upgraded.originalMaximumSpeed());
        assertEquals(0.020F, upgraded.originalAcceleration());
        assertEquals(0.028F, upgraded.originalDeceleration());

        Path custom = directory.resolve("custom-speeds.json");
        Files.writeString(custom, "{\"original_walk_speed\":0.11,\"original_slow_walk_speed\":0.06,"
                + "\"original_fast_walk_speed\":0.18,\"original_maximum_speed\":0.22}");

        EchoConfig preserved = EchoConfig.load(custom);

        assertEquals(0.11F, preserved.originalWalkSpeed());
        assertEquals(0.06F, preserved.originalSlowWalkSpeed());
        assertEquals(0.18F, preserved.originalFastWalkSpeed());
        assertEquals(0.22F, preserved.originalMaximumSpeed());
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
