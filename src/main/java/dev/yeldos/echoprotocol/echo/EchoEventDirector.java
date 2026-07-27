package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.audio.AudioResidueEvent;
import dev.yeldos.echoprotocol.audio.AudioResidueManager;
import dev.yeldos.echoprotocol.audio.AudioResidue;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.director.AdaptiveEventSelector;
import dev.yeldos.echoprotocol.director.EchoEventHistory;
import dev.yeldos.echoprotocol.director.EventCategory;
import dev.yeldos.echoprotocol.entity.EchoEntities;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryBehavior;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryContext;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryDirector;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryPlan;
import dev.yeldos.echoprotocol.habit.HabitType;
import dev.yeldos.echoprotocol.habit.PlayerHabitSummary;
import dev.yeldos.echoprotocol.habit.PlayerHabitTracker;
import dev.yeldos.echoprotocol.panic.PanicImprint;
import dev.yeldos.echoprotocol.panic.PanicImprintManager;
import dev.yeldos.echoprotocol.panic.PanicTriggerType;
import dev.yeldos.echoprotocol.peripheral.PeripheralEchoBehavior;
import dev.yeldos.echoprotocol.original.OriginalMovementMode;
import dev.yeldos.echoprotocol.recording.PlayerRecording;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.recording.ReplayFrames;
import dev.yeldos.echoprotocol.recording.SoundMarker;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import dev.yeldos.echoprotocol.stage.EchoStage;
import dev.yeldos.echoprotocol.stage.FamiliarLocation;
import dev.yeldos.echoprotocol.stage.FamiliarLocationType;
import dev.yeldos.echoprotocol.stage.PlayerEchoState;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
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
    private final FalseMemoryDirector falseMemoryDirector;
    private final PanicImprintManager panicImprints;
    private final AudioResidueManager audioResidues;
    private final PlayerHabitTracker habits = new PlayerHabitTracker();
    private final EchoEventHistory eventHistory = new EchoEventHistory();
    private final AdaptiveEventSelector selector = new AdaptiveEventSelector();
    private final Map<UUID, List<EchoEntity>> activeEchoes = new HashMap<>();
    private final Map<UUID, Long> lastMimicTick = new HashMap<>();
    private final List<UUID> mimicSpawnedThisSession = new ArrayList<>();
    private final Map<UUID, Integer> peripheralSessionCounts = new HashMap<>();
    private final Map<UUID, Long> lastPeripheralTicks = new HashMap<>();
    private final Map<UUID, String> dimensions = new HashMap<>();
    private final Map<UUID, Long> dimensionChangeTicks = new HashMap<>();
    private final Map<UUID, Long> lastSleepTicks = new HashMap<>();
    private long lastGlobalMimicTick = -9999999L;

    public EchoEventDirector(RecordingManager recordingManager, StageManager stageManager) {
        this.recordingManager = recordingManager;
        this.stageManager = stageManager;
        this.falseMemoryDirector = new FalseMemoryDirector(stageManager);
        this.panicImprints = new PanicImprintManager(recordingManager);
        this.audioResidues = new AudioResidueManager(stageManager);
    }

    public void tick(MinecraftServer server, EchoConfig config) {
        cleanupActiveEchoes();
        panicImprints.tick(server, config, stageManager.tick());
        habits.tick(server, config, stageManager.tick());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            noteDimension(player);
            PlayerRecording currentRecording = recordingManager.get(player.getUuid());
            if (config.audioResidueEnabled() && stageManager.tick() % 8L == 0L
                    && currentRecording != null && currentRecording.latest() != null
                    && currentRecording.latest().walking() && currentRecording.latest().onGround()) {
                audioResidues.capture(player, player.getBlockPos(), SoundEvents.BLOCK_STONE_STEP,
                        0.3F, 0.85F, AudioResidueEvent.FOOTSTEP, config, stageManager.tick());
            }
            PlayerEchoState state = stageManager.state(player.getUuid());
            if (state.stage() == EchoStage.THE_ORIGINAL && config.originalEnabled()) {
                if (state.nextOriginalEventTick() <= 0L) {
                    stageManager.scheduleNextOriginalEvent(state, config);
                } else if (stageManager.tick() >= state.nextOriginalEventTick() && canRunEvent(player, state, config)) {
                    boolean spawnedOriginal = spawnOriginal(player, false, null, config);
                    stageManager.scheduleNextOriginalEvent(state, config);
                    if (spawnedOriginal) {
                        continue;
                    }
                }
            }
            if (state.stage() == EchoStage.OBSERVATION || !canRunEvent(player, state, config)) {
                continue;
            }
            if (stageManager.tick() >= state.nextEventTick()) {
                EventCategory category = chooseEvent(player, config);
                boolean spawned = spawnDirectedEvent(player, category, config);
                stageManager.scheduleNextEvent(state, config);
                if (spawned && category == EventCategory.MEMORY && state.stage() == EchoStage.DEJA_VU) {
                    state.incrementStageOneEvents();
                }
            }
        }
    }

    public boolean spawnReplay(ServerPlayerEntity target, boolean forced, EchoConfig config) {
        return spawnEcho(target, EchoType.MEMORY, forced, false, config);
    }

    public boolean spawnEcho(ServerPlayerEntity target, EchoType type, boolean forced, boolean forcedHostile, EchoConfig config) {
        if (!featureEnabled(type, config)) {
            return false;
        }
        if (type == EchoType.FALSE_MEMORY) {
            return spawnFalseMemory(target, forced, false, config);
        }
        PlayerRecording recording = recordingManager.get(target.getUuid());
        if (recording == null) {
            return false;
        }
        cleanupActiveEchoes();
        PlayerEchoState state = stageManager.state(target.getUuid());
        if (!prepareEvent(target, state, forced, config) || (!forced && !canSpawnType(target, type, config))) {
            return false;
        }
        int minFrames = Math.max(2, config.minimumReplaySeconds() * 20 / config.recordingSampleIntervalTicks());
        int maxFrames = Math.max(minFrames, config.maximumReplaySeconds() * 20 / config.recordingSampleIntervalTicks());
        List<RecordedFrame> segment = recording.randomSegment(minFrames, maxFrames);
        if (segment.isEmpty()) {
            return false;
        }
        List<RecordedFrame> frames = type == EchoType.CORRUPTED ? corruptSegment(segment) : segment;
        RecordedFrame start = frames.getFirst();
        ServerWorld world = target.getServerWorld();
        Optional<Vec3d> spawnPos = SafeEchoPositionFinder.findSpawn(world, target, start.pos(), config);
        if (spawnPos.isEmpty()) {
            return false;
        }
        EchoEventContext context = new EchoEventContext(target.getUuid(), config, stageManager, forcedHostile,
                !forced, observedCallback(target, !forced));
        EchoBehaviorController behavior = behaviorFor(type, context);
        List<RecordedFrame> replayFrames = type == EchoType.MIMIC ? frames : ReplayFrames.translated(frames, spawnPos.get());
        EchoEntity echo = createEcho(target, replayFrames, context, behavior, spawnPos.get(), start.bodyYaw(), start.pitch(), config);
        if (echo == null) {
            return false;
        }
        registerEcho(target, echo);
        if (type == EchoType.MIMIC) {
            lastMimicTick.put(target.getUuid(), stageManager.tick());
            lastGlobalMimicTick = stageManager.tick();
            if (!mimicSpawnedThisSession.contains(target.getUuid())) {
                mimicSpawnedThisSession.add(target.getUuid());
            }
        }
        finishSpawnBookkeeping(target, type, recording, spawnPos.get(), forced, config);
        if (!forced) {
            recordEvent(target, switch (type) {
                case MEMORY -> EventCategory.MEMORY;
                case CORRUPTED -> EventCategory.CORRUPTED;
                case MIMIC -> EventCategory.MIMIC;
                case ORIGINAL -> EventCategory.ORIGINAL;
                case FALSE_MEMORY -> EventCategory.FALSE_MEMORY;
            });
        }
        return true;
    }

    public boolean spawnFalseMemory(ServerPlayerEntity target, boolean forced, boolean forceDeviation, EchoConfig config) {
        if (!config.falseMemoriesEnabled()) {
            return false;
        }
        PlayerRecording recording = recordingManager.get(target.getUuid());
        PlayerEchoState state = stageManager.state(target.getUuid());
        if (recording == null || !prepareEvent(target, state, forced, config)
                || (!forced && state.stage().id() < config.falseMemoryMinimumStage())) {
            return false;
        }
        Optional<FalseMemoryPlan> optionalPlan = falseMemoryDirector.createPlan(target, recording, null, config,
                stageManager.tick(), forceDeviation);
        if (optionalPlan.isEmpty()) {
            return false;
        }
        return spawnFalsePlan(target, recording, optionalPlan.get(), config, !forced);
    }

    public boolean replayPanic(ServerPlayerEntity target, EchoConfig config, boolean forced) {
        PanicImprint imprint = panicImprints.latest(target.getUuid());
        PlayerRecording recording = recordingManager.get(target.getUuid());
        PlayerEchoState state = stageManager.state(target.getUuid());
        if (imprint == null || recording == null || !prepareEvent(target, state, forced, config)) {
            return false;
        }
        if (!imprint.dimension().equals(target.getServerWorld().getRegistryKey().getValue().toString())) {
            return false;
        }
        Optional<FalseMemoryPlan> optionalPlan = falseMemoryDirector.createPlan(target, recording, imprint, config,
                stageManager.tick(), true);
        return optionalPlan.isPresent() && spawnFalsePlan(target, recording, optionalPlan.get(), config, !forced);
    }

    private boolean spawnFalsePlan(ServerPlayerEntity target, PlayerRecording recording, FalseMemoryPlan plan,
                                   EchoConfig config, boolean awardsProgress) {
        RecordedFrame start = plan.realPrefix().get(0);
        Runnable observedCallback = observedCallback(target, awardsProgress);
        EchoEventContext echoContext = new EchoEventContext(target.getUuid(), config, stageManager, false,
                awardsProgress, observedCallback);
        FalseMemoryContext falseContext = new FalseMemoryContext(target.getUuid(), config, stageManager,
                falseMemoryDirector.history(), plan, stageManager.tick(), awardsProgress,
                observedCallback);
        EchoEntity echo = createEcho(target, plan.realPrefix(), echoContext, new FalseMemoryBehavior(falseContext),
                start.pos(), start.bodyYaw(), start.pitch(), config);
        if (echo == null) {
            return false;
        }
        registerEcho(target, echo);
        finishSpawnBookkeeping(target, EchoType.FALSE_MEMORY, recording, start.pos(), !awardsProgress, config);
        if (awardsProgress) {
            recordEvent(target, plan.panicImprint() ? EventCategory.PANIC_IMPRINT
                    : plan.majorDeviation() ? EventCategory.MAJOR_FALSE_MEMORY : EventCategory.FALSE_MEMORY);
        }
        return true;
    }

    public boolean spawnPeripheral(ServerPlayerEntity target, EchoConfig config, boolean forced) {
        if (!config.peripheralEchoesEnabled()) {
            return false;
        }
        UUID uuid = target.getUuid();
        long tick = stageManager.tick();
        if (!forced && (peripheralSessionCounts.getOrDefault(uuid, 0) >= config.peripheralEchoMaximumPerSession()
                || tick - lastPeripheralTicks.getOrDefault(uuid, Long.MIN_VALUE / 2)
                < (long) config.peripheralEchoMinimumIntervalMinutes() * 60L * 20L)) {
            return false;
        }
        PlayerRecording recording = recordingManager.get(uuid);
        PlayerEchoState state = stageManager.state(uuid);
        if (recording == null || recording.latest() == null || !prepareEvent(target, state, forced, config)) {
            return false;
        }
        Optional<Vec3d> pos = SafeEchoPositionFinder.findPeripheral(target.getServerWorld(), target, config);
        if (pos.isEmpty()) {
            return false;
        }
        RecordedFrame frame = recording.latest();
        EchoEventContext context = new EchoEventContext(uuid, config, stageManager, false,
                !forced, observedCallback(target, !forced));
        EchoEntity echo = createEcho(target, List.of(frame), context,
                new PeripheralEchoBehavior(context, config.peripheralEchoDurationSeconds() * 20),
                pos.get(), target.bodyYaw + 180.0F, target.getPitch(), config);
        if (echo == null) {
            return false;
        }
        registerEcho(target, echo);
        peripheralSessionCounts.merge(uuid, 1, Integer::sum);
        lastPeripheralTicks.put(uuid, tick);
        if (!forced) {
            recordEvent(target, EventCategory.PERIPHERAL);
        }
        return true;
    }

    public boolean playAudioResidue(ServerPlayerEntity target, EchoConfig config, boolean forced) {
        if (!forced && isInImmediateDanger(target)) {
            return false;
        }
        boolean played = audioResidues.play(target, config, stageManager.tick(), forced, !forced);
        if (played && !forced) {
            recordEvent(target, EventCategory.AUDIO_RESIDUE, true);
        }
        return played;
    }

    public boolean spawnOriginal(ServerPlayerEntity target, boolean forced, OriginalEventKind requestedEvent, EchoConfig config) {
        return spawnOriginal(target, forced, requestedEvent, null, config);
    }

    private boolean spawnOriginal(ServerPlayerEntity target, boolean forced, OriginalEventKind requestedEvent,
                                  OriginalMovementMode movementTest, EchoConfig config) {
        if (!config.originalEnabled()) {
            return false;
        }
        cleanupActiveEchoes();
        PlayerEchoState state = stageManager.state(target.getUuid());
        if ((!forced && state.stage() != EchoStage.THE_ORIGINAL) || !prepareEvent(target, state, forced, config)
                || activeOriginalCount(target) >= config.originalMaximumActivePerPlayer()) {
            return false;
        }
        ServerWorld world = target.getServerWorld();
        PlayerHabitSummary.Habit habit = chooseHabit(target, config, requestedEvent).orElse(null);
        FamiliarLocation location = habit == null ? chooseOriginalLocation(target, config, requestedEvent).orElse(null) : null;
        Vec3d anchor = habit != null ? habit.position().toCenterPos()
                : location == null ? target.getPos() : location.pos().toCenterPos();
        OriginalEventKind eventKind = requestedEvent != null ? requestedEvent
                : habit != null ? chooseOriginalEvent(habit) : chooseOriginalEvent(location);
        Optional<Vec3d> spawnPos = SafeEchoPositionFinder.findOriginalStart(world, target, anchor, config);
        if (spawnPos.isEmpty()) {
            spawnPos = SafeEchoPositionFinder.findSpawn(world, target,
                    target.getPos().subtract(target.getRotationVec(1.0F).multiply(config.minimumEchoSpawnDistance())), config);
        }
        if (spawnPos.isEmpty()) {
            return false;
        }
        PlayerRecording recording = recordingManager.get(target.getUuid());
        RecordedFrame frame = recording != null && recording.latest() != null
                ? recording.latest() : RecordedFrame.capture(target, stageManager.tick(), null);
        ItemStack heldItem = habit != null && !habit.visualItem().isEmpty()
                ? habit.visualItem() : chooseFamiliarItem(recording, target);
        List<Vec3d> knownLocations = collectOriginalAnchors(target);
        EchoEventContext context = new EchoEventContext(target.getUuid(), config, stageManager, false,
                !forced, observedCallback(target, !forced));
        EchoEntity echo = createEcho(target, List.of(frame), context,
                new OriginalEchoBehavior(context, eventKind, anchor, knownLocations, heldItem, movementTest,
                        habit != null || location != null,
                        habit != null && habit.type() == HabitType.SLEEPING
                                || location != null && location.type() == FamiliarLocationType.BED), spawnPos.get(),
                target.bodyYaw + 180.0F, target.getPitch(), config);
        if (echo == null) {
            return false;
        }
        registerEcho(target, echo);
        if (!forced) {
            state.incrementTotalEvents();
            state.incrementEchoEvent(EchoType.ORIGINAL);
        }
        EchoSoundPlayer.playSpawnProfile(target, EchoType.ORIGINAL, config, spawnPos.get());
        if (!forced) {
            recordEvent(target, EventCategory.ORIGINAL);
        }
        return true;
    }

    public boolean spawnOriginalConfrontation(ServerPlayerEntity target, EchoConfig config) {
        return spawnOriginal(target, true, OriginalEventKind.CONFRONTATION, config);
    }

    public boolean spawnOriginalMovementTest(ServerPlayerEntity target, OriginalMovementMode mode, EchoConfig config) {
        OriginalEventKind event = switch (mode) {
            case BED -> OriginalEventKind.YOUR_BED;
            case DOORWAY -> OriginalEventKind.EMPTY_ROOM;
            case APPROACH -> OriginalEventKind.CONFRONTATION;
            default -> OriginalEventKind.WAITING;
        };
        return spawnOriginal(target, true, event, mode, config);
    }

    public String originalStatus(ServerPlayerEntity target) {
        cleanupActiveEchoes();
        for (EchoEntity echo : activeEchoes.getOrDefault(target.getUuid(), List.of())) {
            if (!echo.isRemoved() && echo.echoType() == EchoType.ORIGINAL
                    && echo.behavior() instanceof OriginalEchoBehavior original) {
                return original.status(echo);
            }
        }
        return "inactive";
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
        for (EchoEntity echo : activeEchoes.getOrDefault(target.getUuid(), List.of())) {
            if (!echo.isRemoved() && echo.echoType() == EchoType.MIMIC && echo.behavior() != null) {
                echo.behavior().forceHostile(hostile);
                return true;
            }
        }
        return false;
    }

    public boolean playDebugSound(ServerPlayerEntity target, EchoType type) {
        return EchoSoundPlayer.playSpawnProfile(target, type, EchoProtocol.config(), target.getPos());
    }

    public boolean capturePanic(ServerPlayerEntity target, EchoConfig config) {
        return panicImprints.capture(target, PanicTriggerType.MANUAL, config, stageManager.tick(), true);
    }

    public List<PanicImprint> panicImprints(UUID playerUuid) { return panicImprints.list(playerUuid); }
    public List<AudioResidue> audioResidues(UUID playerUuid) { return audioResidues.list(playerUuid); }
    public int clearPanic(UUID playerUuid) { return panicImprints.clear(playerUuid); }
    public List<PlayerHabitSummary.Habit> habits(UUID playerUuid) { return habits.habits(playerUuid); }
    public int clearHabits(UUID playerUuid) { return habits.clear(playerUuid); }
    public List<EchoEventHistory.Entry> history(UUID playerUuid) { return eventHistory.entries(playerUuid); }
    public int clearHistory(UUID playerUuid) { return eventHistory.clear(playerUuid); }

    public String status(ServerPlayerEntity target, EchoConfig config) {
        PlayerEchoState state = stageManager.state(target.getUuid());
        return "stage=" + state.stage().name().toLowerCase() + ", active=" + state.activeEvent()
                + ", history=" + eventHistory.entries(target.getUuid()).size()
                + ", panic=" + panicImprints.list(target.getUuid()).size()
                + ", habits=" + habits.habits(target.getUuid()).size()
                + ", peripheral=" + peripheralSessionCounts.getOrDefault(target.getUuid(), 0)
                + "/" + config.peripheralEchoMaximumPerSession()
                + ", audio=" + audioResidues.sessionPlays(target.getUuid())
                + "/" + config.audioResidueMaximumPerSession();
    }

    public void recordInteraction(ServerPlayerEntity player, HabitType habit, BlockPos pos, ItemStack item,
                                  SoundEvent sound, float volume, float pitch, AudioResidueEvent audioEvent) {
        EchoConfig config = EchoProtocol.config();
        habits.record(player, habit, pos, item, config, stageManager.tick());
        audioResidues.capture(player, pos, sound, volume, pitch, audioEvent, config, stageManager.tick());
    }

    public void observeDamage(ServerPlayerEntity player, DamageSource source, float baseDamage, boolean blocked) {
        panicImprints.observeDamage(player, source, baseDamage, blocked, EchoProtocol.config(), stageManager.tick());
        if (blocked) {
            audioResidues.capture(player, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK,
                    0.4F, 0.9F, AudioResidueEvent.SHIELD, EchoProtocol.config(), stageManager.tick());
        }
    }

    public void noteSleep(ServerPlayerEntity player) {
        lastSleepTicks.put(player.getUuid(), stageManager.tick());
    }

    public void onDisconnect(ServerPlayerEntity player) {
        clearPlayer(player);
    }

    public void clearPlayer(ServerPlayerEntity player) {
        stopEvents(player);
        UUID uuid = player.getUuid();
        recordingManager.clear(uuid);
        panicImprints.disconnect(player.getUuid());
        audioResidues.disconnect(player.getUuid());
        habits.clear(uuid);
        eventHistory.clear(uuid);
        falseMemoryDirector.clear(uuid);
        lastMimicTick.remove(uuid);
        mimicSpawnedThisSession.remove(uuid);
        peripheralSessionCounts.remove(uuid);
        lastPeripheralTicks.remove(uuid);
        dimensions.remove(uuid);
        dimensionChangeTicks.remove(uuid);
        lastSleepTicks.remove(uuid);
        EchoSoundPlayer.clear(uuid);
    }

    private EventCategory chooseEvent(ServerPlayerEntity target, EchoConfig config) {
        List<AdaptiveEventSelector.Candidate> candidates = new ArrayList<>();
        boolean night = target.getServerWorld().isNight();
        boolean underground = !target.getServerWorld().isSkyVisible(target.getBlockPos());
        boolean atHome = stageManager.familiarLocations(target).stream()
                .anyMatch(location -> location.dimension().equals(target.getServerWorld().getRegistryKey().getValue().toString())
                        && location.pos().getSquaredDistance(target.getBlockPos()) <= 16.0D * 16.0D);
        if (canSpawnType(target, EchoType.MEMORY, config)) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.MEMORY, config.memoryEchoWeight()));
        }
        if (canSpawnType(target, EchoType.CORRUPTED, config)) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.CORRUPTED, config.corruptedEchoWeight()));
        }
        if (canSpawnType(target, EchoType.MIMIC, config)) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.MIMIC, config.mimicEchoWeight()));
        }
        if (canSpawnType(target, EchoType.FALSE_MEMORY, config)) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.FALSE_MEMORY,
                    config.falseMemoryEventWeight() + (atHome ? 3 : 0)));
        }
        if (config.panicImprintsEnabled() && !panicImprints.list(target.getUuid()).isEmpty()) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.PANIC_IMPRINT, 3));
        }
        if (canSpawnPeripheral(target, config)) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.PERIPHERAL, night ? 7 : 4));
        }
        if (audioResidues.isEligible(target, config, stageManager.tick())) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.AUDIO_RESIDUE, underground ? 6 : 4));
        }
        return selector.select(target.getUuid(), stageManager.tick(), candidates, eventHistory, config);
    }

    private boolean spawnDirectedEvent(ServerPlayerEntity target, EventCategory category, EchoConfig config) {
        if (category == null) {
            return false;
        }
        return switch (category) {
            case MEMORY -> spawnEcho(target, EchoType.MEMORY, false, false, config);
            case CORRUPTED -> spawnEcho(target, EchoType.CORRUPTED, false, false, config);
            case MIMIC -> spawnEcho(target, EchoType.MIMIC, false, false, config);
            case FALSE_MEMORY, MAJOR_FALSE_MEMORY -> spawnFalseMemory(target, false, false, config);
            case PANIC_IMPRINT -> replayPanic(target, config, false);
            case PERIPHERAL -> spawnPeripheral(target, config, false);
            case AUDIO_RESIDUE -> playAudioResidue(target, config, false);
            case ORIGINAL -> spawnOriginal(target, false, null, config);
        };
    }

    private boolean prepareEvent(ServerPlayerEntity target, PlayerEchoState state, boolean forced, EchoConfig config) {
        cleanupActiveEchoes();
        if (state.activeEvent()) {
            if (!forced) {
                return false;
            }
            stopEvents(target);
        }
        return forced || !isTooCloseToOtherPlayer(target, config);
    }

    private boolean canRunEvent(ServerPlayerEntity player, PlayerEchoState state, EchoConfig config) {
        long now = stageManager.tick();
        long joinGrace = (long) config.joinEventGraceMinutes() * 60L * 20L;
        if (now - state.lastJoinTick() < joinGrace || now - state.lastRespawnTick() < 20L * 30L
                || now - dimensionChangeTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2) < 100L
                || now - lastSleepTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2) < 200L) {
            return false;
        }
        return !player.isSleeping() && !player.isSpectator() && !player.isDead() && player.getHealth() > 0.0F
                && !state.activeEvent() && player.currentScreenHandler == player.playerScreenHandler
                && !isInImmediateDanger(player);
    }

    private boolean isInImmediateDanger(ServerPlayerEntity player) {
        if (panicImprints.recentlyDamaged(player.getUuid(), stageManager.tick(), 200)
                || player.getHealth() <= Math.min(6.0F, EchoProtocol.config().panicImprintHealthThreshold())) {
            return true;
        }
        List<MobEntity> nearby = player.getServerWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(24.0D), mob -> mob.isAlive()
                        && (mob.getTarget() == player || mob instanceof WitherEntity || mob instanceof EnderDragonEntity));
        return !nearby.isEmpty();
    }

    private void noteDimension(ServerPlayerEntity player) {
        String current = player.getServerWorld().getRegistryKey().getValue().toString();
        String previous = dimensions.put(player.getUuid(), current);
        if (previous != null && !previous.equals(current)) {
            dimensionChangeTicks.put(player.getUuid(), stageManager.tick());
            stopEvents(player);
            recordingManager.clear(player.getUuid());
        }
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

    private boolean canSpawnType(ServerPlayerEntity target, EchoType type, EchoConfig config) {
        PlayerEchoState state = stageManager.state(target.getUuid());
        return switch (type) {
            case MEMORY -> config.memoryEchoEnabled() && state.stage().id() >= EchoStage.DEJA_VU.id();
            case CORRUPTED -> config.corruptedEchoEnabled()
                    && state.stage().id() >= EchoStage.CORRUPTED_MEMORY.id() && state.stageOneEvents() >= 2;
            case MIMIC -> config.mimicEchoEnabled()
                    && state.stage().id() >= EchoStage.CORRUPTED_MEMORY.id()
                    && state.playTicks() >= (long) (config.stageTwoMinutes() + config.mimicMinimumStageTwoMinutes()) * 60L * 20L
                    && !mimicSpawnedThisSession.contains(target.getUuid())
                    && stageManager.tick() - lastGlobalMimicTick >= (long) config.mimicSessionCooldownMinutes() * 60L * 20L
                    && stageManager.tick() - lastMimicTick.getOrDefault(target.getUuid(), -9999999L)
                    >= (long) config.mimicSessionCooldownMinutes() * 60L * 20L;
            case ORIGINAL -> config.originalEnabled() && state.stage() == EchoStage.THE_ORIGINAL;
            case FALSE_MEMORY -> config.falseMemoriesEnabled() && state.stage().id() >= config.falseMemoryMinimumStage();
        };
    }

    private static boolean featureEnabled(EchoType type, EchoConfig config) {
        return switch (type) {
            case MEMORY -> config.memoryEchoEnabled();
            case CORRUPTED -> config.corruptedEchoEnabled();
            case MIMIC -> config.mimicEchoEnabled();
            case FALSE_MEMORY -> config.falseMemoriesEnabled();
            case ORIGINAL -> config.originalEnabled();
        };
    }

    private boolean canSpawnPeripheral(ServerPlayerEntity target, EchoConfig config) {
        UUID uuid = target.getUuid();
        return config.peripheralEchoesEnabled()
                && peripheralSessionCounts.getOrDefault(uuid, 0) < config.peripheralEchoMaximumPerSession()
                && stageManager.tick() - lastPeripheralTicks.getOrDefault(uuid, Long.MIN_VALUE / 2)
                >= (long) config.peripheralEchoMinimumIntervalMinutes() * 60L * 20L;
    }

    private static EchoBehaviorController behaviorFor(EchoType type, EchoEventContext context) {
        return switch (type) {
            case MEMORY -> new MemoryEchoBehavior(context);
            case CORRUPTED -> new CorruptedEchoBehavior(context);
            case MIMIC -> new MimicEchoBehavior(context);
            case ORIGINAL, FALSE_MEMORY -> throw new IllegalArgumentException("Echo type uses a specialized spawn path: " + type);
        };
    }

    private EchoEntity createEcho(ServerPlayerEntity target, List<RecordedFrame> frames, EchoEventContext context,
                                  EchoBehaviorController behavior, Vec3d pos, float yaw, float pitch, EchoConfig config) {
        EchoEntity echo = new EchoEntity(EchoEntities.ECHO, target.getServerWorld());
        echo.configure(target.getUuid(), config.sharedEchoes(), frames, config.recordingSampleIntervalTicks(),
                config, context, behavior);
        echo.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
        return target.getServerWorld().spawnEntity(echo) ? echo : null;
    }

    private void registerEcho(ServerPlayerEntity target, EchoEntity echo) {
        activeEchoes.computeIfAbsent(target.getUuid(), ignored -> new ArrayList<>()).add(echo);
        stageManager.state(target.getUuid()).setActiveEvent(true);
    }

    private void finishSpawnBookkeeping(ServerPlayerEntity target, EchoType type, PlayerRecording recording,
                                        Vec3d effectPosition, boolean forced, EchoConfig config) {
        PlayerEchoState state = stageManager.state(target.getUuid());
        EchoSoundPlayer.playSpawnProfile(target, type, config, effectPosition);
        if (!forced) {
            state.incrementTotalEvents();
            state.incrementEchoEvent(type);
        }
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
            playMemorySound(target, recording, config);
        }
        if (config.torchFlicker() || config.echoLightEffects()) {
            spawnTargetedParticles(target, effectPosition.add(0.0D, 1.1D, 0.0D));
        }
        if (state.stage() == EchoStage.CORRUPTED_MEMORY && config.chatEchoes() && type != EchoType.FALSE_MEMORY) {
            maybeEchoChat(target, recording);
        }
    }

    private void recordEvent(ServerPlayerEntity target, EventCategory category) {
        recordEvent(target, category, false);
    }

    private void recordEvent(ServerPlayerEntity target, EventCategory category, boolean observed) {
        eventHistory.record(target.getUuid(), category, stageManager.tick(), observed, EchoProtocol.config());
    }

    private Runnable observedCallback(ServerPlayerEntity target, boolean awardsProgress) {
        boolean[] handled = {false};
        return () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            eventHistory.markLastObserved(target.getUuid());
            if (awardsProgress) {
                stageManager.grant(target, "deja_vu");
                if (EchoProtocol.config().realPlayerSkins()
                        && target.getGameProfile().getProperties().containsKey("textures")) {
                    stageManager.grant(target, "familiar_face");
                }
            }
        };
    }

    private static List<RecordedFrame> corruptSegment(List<RecordedFrame> original) {
        if (original.size() < 4) {
            return original;
        }
        int trim = ThreadLocalRandom.current().nextInt(0, Math.max(1, original.size() / 3));
        List<RecordedFrame> frames = original.subList(0, original.size() - trim);
        return ThreadLocalRandom.current().nextBoolean() ? frames.reversed() : frames;
    }

    private int activeOriginalCount(ServerPlayerEntity target) {
        int count = 0;
        for (EchoEntity echo : activeEchoes.getOrDefault(target.getUuid(), List.of())) {
            if (!echo.isRemoved() && echo.echoType() == EchoType.ORIGINAL) {
                count++;
            }
        }
        return count;
    }

    private Optional<PlayerHabitSummary.Habit> chooseHabit(ServerPlayerEntity target, EchoConfig config,
                                                            OriginalEventKind requestedEvent) {
        if (!config.borrowedHabitsEnabled()) {
            return Optional.empty();
        }
        String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
        double localRadius = Math.max(12.0D, config.originalMaximumMovementDistance() + 4.0D);
        return habits.habits(target.getUuid()).stream()
                .filter(habit -> habit.dimension().equals(dimension))
                .filter(habit -> target.getServerWorld().isChunkLoaded(habit.position()))
                .filter(habit -> habit.position().getSquaredDistance(target.getBlockPos()) <= localRadius * localRadius)
                .filter(habit -> validHabitAnchor(target.getServerWorld(), habit))
                .filter(habit -> requestedEvent == null || eventMatches(requestedEvent, habit.type()))
                .findFirst();
    }

    private Optional<FamiliarLocation> chooseOriginalLocation(ServerPlayerEntity target, EchoConfig config,
                                                               OriginalEventKind requestedEvent) {
        if (!config.originalFamiliarLocationsEnabled()) {
            return Optional.empty();
        }
        String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
        double localRadius = Math.max(12.0D, config.originalMaximumMovementDistance() + 4.0D);
        return stageManager.familiarLocations(target).stream()
                .filter(location -> location.dimension().equals(dimension))
                .filter(location -> target.getServerWorld().isChunkLoaded(location.pos()))
                .filter(location -> location.pos().getSquaredDistance(target.getBlockPos()) <= localRadius * localRadius)
                .filter(location -> validFamiliarAnchor(target.getServerWorld(), location))
                .filter(location -> requestedEvent == null || eventMatches(requestedEvent, location.type()))
                .sorted((left, right) -> Integer.compare(right.visits(), left.visits())).findFirst();
    }

    private static boolean eventMatches(OriginalEventKind event, HabitType type) {
        return switch (event) {
            case YOUR_BED, ALREADY_HOME -> type == HabitType.SLEEPING;
            case WRONG_OWNER -> type == HabitType.STORAGE;
            case OCCUPIED_PLACE -> type == HabitType.CRAFTING || type == HabitType.FURNACE;
            case EARLIER_THAN_YOU, EMPTY_ROOM -> type == HabitType.ENTRY_ROUTE || type == HabitType.PORTAL;
            case FAMILIAR_ITEM -> type == HabitType.FREQUENT_ITEM;
            case WAITING, CONFRONTATION -> true;
        };
    }

    private static boolean eventMatches(OriginalEventKind event, FamiliarLocationType type) {
        return switch (event) {
            case YOUR_BED -> type == FamiliarLocationType.BED;
            case ALREADY_HOME -> type == FamiliarLocationType.HOME || type == FamiliarLocationType.BED;
            case WRONG_OWNER -> type == FamiliarLocationType.CHEST;
            case OCCUPIED_PLACE -> type == FamiliarLocationType.CRAFTING || type == FamiliarLocationType.FURNACE;
            case EARLIER_THAN_YOU -> type == FamiliarLocationType.PORTAL || type == FamiliarLocationType.MINE_ENTRANCE
                    || type == FamiliarLocationType.DOORWAY;
            case EMPTY_ROOM -> type == FamiliarLocationType.DOORWAY;
            case FAMILIAR_ITEM -> false;
            case WAITING, CONFRONTATION -> true;
        };
    }

    private static boolean validHabitAnchor(ServerWorld world, PlayerHabitSummary.Habit habit) {
        Object block = world.getBlockState(habit.position()).getBlock();
        return switch (habit.type()) {
            case STORAGE -> block instanceof ChestBlock;
            case SLEEPING -> block instanceof BedBlock;
            case CRAFTING -> block instanceof CraftingTableBlock;
            case FURNACE -> block instanceof AbstractFurnaceBlock;
            case ENTRY_ROUTE -> block instanceof DoorBlock;
            case PORTAL -> block instanceof NetherPortalBlock
                    || world.getBlockState(habit.position()).isOf(Blocks.END_PORTAL)
                    || world.getBlockState(habit.position()).isOf(Blocks.END_GATEWAY);
            case IDLE, FREQUENT_ITEM -> true;
        };
    }

    private static boolean validFamiliarAnchor(ServerWorld world, FamiliarLocation location) {
        Object block = world.getBlockState(location.pos()).getBlock();
        return switch (location.type()) {
            case BED -> block instanceof BedBlock;
            case CHEST -> block instanceof ChestBlock;
            case CRAFTING -> block instanceof CraftingTableBlock;
            case FURNACE -> block instanceof AbstractFurnaceBlock;
            case DOORWAY -> block instanceof DoorBlock;
            case PORTAL -> block instanceof NetherPortalBlock
                    || world.getBlockState(location.pos()).isOf(Blocks.END_PORTAL)
                    || world.getBlockState(location.pos()).isOf(Blocks.END_GATEWAY);
            case IDLE, HOME, MINE_ENTRANCE, MANUAL -> true;
        };
    }

    private static OriginalEventKind chooseOriginalEvent(FamiliarLocation location) {
        if (ThreadLocalRandom.current().nextInt(12) == 0) {
            return OriginalEventKind.CONFRONTATION;
        }
        if (location == null) {
            return OriginalEventKind.WAITING;
        }
        return switch (location.type()) {
            case BED -> OriginalEventKind.YOUR_BED;
            case CHEST -> OriginalEventKind.WRONG_OWNER;
            case CRAFTING, FURNACE, MANUAL -> OriginalEventKind.OCCUPIED_PLACE;
            case HOME -> OriginalEventKind.ALREADY_HOME;
            case IDLE -> OriginalEventKind.WAITING;
            case DOORWAY -> OriginalEventKind.EMPTY_ROOM;
            case PORTAL, MINE_ENTRANCE -> OriginalEventKind.EARLIER_THAN_YOU;
        };
    }

    private static OriginalEventKind chooseOriginalEvent(PlayerHabitSummary.Habit habit) {
        return switch (habit.type()) {
            case SLEEPING -> OriginalEventKind.YOUR_BED;
            case STORAGE -> OriginalEventKind.WRONG_OWNER;
            case PORTAL, ENTRY_ROUTE -> OriginalEventKind.EARLIER_THAN_YOU;
            case CRAFTING, FURNACE -> OriginalEventKind.OCCUPIED_PLACE;
            case IDLE -> OriginalEventKind.WAITING;
            case FREQUENT_ITEM -> OriginalEventKind.FAMILIAR_ITEM;
        };
    }

    private static ItemStack chooseFamiliarItem(PlayerRecording recording, ServerPlayerEntity target) {
        if (recording != null) {
            for (RecordedFrame frame : recording.frames().reversed()) {
                if (!frame.heldItemVisual().isEmpty()) {
                    return frame.heldItemVisual().copyWithCount(1);
                }
            }
        }
        return target.getMainHandStack().copyWithCount(Math.min(1, target.getMainHandStack().getCount()));
    }

    private List<Vec3d> collectOriginalAnchors(ServerPlayerEntity target) {
        String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
        List<Vec3d> result = new ArrayList<>(5);
        for (PlayerHabitSummary.Habit habit : habits.habits(target.getUuid())) {
            if (result.size() >= 5) {
                break;
            }
            if (habit.dimension().equals(dimension) && target.getServerWorld().isChunkLoaded(habit.position())
                    && habit.position().getSquaredDistance(target.getBlockPos()) <= 16.0D * 16.0D
                    && validHabitAnchor(target.getServerWorld(), habit)) {
                addDistinctAnchor(result, habit.position().toCenterPos());
            }
        }
        for (FamiliarLocation location : stageManager.familiarLocations(target)) {
            if (result.size() >= 5) {
                break;
            }
            if (location.dimension().equals(dimension) && target.getServerWorld().isChunkLoaded(location.pos())
                    && location.pos().getSquaredDistance(target.getBlockPos()) <= 16.0D * 16.0D
                    && validFamiliarAnchor(target.getServerWorld(), location)) {
                addDistinctAnchor(result, location.pos().toCenterPos());
            }
        }
        return List.copyOf(result);
    }

    private static void addDistinctAnchor(List<Vec3d> anchors, Vec3d candidate) {
        for (Vec3d existing : anchors) {
            if (existing.squaredDistanceTo(candidate) < 2.5D * 2.5D) {
                return;
            }
        }
        anchors.add(candidate);
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
            target.getServerWorld().playSound(null, marker.x(), marker.y(), marker.z(), marker.sound(),
                    SoundCategory.PLAYERS, Math.min(marker.volume(), 0.6F), marker.pitch());
        } else {
            target.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(marker.sound()),
                    SoundCategory.PLAYERS, marker.x(), marker.y(), marker.z(), Math.min(marker.volume(), 0.6F),
                    marker.pitch(), target.getRandom().nextLong()));
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
            target.sendMessage(Text.translatable("text.echoprotocol.chat_echo",
                    target.getGameProfile().getName(), message), false);
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
        peripheralSessionCounts.clear();
        lastPeripheralTicks.clear();
        dimensions.clear();
        dimensionChangeTicks.clear();
        lastSleepTicks.clear();
        eventHistory.clearAll();
        falseMemoryDirector.clear();
        panicImprints.clearAll();
        audioResidues.clearAll();
        habits.clearAll();
        EchoSoundPlayer.clearAll();
        lastGlobalMimicTick = -9999999L;
    }
}
