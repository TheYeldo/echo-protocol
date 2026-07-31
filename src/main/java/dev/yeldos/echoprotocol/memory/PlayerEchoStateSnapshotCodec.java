package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.stage.FamiliarLocationSnapshot;
import dev.yeldos.echoprotocol.stage.FamiliarLocationType;
import dev.yeldos.echoprotocol.stage.PlayerEchoStateSnapshot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static dev.yeldos.echoprotocol.memory.NbtCompat.*;

final class PlayerEchoStateSnapshotCodec {
    private PlayerEchoStateSnapshotCodec() {
    }

    static NbtCompound write(PlayerEchoStateSnapshot snapshot) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("Stage", snapshot.stage());
        nbt.putLong("PlayTicks", snapshot.playTicks());
        nbt.putLong("NextEventDelay", snapshot.nextEventDelay());
        nbt.putLong("NextOriginalEventDelay", snapshot.nextOriginalEventDelay());
        nbt.putInt("StageOneEvents", snapshot.stageOneEvents());
        nbt.putInt("TotalEvents", snapshot.totalEvents());
        nbt.putInt("MemoryEvents", snapshot.memoryEvents());
        nbt.putInt("CorruptedEvents", snapshot.corruptedEvents());
        nbt.putInt("MimicEvents", snapshot.mimicEvents());
        nbt.putInt("OriginalEvents", snapshot.originalEvents());
        nbt.putBoolean("MimicIndependentActionSeen", snapshot.mimicIndependentActionSeen());
        NbtList locations = new NbtList();
        for (FamiliarLocationSnapshot location : snapshot.familiarLocations()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Type", location.type().name());
            entry.putString("Dimension", location.dimension());
            entry.putInt("X", location.position().getX());
            entry.putInt("Y", location.position().getY());
            entry.putInt("Z", location.position().getZ());
            entry.putInt("Visits", location.visits());
            entry.putLong("LastSeenTick", location.lastSeenTick());
            locations.add(entry);
        }
        nbt.put("FamiliarLocations", locations);
        return nbt;
    }

    static DecodeResult read(NbtCompound nbt) {
        List<FamiliarLocationSnapshot> locations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (containsType(nbt, "FamiliarLocations", NbtElement.LIST_TYPE)) {
            NbtList list = getList(nbt, "FamiliarLocations");
            for (int index = 0; index < Math.min(64, list.size()); index++) {
                try {
                    NbtCompound entry = getCompound(list, index);
                    locations.add(new FamiliarLocationSnapshot(
                            FamiliarLocationType.valueOf(getString(entry, "Type")),
                            required(entry, "Dimension"),
                            new BlockPos(getInt(entry, "X"), getInt(entry, "Y"), getInt(entry, "Z")),
                            getInt(entry, "Visits"), getLong(entry, "LastSeenTick")));
                } catch (RuntimeException exception) {
                    warnings.add("familiar location " + index + " skipped");
                }
            }
        }
        return new DecodeResult(new PlayerEchoStateSnapshot(getInt(nbt, "Stage"), getLong(nbt, "PlayTicks"),
                getLong(nbt, "NextEventDelay"), getLong(nbt, "NextOriginalEventDelay"),
                getInt(nbt, "StageOneEvents"), getInt(nbt, "TotalEvents"), getInt(nbt, "MemoryEvents"),
                getInt(nbt, "CorruptedEvents"), getInt(nbt, "MimicEvents"), getInt(nbt, "OriginalEvents"),
                getBoolean(nbt, "MimicIndependentActionSeen"), locations), List.copyOf(warnings));
    }

    private static String required(NbtCompound nbt, String key) {
        String value = getString(nbt, key);
        if (value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }

    record DecodeResult(PlayerEchoStateSnapshot snapshot, List<String> warnings) {
    }
}
