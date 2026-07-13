package dev.yeldos.echoprotocol.stage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class StageManager {
    private static final int DATA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, PlayerEchoState> states = new HashMap<>();
    private long tick;

    public void tick(MinecraftServer server, EchoConfig config) {
        tick++;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerEchoState state = state(player.getUuid());
            state.addPlayTick();
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
        }
    }

    public PlayerEchoState state(UUID playerUuid) {
        return states.computeIfAbsent(playerUuid, ignored -> new PlayerEchoState());
    }

    public void scheduleNextEvent(PlayerEchoState state, EchoConfig config) {
        int seconds = ThreadLocalRandom.current().nextInt(config.minimumEventIntervalSeconds(), config.maximumEventIntervalSeconds() + 1);
        state.setNextEventTick(tick + seconds * 20L);
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

    public void grant(ServerPlayerEntity player, String path) {
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
                state.setStageOneEvents(saved.stageOneEvents);
                state.setTotalEvents(saved.totalEvents);
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
            saved.stageOneEvents = state.stageOneEvents();
            saved.totalEvents = state.totalEvents();
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

    private static final class SaveData {
        int dataVersion;
        Map<String, SavedPlayer> players;
    }

    private static final class SavedPlayer {
        int stage;
        long playTicks;
        long nextEventTick;
        int stageOneEvents;
        int totalEvents;
    }
}
