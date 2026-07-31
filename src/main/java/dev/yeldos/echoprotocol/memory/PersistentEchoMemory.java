package dev.yeldos.echoprotocol.memory;

import com.mojang.serialization.Codec;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.stage.PlayerEchoStateSnapshot;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.yeldos.echoprotocol.memory.NbtCompat.*;

public final class PersistentEchoMemory extends PersistentState {
    public static final String STORAGE_KEY = "echo_protocol_memory";
    private static final Codec<PersistentEchoMemory> CODEC = NbtCompound.CODEC.xmap(
            PersistentEchoMemory::fromNbt, PersistentEchoMemory::toNbt);
    public static final PersistentStateType<PersistentEchoMemory> TYPE = new PersistentStateType<>(STORAGE_KEY,
            PersistentEchoMemory::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, PlayerRecord> players = new LinkedHashMap<>();
    private boolean legacyImported;
    private int unsupportedFutureVersion;
    private NbtCompound futureData;

    public PersistentEchoMemory() {
    }

    private static PersistentEchoMemory fromNbt(NbtCompound nbt) {
        PersistentEchoMemory memory = new PersistentEchoMemory();
        int version = nbt.getInt("DataVersion").orElse(1);
        if (version > MemoryDataVersion.CURRENT) {
            memory.unsupportedFutureVersion = version;
            memory.futureData = nbt.copy();
            memory.loadKnownStageSnapshots(nbt);
            EchoProtocol.LOGGER.error("Echo Protocol memory data version {} is newer than supported version {}; "
                    + "beta memory will remain read-only.", version, MemoryDataVersion.CURRENT);
            return memory;
        }
        memory.legacyImported = getBoolean(nbt, "LegacyImported");
        if (!containsType(nbt, "Players", NbtElement.LIST_TYPE)) {
            return memory;
        }
        NbtList list = getList(nbt, "Players");
        for (int index = 0; index < list.size(); index++) {
            try {
                NbtCompound entry = getCompound(list, index);
                UUID uuid = getUuid(entry, "Uuid");
                PlayerEchoStateSnapshotCodec.DecodeResult stageResult = containsType(entry, "Stage", NbtElement.COMPOUND_TYPE)
                        ? PlayerEchoStateSnapshotCodec.read(getCompound(entry, "Stage"))
                        : new PlayerEchoStateSnapshotCodec.DecodeResult(PlayerEchoStateSnapshot.empty(), List.of());
                PlayerMemoryState playerMemory = containsType(entry, "Memory", NbtElement.COMPOUND_TYPE)
                        ? PlayerMemoryStateCodec.read(getCompound(entry, "Memory")) : new PlayerMemoryState();
                stageResult.warnings().forEach(playerMemory::addLoadWarning);
                memory.players.put(uuid, memory.bind(new PlayerRecord(stageResult.snapshot(), playerMemory)));
            } catch (PlayerMemoryStateCodec.UnsupportedMemoryVersionException exception) {
                EchoProtocol.LOGGER.error("Skipping player memory entry {} with unsupported data version {}.",
                        index, exception.version());
            } catch (RuntimeException exception) {
                EchoProtocol.LOGGER.warn("Skipping malformed Echo Protocol player memory entry {}.", index, exception);
            }
        }
        return memory;
    }

    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (futureData != null) {
            return nbt.copyFrom(futureData);
        }
        nbt.putInt("DataVersion", MemoryDataVersion.CURRENT);
        nbt.putBoolean("LegacyImported", legacyImported);
        NbtList list = new NbtList();
        for (Map.Entry<UUID, PlayerRecord> player : players.entrySet()) {
            NbtCompound entry = new NbtCompound();
            putUuid(entry, "Uuid", player.getKey());
            entry.put("Stage", PlayerEchoStateSnapshotCodec.write(player.getValue().stage()));
            entry.put("Memory", PlayerMemoryStateCodec.write(player.getValue().memory()));
            list.add(entry);
        }
        nbt.put("Players", list);
        return nbt;
    }

    static PersistentEchoMemory fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        return fromNbt(nbt);
    }

    private NbtCompound toNbt() {
        return writeNbt(new NbtCompound(), null);
    }

    public PlayerRecord getOrCreate(UUID uuid, float initialContamination) {
        if (readOnly()) {
            return players.computeIfAbsent(uuid, ignored ->
                    new PlayerRecord(PlayerEchoStateSnapshot.empty(), new PlayerMemoryState(initialContamination)));
        }
        return players.computeIfAbsent(uuid, ignored -> {
            PlayerRecord record = bind(new PlayerRecord(PlayerEchoStateSnapshot.empty(),
                    new PlayerMemoryState(initialContamination)));
            markDirty();
            return record;
        });
    }

    public void put(UUID uuid, PlayerEchoStateSnapshot stage, PlayerMemoryState memory) {
        if (readOnly()) {
            return;
        }
        players.put(uuid, bind(new PlayerRecord(stage, memory)));
        markDirty();
    }

    public void updateStage(UUID uuid, PlayerEchoStateSnapshot stage) {
        if (readOnly()) {
            return;
        }
        PlayerRecord current = getOrCreate(uuid, 0.0F);
        players.put(uuid, bind(new PlayerRecord(stage, current.memory())));
        markDirty();
    }

    public boolean remove(UUID uuid) {
        if (readOnly()) {
            return false;
        }
        boolean removed = players.remove(uuid) != null;
        if (removed) {
            markDirty();
        }
        return removed;
    }

    public PlayerRecord record(UUID uuid) { return players.get(uuid); }
    public Map<UUID, PlayerRecord> records() { return Map.copyOf(players); }
    public boolean legacyImported() { return legacyImported; }
    public boolean readOnly() { return unsupportedFutureVersion > MemoryDataVersion.CURRENT; }
    public int unsupportedFutureVersion() { return unsupportedFutureVersion; }

    public void setLegacyImported() {
        if (!readOnly() && !legacyImported) {
            legacyImported = true;
            markDirty();
        }
    }

    private PlayerRecord bind(PlayerRecord record) {
        record.memory().setMutationListener(this::markDirty);
        return record;
    }

    private void loadKnownStageSnapshots(NbtCompound nbt) {
        if (!containsType(nbt, "Players", NbtElement.LIST_TYPE)) {
            return;
        }
        NbtList list = getList(nbt, "Players");
        for (int index = 0; index < list.size(); index++) {
            try {
                NbtCompound entry = getCompound(list, index);
                UUID uuid = getUuid(entry, "Uuid");
                PlayerEchoStateSnapshot stage = containsType(entry, "Stage", NbtElement.COMPOUND_TYPE)
                        ? PlayerEchoStateSnapshotCodec.read(getCompound(entry, "Stage")).snapshot()
                        : PlayerEchoStateSnapshot.empty();
                players.put(uuid, new PlayerRecord(stage, new PlayerMemoryState()));
            } catch (RuntimeException exception) {
                EchoProtocol.LOGGER.warn("Could not recover known Stage fields from future player entry {}.", index);
            }
        }
    }

    public record PlayerRecord(PlayerEchoStateSnapshot stage, PlayerMemoryState memory) {
        public PlayerRecord {
            stage = stage == null ? PlayerEchoStateSnapshot.empty() : stage;
            memory = memory == null ? new PlayerMemoryState() : memory;
        }
    }
}
