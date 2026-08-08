package dev.yeldos.echoprotocol.stage;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.config.EchoPresetManager;
import dev.yeldos.echoprotocol.memory.PersistentEchoMemory;
import dev.yeldos.echoprotocol.memory.PersistentMemoryManager;
import dev.yeldos.echoprotocol.memory.PlayerMemoryState;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class StageManager {
    private final Map<UUID, PlayerEchoState> states = new HashMap<>();
    private final PersistentMemoryManager persistentMemory = new PersistentMemoryManager();
    private long tick;

    public void tick(MinecraftServer server, EchoConfig config) {
        tick++;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerEchoState state = state(player.getUuid());
            state.addPlayTick();
            if (config.originalFamiliarLocationsEnabled() && state.playTicks() % (20L * 30L) == 0L) {
                recordFamiliarLocation(player, FamiliarLocationType.IDLE, player.getBlockPos(), config);
            }
            if (state.nextEventTick() <= 0) {
                scheduleNextEvent(state, config);
            }
            long minutes = state.playTicks() / (20L * 60L);
            if (state.stage() == EchoStage.OBSERVATION && minutes >= config.stageZeroMinutes()) {
                state.setStage(EchoStage.DEJA_VU);
            }
            if (state.stage().id() < EchoStage.CORRUPTED_MEMORY.id()
                    && (minutes >= config.stageTwoMinutes() || state.stageOneEvents() >= 4)) {
                state.setStage(EchoStage.CORRUPTED_MEMORY);
                grant(player, "corrupted_memory");
            }
            if (canUnlockStageThree(state, config)) {
                state.setStage(EchoStage.THE_ORIGINAL);
                state.setNextOriginalEventTick(tick + (long) config.originalFirstEventDelayMinutes() * 60L * 20L);
            }
        }
    }

    public PlayerEchoState state(UUID playerUuid) {
        return states.computeIfAbsent(playerUuid, uuid -> {
            PlayerEchoState state = new PlayerEchoState();
            if (persistentMemory.loaded()) {
                PersistentEchoMemory.PlayerRecord record = persistentMemory.getOrCreate(uuid, EchoProtocol.config());
                state.restore(record.stage(), tick);
                bindPersistentState(uuid, state);
            }
            return state;
        });
    }

    public PlayerMemoryState memory(UUID playerUuid) {
        return persistentMemory.getOrCreate(playerUuid, EchoProtocol.config()).memory();
    }

    public void scheduleNextEvent(PlayerEchoState state, EchoConfig config) {
        int minimum = EchoPresetManager.eventIntervalSeconds(config.minimumEventIntervalSeconds(), config);
        int maximum = Math.max(minimum,
                EchoPresetManager.eventIntervalSeconds(config.maximumEventIntervalSeconds(), config));
        int seconds = ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
        state.setNextEventTick(tick + seconds * 20L);
    }

    public void scheduleNextOriginalEvent(PlayerEchoState state, EchoConfig config) {
        float activity = EchoPresetManager.values(config).originalFinaleWeightMultiplier();
        int minimum = Math.max(5, Math.round(config.originalMinimumEventIntervalMinutes() / activity));
        int maximum = Math.max(minimum,
                Math.round(config.originalMaximumEventIntervalMinutes() / activity));
        int minutes = ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
        state.setNextOriginalEventTick(tick + minutes * 60L * 20L);
    }

    public long tick() {
        return tick;
    }

    public void markJoin(ServerPlayerEntity player) {
        state(player.getUuid()).setLastJoinTick(tick);
    }

    public void markRespawn(ServerPlayerEntity player) {
        state(player.getUuid()).setLastRespawnTick(tick);
    }

    public void clear(UUID playerUuid) {
        states.remove(playerUuid);
        if (persistentMemory.loaded()) {
            persistentMemory.remove(playerUuid);
        }
    }

    public void recordFamiliarLocation(ServerPlayerEntity player, FamiliarLocationType type, BlockPos pos, EchoConfig config) {
        if (!config.originalFamiliarLocationsEnabled()) {
            return;
        }
        PlayerEchoState state = state(player.getUuid());
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        for (FamiliarLocation location : state.familiarLocations()) {
            if (location.canMerge(type, dimension, pos)) {
                location.markSeen(tick);
                state.markPersistentChanged();
                return;
            }
        }
        state.familiarLocations().add(new FamiliarLocation(type, dimension, pos.toImmutable(), 1, tick));
        trimFamiliarLocations(state, config);
        state.markPersistentChanged();
    }

    public int addCurrentFamiliarLocation(ServerPlayerEntity player, EchoConfig config) {
        recordFamiliarLocation(player, FamiliarLocationType.MANUAL, player.getBlockPos(), config);
        return state(player.getUuid()).familiarLocations().size();
    }

    public int clearFamiliarLocations(ServerPlayerEntity player) {
        PlayerEchoState state = state(player.getUuid());
        int count = state.familiarLocations().size();
        state.familiarLocations().clear();
        if (count > 0) {
            state.markPersistentChanged();
        }
        return count;
    }

    public List<FamiliarLocation> familiarLocations(ServerPlayerEntity player) {
        return List.copyOf(state(player.getUuid()).familiarLocations());
    }

    public void grant(ServerPlayerEntity player, String path) {
        if ("not_me".equals(path) || "do_not_look_away".equals(path)) {
            state(player.getUuid()).setMimicIndependentActionSeen(true);
        }
        AdvancementEntry root = player.getServer().getAdvancementLoader().get(EchoProtocol.id("root"));
        if (root != null) {
            player.getAdvancementTracker().grantCriterion(root, "trigger");
        }
        AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(EchoProtocol.id(path));
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "trigger");
        }
    }

    public void load(MinecraftServer server) {
        states.clear();
        tick = 0L;
        persistentMemory.load(server, EchoProtocol.config());
        for (Map.Entry<UUID, PersistentEchoMemory.PlayerRecord> entry : persistentMemory.records().entrySet()) {
            PlayerEchoState state = new PlayerEchoState();
            state.restore(entry.getValue().stage(), tick);
            bindPersistentState(entry.getKey(), state);
            states.put(entry.getKey(), state);
        }
    }

    public void save(MinecraftServer server) {
        for (Map.Entry<UUID, PlayerEchoState> entry : states.entrySet()) {
            persistentMemory.updateStage(entry.getKey(), entry.getValue().snapshot(tick));
        }
    }

    static long remainingDelay(long currentTick, long deadline) {
        return deadline <= 0L ? 0L : Math.max(1L, deadline - currentTick);
    }

    static long restoreDeadline(long currentTick, long remainingDelay) {
        return remainingDelay <= 0L ? 0L : currentTick + remainingDelay;
    }

    private boolean canUnlockStageThree(PlayerEchoState state, EchoConfig config) {
        if (!config.stageThreeEnabled() || !config.originalEnabled() || state.stage() != EchoStage.CORRUPTED_MEMORY) {
            return false;
        }
        long requiredTicks = (long) config.stageThreeRequiredPlaytimeMinutes() * 60L * 20L;
        return state.playTicks() >= requiredTicks
                && state.memoryEvents() >= config.stageThreeRequiredMemoryEvents()
                && state.corruptedEvents() >= config.stageThreeRequiredCorruptedEvents()
                && state.mimicEvents() >= config.stageThreeRequiredMimicEvents()
                && state.mimicIndependentActionSeen();
    }

    private static void trimFamiliarLocations(PlayerEchoState state, EchoConfig config) {
        int limit = config.originalMaximumFamiliarLocations();
        state.familiarLocations().sort(Comparator
                .comparingInt(FamiliarLocation::visits)
                .thenComparingLong(FamiliarLocation::lastSeenTick)
                .reversed());
        while (state.familiarLocations().size() > limit) {
            state.familiarLocations().remove(state.familiarLocations().size() - 1);
        }
    }

    private void bindPersistentState(UUID uuid, PlayerEchoState state) {
        state.setPersistentMutationListener(() -> persistentMemory.updateStage(uuid, state.snapshot(tick)));
    }
}
