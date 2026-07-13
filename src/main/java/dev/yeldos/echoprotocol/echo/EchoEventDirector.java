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
import dev.yeldos.echoprotocol.stage.FamiliarLocation;
import dev.yeldos.echoprotocol.stage.FamiliarLocationType;
import dev.yeldos.echoprotocol.stage.PlayerEchoState;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.item.ItemStack;
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
            if (state.stage() == EchoStage.THE_ORIGINAL && config.originalEnabled()) {
                if (state.nextOriginalEventTick() <= 0L) {
                    stageManager.scheduleNextOriginalEvent(state, config);
                } else if (stageManager.tick() >= state.nextOriginalEventTick() && canRunEvent(player, state)) {
                    boolean spawnedOriginal = spawnOriginal(player, false, null, config);
                    stageManager.scheduleNextOriginalEvent(state, config);
                    if (spawnedOriginal) {
                        continue;
                    }
                }
            }
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
        state.incrementEchoEvent(type);
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
                    && state.stage().id() >= EchoStage.CORRUPTED_MEMORY.id()
                    && state.stageOneEvents() >= 2;
            case MIMIC -> config.mimicEchoEnabled()
                    && state.stage().id() >= EchoStage.CORRUPTED_MEMORY.id()
                    && state.playTicks() >= (long) (config.stageTwoMinutes() + config.mimicMinimumStageTwoMinutes()) * 60L * 20L
                    && !mimicSpawnedThisSession.contains(target.getUuid())
                    && stageManager.tick() - lastGlobalMimicTick >= (long) config.mimicSessionCooldownMinutes() * 60L * 20L
                    && stageManager.tick() - lastMimicTick.getOrDefault(target.getUuid(), -9999999L) >= (long) config.mimicSessionCooldownMinutes() * 60L * 20L;
            case ORIGINAL -> config.originalEnabled() && state.stage() == EchoStage.THE_ORIGINAL;
        };
    }

    private static EchoBehaviorController behaviorFor(EchoType type, EchoEventContext context) {
        return switch (type) {
            case MEMORY -> new MemoryEchoBehavior();
            case CORRUPTED -> new CorruptedEchoBehavior(context);
            case MIMIC -> new MimicEchoBehavior(context);
            case ORIGINAL -> throw new IllegalArgumentException("Original Echo uses spawnOriginal.");
        };
    }

    public boolean spawnOriginal(ServerPlayerEntity target, boolean forced, OriginalEventKind requestedEvent, EchoConfig config) {
        if (!config.originalEnabled()) {
            return false;
        }
        cleanupActiveEchoes();
        PlayerEchoState state = stageManager.state(target.getUuid());
        if (!forced && state.stage() != EchoStage.THE_ORIGINAL) {
            return false;
        }
        if (state.activeEvent()) {
            if (!forced) {
                return false;
            }
            stopEvents(target);
        }
        if (activeOriginalCount(target) >= config.originalMaximumActivePerPlayer()) {
            return false;
        }
        ServerWorld world = target.getServerWorld();
        FamiliarLocation location = chooseOriginalLocation(target, config).orElse(null);
        Vec3d anchor = location == null ? target.getPos() : location.pos().toCenterPos();
        OriginalEventKind eventKind = requestedEvent == null ? chooseOriginalEvent(location) : requestedEvent;
        Optional<Vec3d> spawnPos = SafeEchoPositionFinder.findSpawn(world, target, anchor, config);
        if (spawnPos.isEmpty()) {
            spawnPos = SafeEchoPositionFinder.findSpawn(world, target,
                    target.getPos().subtract(target.getRotationVec(1.0F).multiply(config.minimumEchoSpawnDistance())), config);
        }
        if (spawnPos.isEmpty()) {
            return false;
        }

        PlayerRecording recording = recordingManager.get(target.getUuid());
        RecordedFrame frame = recording != null && recording.latest() != null
                ? recording.latest()
                : RecordedFrame.capture(target, stageManager.tick(), null);
        ItemStack heldItem = chooseFamiliarItem(recording, target);
        EchoEntity echo = new EchoEntity(EchoEntities.ECHO, world);
        EchoEventContext context = new EchoEventContext(target.getUuid(), config, stageManager, false);
        EchoBehaviorController behavior = new OriginalEchoBehavior(context, eventKind, anchor, heldItem);
        echo.configure(target.getUuid(), config.sharedEchoes(), List.of(frame), config.recordingSampleIntervalTicks(), config, context, behavior);
        Vec3d pos = spawnPos.get();
        echo.refreshPositionAndAngles(pos.x, pos.y, pos.z, target.bodyYaw + 180.0F, target.getPitch());
        echo.setHeldItemVisual(heldItem);
        world.spawnEntity(echo);
        activeEchoes.computeIfAbsent(target.getUuid(), ignored -> new ArrayList<>()).add(echo);
        state.setActiveEvent(true);
        state.incrementTotalEvents();
        state.incrementEchoEvent(EchoType.ORIGINAL);
        EchoSoundPlayer.playSpawnProfile(target, EchoType.ORIGINAL, config, pos);
        stageManager.grant(target, "my_place");
        if (eventKind == OriginalEventKind.ALREADY_HOME || eventKind == OriginalEventKind.YOUR_BED) {
            stageManager.grant(target, "already_home");
        }
        return true;
    }

    public boolean spawnOriginalConfrontation(ServerPlayerEntity target, EchoConfig config) {
        return spawnOriginal(target, true, OriginalEventKind.CONFRONTATION, config);
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

    private int activeOriginalCount(ServerPlayerEntity target) {
        List<EchoEntity> echoes = activeEchoes.getOrDefault(target.getUuid(), List.of());
        int count = 0;
        for (EchoEntity echo : echoes) {
            if (!echo.isRemoved() && echo.echoType() == EchoType.ORIGINAL) {
                count++;
            }
        }
        return count;
    }

    private Optional<FamiliarLocation> chooseOriginalLocation(ServerPlayerEntity target, EchoConfig config) {
        if (!config.originalFamiliarLocationsEnabled()) {
            return Optional.empty();
        }
        String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
        return stageManager.familiarLocations(target).stream()
                .filter(location -> location.dimension().equals(dimension))
                .filter(location -> target.getServerWorld().isChunkLoaded(location.pos()))
                .filter(location -> location.pos().getSquaredDistance(target.getBlockPos()) <= 96.0D * 96.0D)
                .sorted((left, right) -> Integer.compare(right.visits(), left.visits()))
                .findFirst();
    }

    private static OriginalEventKind chooseOriginalEvent(FamiliarLocation location) {
        if (ThreadLocalRandom.current().nextInt(12) == 0) {
            return OriginalEventKind.CONFRONTATION;
        }
        if (location == null) {
            return OriginalEventKind.WAITING;
        }
        FamiliarLocationType type = location.type();
        return switch (type) {
            case BED -> OriginalEventKind.YOUR_BED;
            case CHEST -> OriginalEventKind.WRONG_OWNER;
            case CRAFTING, FURNACE, IDLE, MANUAL, HOME -> OriginalEventKind.OCCUPIED_PLACE;
            case DOORWAY -> OriginalEventKind.EMPTY_ROOM;
            case PORTAL, MINE_ENTRANCE -> OriginalEventKind.EARLIER_THAN_YOU;
        };
    }

    private static ItemStack chooseFamiliarItem(PlayerRecording recording, ServerPlayerEntity target) {
        if (recording != null) {
            List<RecordedFrame> frames = recording.frames();
            for (int i = frames.size() - 1; i >= 0; i--) {
                ItemStack stack = frames.get(i).heldItemVisual();
                if (!stack.isEmpty()) {
                    return stack.copyWithCount(1);
                }
            }
        }
        return target.getMainHandStack().copyWithCount(Math.min(1, target.getMainHandStack().getCount()));
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
