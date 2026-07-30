package dev.yeldos.echoprotocol.memory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.yeldos.echoprotocol.stage.FamiliarLocationSnapshot;
import dev.yeldos.echoprotocol.stage.FamiliarLocationType;
import dev.yeldos.echoprotocol.stage.PlayerEchoStateSnapshot;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MemoryMigration {
    private MemoryMigration() {
    }

    public static MigrationResult migrateLegacyJson(String json) {
        Map<UUID, PlayerEchoStateSnapshot> players = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException exception) {
            return new MigrationResult(Map.of(), List.of("legacy state is malformed"), false);
        }
        int dataVersion = integer(root, "dataVersion", 0);
        JsonObject playerObject = object(root, "players");
        if (playerObject == null) {
            return new MigrationResult(Map.of(), List.of(), true);
        }
        for (Map.Entry<String, JsonElement> entry : playerObject.entrySet()) {
            try {
                UUID uuid = UUID.fromString(entry.getKey());
                JsonObject player = entry.getValue().getAsJsonObject();
                List<FamiliarLocationSnapshot> familiar = readFamiliar(player, warnings, uuid);
                long nextEvent = dataVersion >= 3 ? longValue(player, "nextEventDelay", 0L) : 0L;
                long nextOriginal = dataVersion >= 3 ? longValue(player, "nextOriginalEventDelay", 0L) : 0L;
                players.put(uuid, new PlayerEchoStateSnapshot(integer(player, "stage", 0),
                        longValue(player, "playTicks", 0L), nextEvent, nextOriginal,
                        integer(player, "stageOneEvents", 0), integer(player, "totalEvents", 0),
                        integer(player, "memoryEvents", 0), integer(player, "corruptedEvents", 0),
                        integer(player, "mimicEvents", 0), integer(player, "originalEvents", 0),
                        bool(player, "mimicIndependentActionSeen", false), familiar));
            } catch (RuntimeException exception) {
                warnings.add("legacy player " + entry.getKey() + " skipped");
            }
        }
        return new MigrationResult(Map.copyOf(players), List.copyOf(warnings), true);
    }

    private static List<FamiliarLocationSnapshot> readFamiliar(JsonObject player, List<String> warnings, UUID uuid) {
        JsonArray array = array(player, "familiarLocations");
        if (array == null) {
            return List.of();
        }
        List<FamiliarLocationSnapshot> result = new ArrayList<>();
        for (int index = 0; index < Math.min(64, array.size()); index++) {
            try {
                JsonObject location = array.get(index).getAsJsonObject();
                result.add(new FamiliarLocationSnapshot(
                        FamiliarLocationType.valueOf(string(location, "type", "")),
                        string(location, "dimension", ""),
                        new BlockPos(integer(location, "x", 0), integer(location, "y", 0), integer(location, "z", 0)),
                        integer(location, "visits", 1), longValue(location, "lastSeenTick", 0L)));
            } catch (RuntimeException exception) {
                warnings.add("legacy familiar location " + index + " skipped for " + uuid);
            }
        }
        return List.copyOf(result);
    }

    private static JsonObject object(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : null;
    }

    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : null;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (RuntimeException exception) { return fallback; }
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        try { return object.has(key) ? object.get(key).getAsLong() : fallback; }
        catch (RuntimeException exception) { return fallback; }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        try { return object.has(key) ? object.get(key).getAsBoolean() : fallback; }
        catch (RuntimeException exception) { return fallback; }
    }

    private static String string(JsonObject object, String key, String fallback) {
        try { return object.has(key) ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException exception) { return fallback; }
    }

    public record MigrationResult(Map<UUID, PlayerEchoStateSnapshot> players, List<String> warnings,
                                  boolean recoverable) {
    }
}
