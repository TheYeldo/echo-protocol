package dev.yeldos.echoprotocol.memory;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryMigrationTest {
    @Test
    void olderPointTwoAndPointThreeShapesReceiveSafeDefaults() {
        UUID old = UUID.fromString("5dd2b573-0038-4ba2-853b-f4a87c093712");
        MemoryMigration.MigrationResult pointTwo = MemoryMigration.migrateLegacyJson(
                "{\"players\":{\"" + old + "\":{\"stage\":1,\"playTicks\":20}}}");
        MemoryMigration.MigrationResult pointThree = MemoryMigration.migrateLegacyJson(
                "{\"dataVersion\":2,\"players\":{\"" + old + "\":{\"stage\":2,\"memoryEvents\":3}}}");

        assertEquals(1, pointTwo.players().get(old).stage());
        assertEquals(0L, pointTwo.players().get(old).nextEventDelay());
        assertEquals(2, pointThree.players().get(old).stage());
        assertEquals(3, pointThree.players().get(old).memoryEvents());
    }

    @Test
    void migratesRepresentativePointFourStateWithoutLosingProgression() {
        UUID uuid = UUID.fromString("8ed6cebc-1ffc-4f65-a742-7d7261154b52");
        String json = """
                {
                  "dataVersion": 3,
                  "players": {
                    "%s": {
                      "stage": 3,
                      "playTicks": 250000,
                      "nextEventDelay": 400,
                      "nextOriginalEventDelay": 1200,
                      "stageOneEvents": 5,
                      "totalEvents": 19,
                      "memoryEvents": 8,
                      "corruptedEvents": 4,
                      "mimicEvents": 2,
                      "originalEvents": 1,
                      "mimicIndependentActionSeen": true,
                      "familiarLocations": [
                        {"type":"BED","dimension":"minecraft:overworld","x":4,"y":64,"z":9,"visits":7,"lastSeenTick":42}
                      ]
                    }
                  }
                }
                """.formatted(uuid);

        MemoryMigration.MigrationResult result = MemoryMigration.migrateLegacyJson(json);
        var migrated = result.players().get(uuid);

        assertTrue(result.recoverable());
        assertEquals(3, migrated.stage());
        assertEquals(250000L, migrated.playTicks());
        assertEquals(400L, migrated.nextEventDelay());
        assertEquals(8, migrated.memoryEvents());
        assertTrue(migrated.mimicIndependentActionSeen());
        assertEquals(1, migrated.familiarLocations().size());
        assertEquals(7, migrated.familiarLocations().getFirst().visits());
    }

    @Test
    void malformedPlayersAndLocationsAreSkippedIndependently() {
        UUID valid = UUID.fromString("868352dc-b9f3-4aaf-b349-8604831490ec");
        String json = """
                {"dataVersion":3,"players":{
                  "not-a-uuid":{"stage":3},
                  "%s":{"stage":2,"playTicks":99,"familiarLocations":[
                    {"type":"NOT_A_TYPE","dimension":"minecraft:overworld","x":0,"y":64,"z":0},
                    {"type":"CHEST","dimension":"minecraft:overworld","x":2,"y":64,"z":3,"visits":2}
                  ]}
                }}
                """.formatted(valid);

        MemoryMigration.MigrationResult result = MemoryMigration.migrateLegacyJson(json);

        assertEquals(1, result.players().size());
        assertEquals(1, result.players().get(valid).familiarLocations().size());
        assertFalse(result.warnings().isEmpty());
    }
}
