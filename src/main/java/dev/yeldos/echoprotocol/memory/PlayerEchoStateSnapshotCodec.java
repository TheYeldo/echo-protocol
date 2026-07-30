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
        if (nbt.contains("FamiliarLocations", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("FamiliarLocations", NbtElement.COMPOUND_TYPE);
            for (int index = 0; index < Math.min(64, list.size()); index++) {
                try {
                    NbtCompound entry = list.getCompound(index);
                    locations.add(new FamiliarLocationSnapshot(
                            FamiliarLocationType.valueOf(entry.getString("Type")),
                            required(entry, "Dimension"),
                            new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")),
                            entry.getInt("Visits"), entry.getLong("LastSeenTick")));
                } catch (RuntimeException exception) {
                    warnings.add("familiar location " + index + " skipped");
                }
            }
        }
        return new DecodeResult(new PlayerEchoStateSnapshot(nbt.getInt("Stage"), nbt.getLong("PlayTicks"),
                nbt.getLong("NextEventDelay"), nbt.getLong("NextOriginalEventDelay"),
                nbt.getInt("StageOneEvents"), nbt.getInt("TotalEvents"), nbt.getInt("MemoryEvents"),
                nbt.getInt("CorruptedEvents"), nbt.getInt("MimicEvents"), nbt.getInt("OriginalEvents"),
                nbt.getBoolean("MimicIndependentActionSeen"), locations), List.copyOf(warnings));
    }

    private static String required(NbtCompound nbt, String key) {
        String value = nbt.getString(key);
        if (value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }

    record DecodeResult(PlayerEchoStateSnapshot snapshot, List<String> warnings) {
    }
}
