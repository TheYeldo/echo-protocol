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

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
