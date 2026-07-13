package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.entity.EchoEntities;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.recording.PlayerRecording;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.recording.SoundMarker;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import dev.yeldos.echoprotocol.stage.EchoStage;
import dev.yeldos.echoprotocol.stage.PlayerEchoState;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class EchoEventDirector {
    private final RecordingManager recordingManager;
    private final StageManager stageManager;
    private final Map<UUID, List<EchoEntity>> activeEchoes = new HashMap<>();
    private final Map<UUID, Long> lastMimicTick = new HashMap<>();
    private final List<UUID> mimicSpawnedThisSession = new ArrayList<>();
    private long lastGlobalMimicTick = -9999999L;

    public EchoEventDirector(RecordingManager recordingManager, StageManager stageManager) {
        this.recordingManager = recordingManager;
        this.stageManager = stageManager;
    }

    public void tick(MinecraftServer server, EchoConfig config) {
        cleanupActiveEchoes();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerEchoState state = stageManager.state(player.getUuid());
            if (state.stage() == EchoStage.OBSERVATION || !canRunEvent(player, state)) {
                continue;
            }
            if (stageManager.tick() >= state.nextEventTick()) {
                EchoType type = chooseEchoType(player, config, false);
                boolean spawned = spawnEcho(player, type, false, false, config);
                stageManager.scheduleNextEvent(state, config);
                if (spawned && type == EchoType.MEMORY && state.stage() == EchoStage.DEJA_VU) {
                    state.incrementStageOneEvents();
                }
            }
        }
    }

    public boolean spawnReplay(ServerPlayerEntity target, boolean forced, EchoConfig config) {
        return spawnEcho(target, EchoType.MEMORY, forced, false, config);
    }

    public boolean spawnEcho(ServerPlayerEntity target, EchoType type, boolean forced, boolean forcedHostile, EchoConfig config) {
        PlayerRecording recording = recordingManager.get(target.getUuid());
        if (recording == null) {
            return false;
        }
        cleanupActiveEchoes();
        PlayerEchoState state = stageManager.state(target.getUuid());
        if (state.activeEvent()) {
            if (!forced) {
                return false;
            }
            stopEvents(target);
        }
        if (!forced && isTooCloseToOtherPlayer(target, config)) {
            return false;
        }
        if (!forced && !canSpawnType(target, type, config)) {
            type = EchoType.MEMORY;
        }
        int minFrames = Math.max(2, config.minimumReplaySeconds() * 20 / config.recordingSampleIntervalTicks());
        int maxFrames = Math.max(minFrames, config.maximumReplaySeconds() * 20 / config.recordingSampleIntervalTicks());
        List<RecordedFrame> segment = recording.randomSegment(minFrames, maxFrames);
        if (segment.isEmpty()) {
            return false;
        }
        RecordedFrame start = segment.get(0);
        ServerWorld world = target.getServerWorld();
        Optional<Vec3d> spawnPos = SafeEchoPositionFinder.findSpawn(world, target, start.pos(), config);
        if (spawnPos.isEmpty()) {
            return false;
        }
        EchoEntity echo = new EchoEntity(EchoEntities.ECHO, world);
        EchoEventContext context = new EchoEventContext(target.getUuid(), config, stageManager, forcedHostile);
        EchoBehaviorController behavior = behaviorFor(type, context);
        List<RecordedFrame> frames = type == EchoType.CORRUPTED ? corruptSegment(segment, target) : segment;
        echo.configure(target.getUuid(), config.sharedEchoes(), frames, config.recordingSampleIntervalTicks(), config, context, behavior);
        Vec3d pos = spawnPos.get();
        echo.refreshPositionAndAngles(pos.x, pos.y, pos.z, start.bodyYaw(), start.pitch());
        world.spawnEntity(echo);
        activeEchoes.computeIfAbsent(target.getUuid(), ignored -> new ArrayList<>()).add(echo);
        state.setActiveEvent(true);
        if (type == EchoType.MIMIC) {
            lastMimicTick.put(target.getUuid(), stageManager.tick());
            lastGlobalMimicTick = stageManager.tick();
            if (!mimicSpawnedThisSession.contains(target.getUuid())) {
                mimicSpawnedThisSession.add(target.getUuid());
            }
        }
        EchoSoundPlayer.playSpawnProfile(target, type, config, pos);

        state.incrementTotalEvents();
        stageManager.grant(target, "deja_vu");
        if (type == EchoType.MEMORY) {
            stageManager.grant(target, "that_was_me");
        }
        if (config.realPlayerSkins() && target.getGameProfile().getProperties().containsKey("textures")) {
            stageManager.grant(target, "familiar_face");
        }
        if (forced || ThreadLocalRandom.current().nextInt(6) == 0) {
            playMemorySound(target, recording, config);
        }
        if (config.torchFlicker() || config.echoLightEffects()) {
            spawnTargetedParticles(target, new Vec3d(start.x(), start.y() + 1.1D, start.z()));
        }
        if (state.stage() == EchoStage.CORRUPTED_MEMORY && config.chatEchoes()) {
            maybeEchoChat(target, recording);
        }
        return true;
    }

    private boolean canRunEvent(ServerPlayerEntity player, PlayerEchoState state) {
        long now = stageManager.tick();
        if (now - state.lastJoinTick() < 20L * 60L || now - state.lastRespawnTick() < 20L * 30L) {
            return false;
        }
        return !player.isSleeping() && !state.activeEvent() && !player.isSpectator();
    }

    private static boolean isTooCloseToOtherPlayer(ServerPlayerEntity target, EchoConfig config) {
        if (config.sharedEchoes()) {
            return false;
        }
        for (ServerPlayerEntity other : target.getServerWorld().getPlayers()) {
            if (other != target && !other.isSpectator() && other.squaredDistanceTo(target) < 24.0D * 24.0D) {
                return true;
            }
        }
        return false;
    }

    private static List<RecordedFrame> corruptSegment(List<RecordedFrame> original, ServerPlayerEntity target) {
        if (original.size() < 4) {
            return original;
        }
        int trim = ThreadLocalRandom.current().nextInt(0, Math.max(1, original.size() / 3));
        List<RecordedFrame> frames = original.subList(0, original.size() - trim);
        if (ThreadLocalRandom.current().nextBoolean()) {
            return frames.reversed();
        }
        return frames;
    }

    private EchoType chooseEchoType(ServerPlayerEntity target, EchoConfig config, boolean forced) {
        List<EchoType> types = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        addType(target, config, types, weights, EchoType.MEMORY, config.memoryEchoEnabled(), config.memoryEchoWeight());
        addType(target, config, types, weights, EchoType.CORRUPTED, config.corruptedEchoEnabled(), config.corruptedEchoWeight());
        addType(target, config, types, weights, EchoType.MIMIC, config.mimicEchoEnabled(), config.mimicEchoWeight());
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        if (total <= 0 || types.isEmpty()) {
            return EchoType.MEMORY;
        }
        int pick = ThreadLocalRandom.current().nextInt(total);
        for (int i = 0; i < types.size(); i++) {
            pick -= weights.get(i);
            if (pick < 0) {
                return types.get(i);
            }
        }
        return EchoType.MEMORY;
    }

    private void addType(ServerPlayerEntity target, EchoConfig config, List<EchoType> types, List<Integer> weights, EchoType type, boolean enabled, int weight) {
        if (enabled && weight > 0 && canSpawnType(target, type, config)) {
            types.add(type);
            weights.add(weight);
        }
    }

    private boolean canSpawnType(ServerPlayerEntity target, EchoType type, EchoConfig config) {
        PlayerEchoState state = stageManager.state(target.getUuid());
        return switch (type) {
            case MEMORY -> config.memoryEchoEnabled() && state.stage().id() >= EchoStage.DEJA_VU.id();
            case CORRUPTED -> config.corruptedEchoEnabled()
                    && state.stage() == EchoStage.CORRUPTED_MEMORY
                    && state.stageOneEvents() >= 2;
            case MIMIC -> config.mimicEchoEnabled()
                    && state.stage() == EchoStage.CORRUPTED_MEMORY
                    && state.playTicks() >= (long) (config.stageTwoMinutes() + config.mimicMinimumStageTwoMinutes()) * 60L * 20L
                    && !mimicSpawnedThisSession.contains(target.getUuid())
                    && stageManager.tick() - lastGlobalMimicTick >= (long) config.mimicSessionCooldownMinutes() * 60L * 20L
                    && stageManager.tick() - lastMimicTick.getOrDefault(target.getUuid(), -9999999L) >= (long) config.mimicSessionCooldownMinutes() * 60L * 20L;
        };
    }

    private static EchoBehaviorController behaviorFor(EchoType type, EchoEventContext context) {
        return switch (type) {
            case MEMORY -> new MemoryEchoBehavior();
            case CORRUPTED -> new CorruptedEchoBehavior(context);
            case MIMIC -> new MimicEchoBehavior(context);
        };
    }

    public int stopEvents(ServerPlayerEntity target) {
        List<EchoEntity> echoes = activeEchoes.remove(target.getUuid());
        if (echoes == null) {
            stageManager.state(target.getUuid()).setActiveEvent(false);
            return 0;
        }
        int stopped = 0;
        for (EchoEntity echo : echoes) {
            if (!echo.isRemoved()) {
                echo.finishAndDiscard();
                stopped++;
            }
        }
        stageManager.state(target.getUuid()).setActiveEvent(false);
        return stopped;
    }

    public boolean setMimicHostile(ServerPlayerEntity target, boolean hostile) {
        cleanupActiveEchoes();
        List<EchoEntity> echoes = activeEchoes.getOrDefault(target.getUuid(), List.of());
        for (EchoEntity echo : echoes) {
            if (!echo.isRemoved() && echo.echoType() == EchoType.MIMIC && echo.behavior() != null) {
                echo.behavior().forceHostile(hostile);
                return true;
            }
        }
        return false;
    }

    public void playDebugSound(ServerPlayerEntity target, EchoType type) {
        EchoSoundPlayer.playSpawnProfile(target, type, EchoProtocol.config(), target.getPos());
    }

    private void cleanupActiveEchoes() {
        activeEchoes.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(EchoEntity::isRemoved);
            if (entry.getValue().isEmpty()) {
                stageManager.state(entry.getKey()).setActiveEvent(false);
                return true;
            }
            return false;
        });
    }

    private static void playMemorySound(ServerPlayerEntity target, PlayerRecording recording, EchoConfig config) {
        if (!config.soundEchoes()) {
            return;
        }
        List<SoundMarker> markers = recording.soundMarkers();
        if (markers.isEmpty()) {
            return;
        }
        SoundMarker marker = markers.get(ThreadLocalRandom.current().nextInt(markers.size()));
        if (target.squaredDistanceTo(marker.x(), marker.y(), marker.z()) > 64.0D * 64.0D) {
            return;
        }
        if (config.sharedEchoes()) {
            target.getServerWorld().playSound(null, marker.x(), marker.y(), marker.z(), marker.sound(), SoundCategory.PLAYERS,
                    Math.min(marker.volume(), 0.6F), marker.pitch());
        } else {
            target.playSoundToPlayer(marker.sound(), SoundCategory.PLAYERS, Math.min(marker.volume(), 0.6F), marker.pitch());
        }
    }

    private static void spawnTargetedParticles(ServerPlayerEntity player, Vec3d pos) {
        player.getServerWorld().spawnParticles(player, ParticleTypes.SCULK_SOUL, true, pos.x, pos.y, pos.z,
                8, 0.15D, 0.35D, 0.15D, 0.01D);
    }

    private static void maybeEchoChat(ServerPlayerEntity target, PlayerRecording recording) {
        if (ThreadLocalRandom.current().nextInt(4) != 0) {
            return;
        }
        String message = recording.randomChat();
        if (!message.isBlank()) {
            target.sendMessage(Text.translatable("text.echoprotocol.chat_echo", target.getGameProfile().getName(), message), false);
        }
    }

    public void clear() {
        for (List<EchoEntity> echoes : activeEchoes.values()) {
            for (EchoEntity echo : echoes) {
                if (!echo.isRemoved()) {
                    echo.finishAndDiscard();
                }
            }
        }
        activeEchoes.clear();
        lastMimicTick.clear();
        mimicSpawnedThisSession.clear();
        lastGlobalMimicTick = -9999999L;
    }
}
