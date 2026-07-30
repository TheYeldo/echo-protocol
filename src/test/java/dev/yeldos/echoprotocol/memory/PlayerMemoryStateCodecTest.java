package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.audio.AudioResidue;
import dev.yeldos.echoprotocol.audio.AudioResidueEvent;
import dev.yeldos.echoprotocol.contamination.ContaminationSource;
import dev.yeldos.echoprotocol.profile.ObservationMetric;
import dev.yeldos.echoprotocol.room.RoomMemoryType;
import dev.yeldos.echoprotocol.room.RoomObservation;
import dev.yeldos.echoprotocol.room.RoomObservationSource;
import dev.yeldos.echoprotocol.thread.MemoryThreadPlanner;
import dev.yeldos.echoprotocol.thread.MemoryThreadType;
import dev.yeldos.echoprotocol.thread.ThreadPlanningOptions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMemoryStateCodecTest {
    @Test
    void boundedStateRoundTripsWithThreadGraphProfileAndContamination() {
        UUID uuid = UUID.fromString("a3c47d11-a1b4-4348-9502-89060bc2c965");
        PlayerMemoryState state = new PlayerMemoryState();
        var bedroom = state.roomGraph().observe(observation("minecraft:overworld", 0, RoomMemoryType.BEDROOM, 10), 20, 48);
        var entrance = state.roomGraph().observe(observation("minecraft:overworld", 7, RoomMemoryType.ENTRANCE, 20), 20, 48);
        state.roomGraph().reinforceEdge(entrance.id(), bedroom.id(), new BlockPos(4, 64, 0), 25L, 48);
        state.restoreAudio(new AudioResidue(Identifier.of("minecraft", "block.wooden_door.open"),
                AudioResidueEvent.DOOR, "minecraft:overworld", new BlockPos(4, 64, 0), 0.3F, 0.9F, 30L));
        state.recordObservation(ObservationMetric.ECHO_FOLLOWED, 4.0F, 6.0F);
        state.recordObservation(ObservationMetric.APPROACH, 3.0F, 5.0F);
        var thread = new MemoryThreadPlanner().plan(uuid, MemoryThreadType.BEDROOM, bedroom.id(),
                "minecraft:overworld", "minecraft:red_bed", 40L, 7L,
                new ThreadPlanningOptions(2, 4, true, true, true, true, true, true)).orElseThrow();
        state.setActiveThread(thread);
        var config = dev.yeldos.echoprotocol.config.EchoConfig.defaults();
        assertTrue(state.contributeContamination(9L, ContaminationSource.THREAD_ADVANCED, 0.25F, config, false));

        PlayerMemoryState restored = PlayerMemoryStateCodec.read(PlayerMemoryStateCodec.write(state));

        assertEquals(2, restored.roomGraph().nodes().size());
        assertEquals(1, restored.roomGraph().edges().size());
        assertEquals(1, restored.audioResidues().size());
        assertNotNull(restored.activeThread());
        assertEquals(dev.yeldos.echoprotocol.thread.MemoryThreadStage.PAUSED,
                restored.activeThread().stage());
        assertTrue(restored.activeThread().resumedAfterRestart());
        assertEquals(thread.totalSteps(), restored.activeThread().totalSteps());
        assertEquals(0.25F, restored.contamination().value(), 0.0001F);
        assertTrue(restored.loadedFromPreviousSession());
        assertTrue(restored.validate().isEmpty());
    }

    @Test
    void malformedChildEntryDoesNotDiscardValidEntries() {
        PlayerMemoryState state = new PlayerMemoryState();
        state.restoreAudio(new AudioResidue(Identifier.of("minecraft", "block.wooden_door.open"),
                AudioResidueEvent.DOOR, "minecraft:overworld", BlockPos.ORIGIN, 0.3F, 0.8F, 1L));
        NbtCompound encoded = PlayerMemoryStateCodec.write(state);
        NbtList audio = encoded.getList("AudioResidues", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
        NbtCompound malformed = new NbtCompound();
        malformed.putString("Sound", "%%%bad%%%");
        audio.add(malformed);

        PlayerMemoryState restored = PlayerMemoryStateCodec.read(encoded);

        assertEquals(1, restored.audioResidues().size());
        assertFalse(restored.loadWarnings().isEmpty());
    }

    @Test
    void oneMalformedPlayerRecordDoesNotInvalidateAnother() {
        UUID uuid = UUID.fromString("765a4b32-cdde-43db-a749-5eacbcb02129");
        NbtCompound root = new NbtCompound();
        root.putInt("DataVersion", 1);
        NbtList players = new NbtList();
        NbtCompound valid = new NbtCompound();
        valid.putUuid("Uuid", uuid);
        valid.put("Memory", PlayerMemoryStateCodec.write(new PlayerMemoryState()));
        players.add(valid);
        players.add(new NbtCompound());
        root.put("Players", players);

        PersistentEchoMemory restored = PersistentEchoMemory.fromNbt(root, null);

        assertEquals(1, restored.records().size());
        assertNotNull(restored.record(uuid));
    }

    @Test
    void unsupportedFutureVersionStaysReadOnlyAndPreservesKnownStageAndUnknownData() {
        UUID uuid = UUID.fromString("37a9459f-ee86-4b84-90e1-c46a7ed6ac2a");
        NbtCompound root = new NbtCompound();
        root.putInt("DataVersion", MemoryDataVersion.CURRENT + 5);
        root.putString("FutureOnly", "preserve-me");
        NbtCompound entry = new NbtCompound();
        entry.putUuid("Uuid", uuid);
        entry.put("Stage", PlayerEchoStateSnapshotCodec.write(new dev.yeldos.echoprotocol.stage.PlayerEchoStateSnapshot(
                3, 400L, 2L, 3L, 1, 2, 3, 4, 5, 6, true, List.of())));
        NbtList players = new NbtList();
        players.add(entry);
        root.put("Players", players);

        PersistentEchoMemory restored = PersistentEchoMemory.fromNbt(root, null);
        NbtCompound written = restored.writeNbt(new NbtCompound(), null);

        assertTrue(restored.readOnly());
        assertEquals(3, restored.record(uuid).stage().stage());
        assertEquals("preserve-me", written.getString("FutureOnly"));
    }

    private static RoomObservation observation(String dimension, int x, RoomMemoryType type, long tick) {
        return new RoomObservation(dimension, new BlockPos(x, 64, 0), type, 0.75F, tick, "",
                List.of(), RoomObservationSource.EXPLICIT_INTERACTION);
    }
}
