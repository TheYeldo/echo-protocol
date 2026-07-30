package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.stage.PlayerEchoStateSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public final class PersistentMemoryManager {
    private PersistentEchoMemory persistent;
    private final Map<UUID, PlayerMemoryState> sessionOnlyMemories = new HashMap<>();

    public void load(MinecraftServer server, EchoConfig config) {
        persistent = server.getOverworld().getPersistentStateManager()
                .getOrCreate(PersistentEchoMemory.TYPE, PersistentEchoMemory.STORAGE_KEY);
        if (persistent.readOnly()) {
            EchoProtocol.LOGGER.error("Echo Protocol beta memory is disabled for this world because data version {} "
                    + "is newer than supported version {}.", persistent.unsupportedFutureVersion(), MemoryDataVersion.CURRENT);
            return;
        }
        if (!persistent.legacyImported()) {
            if (importLegacy(server, config)) {
                persistent.setLegacyImported();
            }
        }
        for (PersistentEchoMemory.PlayerRecord record : persistent.records().values()) {
            record.memory().applyBounds(config);
            record.memory().setMutationListener(persistent::markDirty);
        }
    }

    private boolean importLegacy(MinecraftServer server, EchoConfig config) {
        Path path = server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve("echo_protocol_state.json");
        if (!Files.exists(path)) {
            return true;
        }
        try {
            MemoryMigration.MigrationResult migration = MemoryMigration.migrateLegacyJson(Files.readString(path));
            migration.warnings().forEach(warning -> EchoProtocol.LOGGER.warn("Legacy memory migration: {}", warning));
            for (Map.Entry<UUID, PlayerEchoStateSnapshot> entry : migration.players().entrySet()) {
                if (persistent.record(entry.getKey()) == null) {
                    persistent.put(entry.getKey(), entry.getValue(),
                            new PlayerMemoryState(config.memoryContaminationInitial()));
                }
            }
            if (!migration.recoverable()) {
                EchoProtocol.LOGGER.error("Legacy Echo Protocol state could not be migrated; the original file was left untouched.");
                return false;
            }
            return true;
        } catch (IOException exception) {
            EchoProtocol.LOGGER.warn("Failed to read legacy Echo Protocol state; the original file was left untouched.", exception);
            return false;
        }
    }

    public PersistentEchoMemory.PlayerRecord getOrCreate(UUID uuid, EchoConfig config) {
        ensureLoaded();
        if (!config.persistentMemoryEnabled()) {
            PersistentEchoMemory.PlayerRecord saved = persistent.record(uuid);
            PlayerEchoStateSnapshot stage = saved == null ? PlayerEchoStateSnapshot.empty() : saved.stage();
            PlayerMemoryState session = sessionOnlyMemories.computeIfAbsent(uuid,
                    ignored -> new PlayerMemoryState(config.memoryContaminationInitial()));
            return new PersistentEchoMemory.PlayerRecord(stage, session);
        }
        return persistent.getOrCreate(uuid, config.memoryContaminationInitial());
    }

    public PersistentEchoMemory.PlayerRecord record(UUID uuid) {
        ensureLoaded();
        return persistent.record(uuid);
    }

    public Map<UUID, PersistentEchoMemory.PlayerRecord> records() {
        ensureLoaded();
        return persistent.records();
    }

    public boolean loaded() { return persistent != null; }

    public void updateStage(UUID uuid, PlayerEchoStateSnapshot snapshot) {
        ensureLoaded();
        persistent.updateStage(uuid, snapshot);
    }

    public boolean remove(UUID uuid) {
        ensureLoaded();
        boolean sessionRemoved = sessionOnlyMemories.remove(uuid) != null;
        return persistent.remove(uuid) || sessionRemoved;
    }

    public boolean readOnly() {
        return persistent != null && persistent.readOnly();
    }

    public void markDirty() {
        if (persistent != null && !persistent.readOnly()) {
            persistent.markDirty();
        }
    }

    public void clearReference() {
        persistent = null;
        sessionOnlyMemories.clear();
    }

    private void ensureLoaded() {
        if (persistent == null) {
            throw new IllegalStateException("persistent memory is not loaded");
        }
    }
}
