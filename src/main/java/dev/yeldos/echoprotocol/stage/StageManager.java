package dev.yeldos.echoprotocol.stage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class StageManager {
    private static final int DATA_VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, PlayerEchoState> states = new HashMap<>();
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
                grant(player, "the_original");
            }
        }
    }

    public PlayerEchoState state(UUID playerUuid) {
        return states.computeIfAbsent(playerUuid, ignored -> new PlayerEchoState());
    }

    public void scheduleNextEvent(PlayerEchoState state, EchoConfig config) {
        int seconds = ThreadLocalRandom.current().nextInt(config.minimumEventIntervalSeconds(), config.maximumEventIntervalSeconds() + 1);
        state.setNextEventTick(tick + seconds * 20L);
    }

    public void scheduleNextOriginalEvent(PlayerEchoState state, EchoConfig config) {
        int minutes = ThreadLocalRandom.current().nextInt(config.originalMinimumEventIntervalMinutes(),
                config.originalMaximumEventIntervalMinutes() + 1);
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
    }

    public void recordFamiliarLocation(ServerPlayerEntity player, FamiliarLocationType type, BlockPos pos, EchoConfig config) {
        if (!config.originalFamiliarLocationsEnabled()) {
            return;
        }
        PlayerEchoState state = state(player.getUuid());
        String dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        for (FamiliarLocation location : state.familiarLocations()) {
            if (location.canMerge(type, dimension, pos)) {
                location.markSeen(tick);
                return;
            }
        }
        state.familiarLocations().add(new FamiliarLocation(type, dimension, pos.toImmutable(), 1, tick));
        trimFamiliarLocations(state, config);
    }

    public int addCurrentFamiliarLocation(ServerPlayerEntity player, EchoConfig config) {
        recordFamiliarLocation(player, FamiliarLocationType.MANUAL, player.getBlockPos(), config);
        return state(player.getUuid()).familiarLocations().size();
    }

    public int clearFamiliarLocations(ServerPlayerEntity player) {
        PlayerEchoState state = state(player.getUuid());
        int count = state.familiarLocations().size();
        state.familiarLocations().clear();
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
        Path path = dataPath(server);
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            SaveData data = GSON.fromJson(reader, SaveData.class);
            if (data == null || data.players == null) {
                return;
            }
            states.clear();
            for (Map.Entry<String, SavedPlayer> entry : data.players.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                SavedPlayer saved = entry.getValue();
                PlayerEchoState state = new PlayerEchoState();
                state.setStage(EchoStage.fromId(saved.stage));
                state.setPlayTicks(saved.playTicks);
                state.setNextEventTick(saved.nextEventTick);
                state.setNextOriginalEventTick(saved.nextOriginalEventTick);
                state.setStageOneEvents(saved.stageOneEvents);
                state.setTotalEvents(saved.totalEvents);
                state.setMemoryEvents(saved.memoryEvents);
                state.setCorruptedEvents(saved.corruptedEvents);
                state.setMimicEvents(saved.mimicEvents);
                state.setOriginalEvents(saved.originalEvents);
                state.setMimicIndependentActionSeen(saved.mimicIndependentActionSeen);
                if (saved.familiarLocations != null) {
                    for (SavedFamiliarLocation savedLocation : saved.familiarLocations) {
                        FamiliarLocation location = savedLocation.toLocation();
                        if (location != null) {
                            state.familiarLocations().add(location);
                        }
                    }
                }
                states.put(uuid, state);
            }
        } catch (RuntimeException | IOException exception) {
            EchoProtocol.LOGGER.warn("Failed to load Echo Protocol world data; starting with empty state.", exception);
        }
    }

    public void save(MinecraftServer server) {
        Path path = dataPath(server);
        SaveData data = new SaveData();
        data.dataVersion = DATA_VERSION;
        data.players = new HashMap<>();
        for (Map.Entry<UUID, PlayerEchoState> entry : states.entrySet()) {
            PlayerEchoState state = entry.getValue();
            SavedPlayer saved = new SavedPlayer();
            saved.stage = state.stage().id();
            saved.playTicks = state.playTicks();
            saved.nextEventTick = state.nextEventTick();
            saved.nextOriginalEventTick = state.nextOriginalEventTick();
            saved.stageOneEvents = state.stageOneEvents();
            saved.totalEvents = state.totalEvents();
            saved.memoryEvents = state.memoryEvents();
            saved.corruptedEvents = state.corruptedEvents();
            saved.mimicEvents = state.mimicEvents();
            saved.originalEvents = state.originalEvents();
            saved.mimicIndependentActionSeen = state.mimicIndependentActionSeen();
            saved.familiarLocations = state.familiarLocations().stream().map(SavedFamiliarLocation::from).toList();
            data.players.put(entry.getKey().toString(), saved);
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            EchoProtocol.LOGGER.warn("Failed to save Echo Protocol world data.", exception);
        }
    }

    private static Path dataPath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve("echo_protocol_state.json");
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

    private static final class SaveData {
        int dataVersion;
        Map<String, SavedPlayer> players;
    }

    private static final class SavedPlayer {
        int stage;
        long playTicks;
        long nextEventTick;
        long nextOriginalEventTick;
        int stageOneEvents;
        int totalEvents;
        int memoryEvents;
        int corruptedEvents;
        int mimicEvents;
        int originalEvents;
        boolean mimicIndependentActionSeen;
        List<SavedFamiliarLocation> familiarLocations;
    }

    private static final class SavedFamiliarLocation {
        String type;
        String dimension;
        int x;
        int y;
        int z;
        int visits;
        long lastSeenTick;

        static SavedFamiliarLocation from(FamiliarLocation location) {
            SavedFamiliarLocation saved = new SavedFamiliarLocation();
            saved.type = location.type().name();
            saved.dimension = location.dimension();
            BlockPos pos = location.pos();
            saved.x = pos.getX();
            saved.y = pos.getY();
            saved.z = pos.getZ();
            saved.visits = location.visits();
            saved.lastSeenTick = location.lastSeenTick();
            return saved;
        }

        FamiliarLocation toLocation() {
            try {
                return new FamiliarLocation(FamiliarLocationType.valueOf(type), dimension, new BlockPos(x, y, z), visits, lastSeenTick);
            } catch (RuntimeException exception) {
                return null;
            }
        }
    }
}
