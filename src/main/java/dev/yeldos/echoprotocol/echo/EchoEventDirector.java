package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.audio.AudioResidueEvent;
import dev.yeldos.echoprotocol.audio.AudioResidueManager;
import dev.yeldos.echoprotocol.audio.AudioResidue;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.config.EchoPresetManager;
import dev.yeldos.echoprotocol.config.EchoPresetValues;
import dev.yeldos.echoprotocol.contamination.ContaminationSource;
import dev.yeldos.echoprotocol.contamination.ContaminationTier;
import dev.yeldos.echoprotocol.contradiction.ContradictionBehavior;
import dev.yeldos.echoprotocol.contradiction.ContradictionGroup;
import dev.yeldos.echoprotocol.contradiction.ContradictionPlan;
import dev.yeldos.echoprotocol.contradiction.ContradictionPlanner;
import dev.yeldos.echoprotocol.contradiction.ContradictionVariant;
import dev.yeldos.echoprotocol.director.AdaptiveEventSelector;
import dev.yeldos.echoprotocol.director.EchoEventHistory;
import dev.yeldos.echoprotocol.director.EventCategory;
import dev.yeldos.echoprotocol.director.EventDirectorPolicy;
import dev.yeldos.echoprotocol.entity.EchoEntities;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryBehavior;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryContext;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryDirector;
import dev.yeldos.echoprotocol.falsememory.FalseMemoryPlan;
import dev.yeldos.echoprotocol.habit.HabitType;
import dev.yeldos.echoprotocol.habit.PlayerHabitSummary;
import dev.yeldos.echoprotocol.habit.PlayerHabitTracker;
import dev.yeldos.echoprotocol.memory.SignificantEventRecord;
import dev.yeldos.echoprotocol.profile.ObservationMetric;
import dev.yeldos.echoprotocol.profile.ObservationStyle;
import dev.yeldos.echoprotocol.panic.PanicImprint;
import dev.yeldos.echoprotocol.panic.PanicImprintManager;
import dev.yeldos.echoprotocol.panic.PanicTriggerType;
import dev.yeldos.echoprotocol.peripheral.PeripheralEchoBehavior;
import dev.yeldos.echoprotocol.room.RoomMemoryManager;
import dev.yeldos.echoprotocol.room.RoomMemoryNode;
import dev.yeldos.echoprotocol.thread.MemoryObservationResult;
import dev.yeldos.echoprotocol.thread.MemoryThreadContext;
import dev.yeldos.echoprotocol.thread.MemoryThreadEventType;
import dev.yeldos.echoprotocol.thread.MemoryThreadManager;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class EchoEventDirector {
    private final RecordingManager recordingManager;
    private final StageManager stageManager;
    private final FalseMemoryDirector falseMemoryDirector;
    private final PanicImprintManager panicImprints;
    private final AudioResidueManager audioResidues;
    private final PlayerHabitTracker habits;
    private final RoomMemoryManager roomMemories;
    private final MemoryThreadManager memoryThreads;
    private final ContradictionPlanner contradictionPlanner = new ContradictionPlanner();
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
    private final Map<UUID, PendingSoundObservation> pendingSoundObservations = new HashMap<>();
    private final Map<UUID, MemoryThreadContext> activeThreadEvents = new HashMap<>();
    private final Map<UUID, Integer> contradictionSessionCounts = new HashMap<>();
    private final Map<UUID, Long> lastMemoryFragmentTicks = new HashMap<>();
    private long lastGlobalMimicTick = -9999999L;

    public EchoEventDirector(RecordingManager recordingManager, StageManager stageManager) {
        this.recordingManager = recordingManager;
        this.stageManager = stageManager;
        this.falseMemoryDirector = new FalseMemoryDirector(stageManager);
        this.panicImprints = new PanicImprintManager(recordingManager, stageManager);
        this.audioResidues = new AudioResidueManager(stageManager);
        this.habits = new PlayerHabitTracker(stageManager);
        this.roomMemories = new RoomMemoryManager(stageManager);
        this.memoryThreads = new MemoryThreadManager(stageManager, roomMemories);
    }

    public void tick(MinecraftServer server, EchoConfig config) {
        cleanupActiveEchoes();
        finalizeMissingThreadEvents();
        long persistentTick = memoryTick(server);
        panicImprints.tick(server, config, stageManager.tick());
        habits.tick(server, config, stageManager.tick());
        roomMemories.tick(server, config, persistentTick);
        memoryThreads.tick(server, config, persistentTick);
        tickPendingSoundObservations(server, config);
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
            if (state.stage() == EchoStage.THE_ORIGINAL && config.originalEnabled()
                    && stageManager.memory(player.getUuid()).activeThread() == null) {
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
                Optional<MemoryThreadContext> threadStep = memoryThreads.nextStep(player, config, persistentTick);
                EventDirectorPolicy.Decision decision = EventDirectorPolicy.decide(new EventDirectorPolicy.Inputs(
                        true, false, true, stageManager.memory(player.getUuid()).activeThread() != null,
                        threadStep.isPresent()));
                if (decision == EventDirectorPolicy.Decision.ATTEMPT_THREAD
                        && spawnThreadEvent(player, threadStep.orElseThrow(), config)) {
                    stageManager.scheduleNextEvent(state, config);
                    continue;
                }
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
        Runnable observed = observedCallback(target, !forced, null, MemoryObservationResult.DIRECT,
                type.name().toLowerCase(java.util.Locale.ROOT), 0.0F);
        if (type == EchoType.MEMORY && !forced) {
            Runnable baseObserved = observed;
            boolean[] recovered = {false};
            observed = () -> {
                baseObserved.run();
                if (!recovered[0]) {
                    recovered[0] = true;
                    stageManager.memory(target.getUuid()).reduceContamination(
                            start.serverTick() ^ memoryTick(target), 0.025F, config);
                }
            };
        }
        EchoEventContext context = new EchoEventContext(target.getUuid(), config, stageManager, forcedHostile,
                !forced, observed);
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
        return spawnFalseMemory(target, forced, forceDeviation, null, config);
    }

    private boolean spawnFalseMemory(ServerPlayerEntity target, boolean forced, boolean forceDeviation,
                                     MemoryThreadContext threadContext, EchoConfig config) {
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
        return spawnFalsePlan(target, recording, optionalPlan.get(), config, !forced, threadContext);
    }

    public boolean replayPanic(ServerPlayerEntity target, EchoConfig config, boolean forced) {
        return replayPanic(target, config, forced, null);
    }

    private boolean replayPanic(ServerPlayerEntity target, EchoConfig config, boolean forced,
                                MemoryThreadContext threadContext) {
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
        return optionalPlan.isPresent()
                && spawnFalsePlan(target, recording, optionalPlan.get(), config, !forced, threadContext);
    }

    private boolean spawnFalsePlan(ServerPlayerEntity target, PlayerRecording recording, FalseMemoryPlan plan,
                                   EchoConfig config, boolean awardsProgress, MemoryThreadContext threadContext) {
        RecordedFrame start = plan.realPrefix().get(0);
        Vec3d observedDestination = plan.fabricatedFrames().isEmpty() ? start.pos()
                : plan.fabricatedFrames().getLast().pos();
        Runnable baseObserved = observedCallback(target, awardsProgress, threadContext,
                () -> target.getPos().squaredDistanceTo(observedDestination) <= 6.0D * 6.0D
                        ? MemoryObservationResult.FOLLOWED : MemoryObservationResult.DIRECT,
                plan.panicImprint() ? "panic_imprint" : "false_memory", 0.035F);
        boolean[] contaminationHandled = {false};
        Runnable observedCallback = () -> {
            baseObserved.run();
            if (!contaminationHandled[0] && awardsProgress && threadContext == null) {
                contaminationHandled[0] = true;
                contributeContaminationAndGrantTier(target, plan.seed() ^ memoryTick(target),
                        ContaminationSource.FALSE_MEMORY_OBSERVED, 0.025F, config);
            }
        };
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
        return spawnPeripheral(target, config, forced, null);
    }

    private boolean spawnPeripheral(ServerPlayerEntity target, EchoConfig config, boolean forced,
                                    MemoryThreadContext threadContext) {
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
                !forced, observedCallback(target, !forced, threadContext,
                MemoryObservationResult.PERIPHERAL, "peripheral_echo", 0.015F));
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
        return playAudioResidue(target, config, forced, null);
    }

    private boolean playAudioResidue(ServerPlayerEntity target, EchoConfig config, boolean forced,
                                     MemoryThreadContext threadContext) {
        if (!forced && isInImmediateDanger(target)) {
            return false;
        }
        boolean played = audioResidues.play(target, config, stageManager.tick(), forced, !forced);
        if (played && !forced) {
            recordEvent(target, EventCategory.AUDIO_RESIDUE, true);
        }
        if (played && threadContext != null) {
            audioResidues.lastPlaybackPosition(target.getUuid()).ifPresent(position ->
                    pendingSoundObservations.put(target.getUuid(), new PendingSoundObservation(position,
                            target.getPos().distanceTo(position), stageManager.tick(), threadContext)));
        }
        return played;
    }

    public boolean spawnContradiction(ServerPlayerEntity target, ContradictionVariant variant,
                                      EchoConfig config, boolean forced) {
        return spawnContradiction(target, variant, config, forced, null);
    }

    private boolean spawnContradiction(ServerPlayerEntity target, ContradictionVariant variant,
                                       EchoConfig config, boolean forced, MemoryThreadContext threadContext) {
        if (!contradictionEnabled(variant, config)
                || (!forced && contradictionSessionCounts.getOrDefault(target.getUuid(), 0)
                >= config.contradictionEventMaximumPerSession())) {
            return false;
        }
        PlayerRecording recording = recordingManager.get(target.getUuid());
        PlayerEchoState state = stageManager.state(target.getUuid());
        if (recording == null || recording.frames().size() < 8 || !prepareEvent(target, state, forced, config)) {
            return false;
        }
        ServerWorld world = target.getServerWorld();
        List<RecordedFrame> source = recording.frames();
        int sourceStart = Math.max(0, source.size() - Math.min(60, source.size()));
        List<RecordedFrame> boundedSource = source.subList(sourceStart, source.size());
        Optional<Vec3d> safeStart = SafeEchoPositionFinder.findSpawn(world, target,
                boundedSource.getFirst().pos(), config);
        if (safeStart.isEmpty()) {
            return false;
        }
        List<RecordedFrame> translated = ReplayFrames.translated(boundedSource, safeStart.get());
        Vec3d destination = chooseContradictionDestination(target, threadContext, translated.getLast().pos(), config);
        if ((variant == ContradictionVariant.MEMORY_ARRIVED_FIRST
                || variant == ContradictionVariant.WRONG_DESTINATION) && destination == null) {
            return false;
        }
        ItemStack alternativeItem = chooseContradictionItem(target, threadContext, recording);
        if (variant == ContradictionVariant.CONFLICTING_ITEM && alternativeItem.isEmpty()) {
            return false;
        }
        long seed = target.getUuid().getMostSignificantBits() ^ target.getUuid().getLeastSignificantBits()
                ^ memoryTick(target) ^ ((long) variant.ordinal() << 48);
        Optional<ContradictionPlan> planned = contradictionPlanner.plan(variant, translated, destination,
                alternativeItem, seed);
        if (planned.isEmpty()) {
            return false;
        }
        ContradictionPlan plan = sanitizeContradictionPlan(world, planned.get());
        if (plan == null) {
            return false;
        }
        Runnable observed = contradictionObservedCallback(target, variant, seed, forced, threadContext);
        EchoEventContext context = new EchoEventContext(target.getUuid(), config, stageManager, false,
                !forced, observed);
        ContradictionGroup group = plan.paired() ? new ContradictionGroup() : null;
        ContradictionBehavior primaryBehavior = new ContradictionBehavior(plan, false, group, observed);
        RecordedFrame first = plan.primaryFrames().getFirst();
        EchoEntity primary = createEcho(target, plan.primaryFrames(), context, primaryBehavior, first.pos(),
                first.bodyYaw(), first.pitch(), config);
        if (primary == null) {
            return false;
        }
        EchoEntity secondary = null;
        if (plan.paired()) {
            group.register(primary::finishAndDiscard);
            RecordedFrame second = plan.secondaryFrames().getFirst();
            secondary = createEcho(target, plan.secondaryFrames(), context,
                    new ContradictionBehavior(plan, true, group, observed), second.pos(),
                    second.bodyYaw(), second.pitch(), config);
            if (secondary == null) {
                group.finishAll();
                return false;
            }
            EchoEntity pairedEcho = secondary;
            group.register(pairedEcho::finishAndDiscard);
        }
        registerEcho(target, primary);
        if (secondary != null) {
            registerEcho(target, secondary);
        }
        finishSpawnBookkeeping(target, EchoType.FALSE_MEMORY, recording, first.pos(), forced, config);
        if (!forced) {
            contradictionSessionCounts.merge(target.getUuid(), 1, Integer::sum);
            recordEvent(target, EventCategory.CONTRADICTION);
        }
        return true;
    }

    private Runnable contradictionObservedCallback(ServerPlayerEntity target, ContradictionVariant variant,
                                                    long seed, boolean forced,
                                                    MemoryThreadContext threadContext) {
        Runnable base = observedCallback(target, !forced, threadContext, MemoryObservationResult.DIRECT,
                "contradiction_" + variant.commandName(), 0.055F);
        boolean[] handled = {false};
        return () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            base.run();
            if (!forced && threadContext == null) {
                contributeContaminationAndGrantTier(target, seed,
                        ContaminationSource.CONTRADICTION_COMPLETED, 0.055F, EchoProtocol.config());
            }
            if (!forced && variant == ContradictionVariant.SPLIT_MEMORY) {
                stageManager.grant(target, "two_different_endings");
            }
            if (!forced && variant == ContradictionVariant.MEMORY_ARRIVED_FIRST) {
                stageManager.grant(target, "it_was_waiting_there");
            }
        };
    }

    private void contributeContaminationAndGrantTier(ServerPlayerEntity target, long contributionId,
                                                      ContaminationSource source, float amount,
                                                      EchoConfig config) {
        ContaminationTier before = stageManager.memory(target.getUuid()).contamination().tier();
        boolean changed = stageManager.memory(target.getUuid()).contributeContamination(
                contributionId, source, amount * EchoPresetManager.values(config).contaminationGrowthMultiplier(),
                config, false);
        if (changed && before.ordinal() < ContaminationTier.DISTORTED.ordinal()
                && stageManager.memory(target.getUuid()).contamination().tier().ordinal()
                >= ContaminationTier.DISTORTED.ordinal()) {
            stageManager.grant(target, "this_is_not_how_it_happened");
        }
    }

    public boolean spawnOriginal(ServerPlayerEntity target, boolean forced, OriginalEventKind requestedEvent, EchoConfig config) {
        return spawnOriginal(target, forced, requestedEvent, null, config);
    }

    private boolean spawnOriginal(ServerPlayerEntity target, boolean forced, OriginalEventKind requestedEvent,
                                  OriginalMovementMode movementTest, EchoConfig config) {
        return spawnOriginal(target, forced, requestedEvent, movementTest, null, config);
    }

    private boolean spawnOriginal(ServerPlayerEntity target, boolean forced, OriginalEventKind requestedEvent,
                                  OriginalMovementMode movementTest, MemoryThreadContext threadContext,
                                  EchoConfig config) {
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
        Vec3d threadAnchor = threadContext == null ? null : resolveThreadAnchor(target, threadContext);
        if (threadContext != null && threadAnchor == null) {
            return false;
        }
        PlayerHabitSummary.Habit habit = threadAnchor == null
                ? chooseHabit(target, config, requestedEvent).orElse(null) : null;
        FamiliarLocation location = threadAnchor == null && habit == null
                ? chooseOriginalLocation(target, config, requestedEvent).orElse(null) : null;
        Vec3d anchor = threadAnchor != null ? threadAnchor : habit != null ? habit.position().toCenterPos()
                : location == null ? target.getPos() : location.pos().toCenterPos();
        OriginalEventKind eventKind = requestedEvent != null ? requestedEvent
                : habit != null ? chooseOriginalEvent(habit) : chooseOriginalEvent(location);
        Optional<Vec3d> spawnPos = forced
                ? SafeEchoPositionFinder.findVisibleOriginalStart(world, target, config)
                : SafeEchoPositionFinder.findOriginalStart(world, target, anchor, config);
        if (spawnPos.isEmpty() && forced) {
            spawnPos = SafeEchoPositionFinder.findOriginalStart(world, target, anchor, config);
        }
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
        ItemStack threadItem = threadContext == null ? ItemStack.EMPTY
                : visualItem(threadContext.thread().relatedItemId());
        ItemStack heldItem = !threadItem.isEmpty() ? threadItem
                : habit != null && !habit.visualItem().isEmpty() ? habit.visualItem()
                : chooseFamiliarItem(recording, target);
        List<Vec3d> knownLocations = collectOriginalAnchors(target);
        EchoEventContext context = new EchoEventContext(target.getUuid(), config, stageManager, false,
                !forced, observedCallback(target, !forced, threadContext,
                MemoryObservationResult.DIRECT, "original", 0.06F));
        EchoEntity echo = createEcho(target, List.of(frame), context,
                new OriginalEchoBehavior(context, eventKind, anchor, knownLocations, heldItem, movementTest,
                        threadAnchor != null || habit != null || location != null,
                        habit != null && habit.type() == HabitType.SLEEPING
                                || location != null && location.type() == FamiliarLocationType.BED
                                || threadContext != null && threadContext.thread().type()
                                == dev.yeldos.echoprotocol.thread.MemoryThreadType.BEDROOM,
                        threadContext == null ? "none" : threadContext.thread().type().commandName()
                                + ":" + (threadContext.thread().currentStep() + 1)), spawnPos.get(),
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

    public boolean startThread(ServerPlayerEntity player, dev.yeldos.echoprotocol.thread.MemoryThreadType type,
                               EchoConfig config, boolean awardsProgress) {
        return memoryThreads.startPlanned(player, type, config, memoryTick(player), awardsProgress);
    }

    public boolean advanceThreadForAdmin(ServerPlayerEntity player, EchoConfig config) {
        return memoryThreads.advanceForAdmin(player, config, memoryTick(player));
    }

    public boolean cancelThread(UUID playerUuid) {
        activeThreadEvents.remove(playerUuid);
        pendingSoundObservations.remove(playerUuid);
        return memoryThreads.cancel(playerUuid);
    }

    public String threadStatus(UUID playerUuid) { return memoryThreads.status(playerUuid); }

    public void clearObservationProfile(UUID playerUuid) {
        stageManager.memory(playerUuid).clearObservationProfile();
    }

    public void resetBetaData(ServerPlayerEntity player, EchoConfig config) {
        stopEvents(player);
        UUID uuid = player.getUuid();
        pendingSoundObservations.remove(uuid);
        activeThreadEvents.remove(uuid);
        panicImprints.disconnect(uuid);
        audioResidues.disconnect(uuid);
        habits.disconnect(uuid);
        roomMemories.clearSession(uuid);
        memoryThreads.clearSession(uuid);
        stageManager.memory(uuid).resetBetaData(config.memoryContaminationInitial());
    }

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
        FamiliarLocationType familiarType = switch (habit) {
            case STORAGE -> FamiliarLocationType.CHEST;
            case SLEEPING -> FamiliarLocationType.BED;
            case CRAFTING -> FamiliarLocationType.CRAFTING;
            case FURNACE -> FamiliarLocationType.FURNACE;
            case ENTRY_ROUTE -> FamiliarLocationType.DOORWAY;
            case PORTAL -> FamiliarLocationType.PORTAL;
            case IDLE -> FamiliarLocationType.IDLE;
            case FREQUENT_ITEM -> null;
        };
        if (familiarType != null) {
            roomMemories.observeInteraction(player, familiarType, pos, config, memoryTick(player));
        }
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

    public void onRespawn(ServerPlayerEntity player) {
        stopEvents(player);
        pendingSoundObservations.remove(player.getUuid());
        activeThreadEvents.remove(player.getUuid());
        recordingManager.clear(player.getUuid());
        roomMemories.clearSession(player.getUuid());
        memoryThreads.pause(player.getUuid());
    }

    public void clearPlayer(ServerPlayerEntity player) {
        stopEvents(player);
        UUID uuid = player.getUuid();
        recordingManager.clear(uuid);
        panicImprints.disconnect(player.getUuid());
        audioResidues.disconnect(player.getUuid());
        habits.disconnect(uuid);
        roomMemories.clearSession(uuid);
        memoryThreads.clearSession(uuid);
        pendingSoundObservations.remove(uuid);
        activeThreadEvents.remove(uuid);
        contradictionSessionCounts.remove(uuid);
        lastMemoryFragmentTicks.remove(uuid);
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
        var memory = stageManager.memory(target.getUuid());
        float contamination = config.memoryContaminationEnabled() ? memory.contamination().value() : 0.0F;
        EchoPresetValues preset = EchoPresetManager.values(config);
        ObservationStyle style = config.observationProfileEnabled()
                ? memory.observationProfile().style(config.observationProfileMinimumSamples())
                : ObservationStyle.UNCLASSIFIED;
        float adaptation = config.observationProfileAdaptationStrength();
        boolean night = target.getServerWorld().isNight();
        boolean underground = !target.getServerWorld().isSkyVisible(target.getBlockPos());
        boolean atHome = stageManager.familiarLocations(target).stream()
                .anyMatch(location -> location.dimension().equals(target.getServerWorld().getRegistryKey().getValue().toString())
                        && location.pos().getSquaredDistance(target.getBlockPos()) <= 16.0D * 16.0D);
        if (canSpawnType(target, EchoType.MEMORY, config)) {
            int weight = EchoPresetManager.adjustWeight(config.memoryEchoWeight(),
                    memory.contamination().authenticMemoryMinimumWeight());
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.MEMORY, Math.max(1, weight)));
        }
        if (canSpawnType(target, EchoType.CORRUPTED, config)) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.CORRUPTED,
                    EchoPresetManager.adjustWeight(config.corruptedEchoWeight(), 1.0F + contamination * 0.35F)));
        }
        if (canSpawnType(target, EchoType.MIMIC, config)) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.MIMIC,
                    EchoPresetManager.adjustWeight(config.mimicEchoWeight(), preset.strongEventWeightMultiplier())));
        }
        if (canSpawnType(target, EchoType.FALSE_MEMORY, config)) {
            float styleMultiplier = style == ObservationStyle.FOLLOWER ? 1.0F + adaptation
                    : style == ObservationStyle.AVOIDANT ? 1.0F - adaptation * 0.35F : 1.0F;
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.FALSE_MEMORY,
                    EchoPresetManager.adjustWeight(config.falseMemoryEventWeight() + (atHome ? 3 : 0),
                            (1.0F + contamination * 0.75F) * styleMultiplier)));
        }
        if (config.panicImprintsEnabled() && !panicImprints.list(target.getUuid()).isEmpty()) {
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.PANIC_IMPRINT,
                    EchoPresetManager.adjustWeight(3, preset.strongEventWeightMultiplier())));
        }
        if (canSpawnPeripheral(target, config)) {
            float styleMultiplier = style == ObservationStyle.AVOIDANT || style == ObservationStyle.DISTANT
                    ? 1.0F + adaptation : 1.0F;
            float presetMultiplier = config.intensityPreset() == dev.yeldos.echoprotocol.config.EchoIntensityPreset.SUBTLE
                    ? 1.25F : 1.0F;
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.PERIPHERAL,
                    EchoPresetManager.adjustWeight(night ? 7 : 4, styleMultiplier * presetMultiplier)));
        }
        if (audioResidues.isEligible(target, config, stageManager.tick())) {
            float styleMultiplier = style == ObservationStyle.INVESTIGATOR ? 1.0F + adaptation : 1.0F;
            float presetMultiplier = config.intensityPreset() == dev.yeldos.echoprotocol.config.EchoIntensityPreset.SUBTLE
                    ? 1.25F : 1.0F;
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.AUDIO_RESIDUE,
                    EchoPresetManager.adjustWeight(underground ? 6 : 4, styleMultiplier * presetMultiplier)));
        }
        PlayerRecording recording = recordingManager.get(target.getUuid());
        if (config.contradictoryMemoriesEnabled()
                && contradictionSessionCounts.getOrDefault(target.getUuid(), 0)
                < config.contradictionEventMaximumPerSession()
                && recording != null && recording.frames().size() >= 8) {
            float multiplier = preset.contradictionWeightMultiplier()
                    * memory.contamination().contradictionWeightMultiplier();
            candidates.add(new AdaptiveEventSelector.Candidate(EventCategory.CONTRADICTION,
                    EchoPresetManager.adjustWeight(2, multiplier)));
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
            case CONTRADICTION -> spawnContradiction(target, chooseContradictionVariant(target, config),
                    config, false);
        };
    }

    private static ContradictionVariant chooseContradictionVariant(ServerPlayerEntity target, EchoConfig config) {
        List<ContradictionVariant> available = new ArrayList<>();
        if (config.splitMemoryEnabled()) available.add(ContradictionVariant.SPLIT_MEMORY);
        if (config.repeatedEndingEnabled()) available.add(ContradictionVariant.REPEATED_ENDING);
        if (config.wrongDestinationEnabled()) available.add(ContradictionVariant.WRONG_DESTINATION);
        if (config.memoryArrivedFirstEnabled()) available.add(ContradictionVariant.MEMORY_ARRIVED_FIRST);
        if (config.conflictingItemEnabled()) available.add(ContradictionVariant.CONFLICTING_ITEM);
        if (config.missingSegmentEnabled()) available.add(ContradictionVariant.MISSING_SEGMENT);
        available.add(ContradictionVariant.CONFLICTING_COPIES);
        if (available.isEmpty()) {
            return null;
        }
        long seed = target.getUuid().getMostSignificantBits() ^ target.getUuid().getLeastSignificantBits()
                ^ target.getServerWorld().getTime();
        return available.get(Math.floorMod((int) (seed ^ (seed >>> 32)), available.size()));
    }

    private boolean spawnThreadEvent(ServerPlayerEntity target, MemoryThreadContext context, EchoConfig config) {
        if (activeThreadEvents.containsKey(target.getUuid())) {
            return false;
        }
        boolean started = switch (context.step().eventType()) {
            case AUDIO_RESIDUE -> playAudioResidue(target, config, false, context);
            case FALSE_MEMORY -> context.step().contradictionVariant() != null
                    && contradictionEnabled(context.step().contradictionVariant(), config)
                    ? spawnContradiction(target, context.step().contradictionVariant(), config, false, context)
                    : spawnFalseMemory(target, false, true, context, config);
            case PERIPHERAL_ECHO -> spawnPeripheral(target, config, false, context);
            case ORIGINAL -> spawnOriginal(target, false, originalEventFor(context), null, context, config);
            case PANIC_IMPRINT -> replayPanic(target, config, false, context);
            case CONTRADICTION -> spawnContradiction(target,
                    context.step().contradictionVariant() == null
                            ? ContradictionVariant.WRONG_DESTINATION : context.step().contradictionVariant(),
                    config, false, context);
        };
        if (!started) {
            memoryThreads.fail(target.getUuid());
            return false;
        }
        if (!memoryThreads.eventStarted(target.getUuid(), context, memoryTick(target))) {
            stopEvents(target);
            pendingSoundObservations.remove(target.getUuid());
            memoryThreads.fail(target.getUuid());
            return false;
        }
        activeThreadEvents.put(target.getUuid(), context);
        return true;
    }

    private static OriginalEventKind originalEventFor(MemoryThreadContext context) {
        return switch (context.thread().type()) {
            case BEDROOM -> OriginalEventKind.YOUR_BED;
            case STORAGE -> OriginalEventKind.WRONG_OWNER;
            case ENTRANCE -> OriginalEventKind.ALREADY_HOME;
            case PORTAL -> OriginalEventKind.EARLIER_THAN_YOU;
            case EMPTY_ROOM, IGNORED_SOUND -> OriginalEventKind.EMPTY_ROOM;
            case PANIC -> OriginalEventKind.WAITING;
            case FOLLOWED_ECHO, MISSING_ROUTE -> OriginalEventKind.CONFRONTATION;
        };
    }

    private Vec3d resolveThreadAnchor(ServerPlayerEntity target, MemoryThreadContext context) {
        String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
        if (context.room() != null && context.room().valid() && context.room().dimension().equals(dimension)
                && target.getServerWorld().isChunkLoaded(context.room().center())) {
            return context.room().center().toCenterPos();
        }
        if (context.thread().type() == dev.yeldos.echoprotocol.thread.MemoryThreadType.PANIC) {
            PanicImprint imprint = panicImprints.latest(target.getUuid());
            if (imprint != null && imprint.dimension().equals(dimension) && !imprint.frames().isEmpty()) {
                BlockPos position = BlockPos.ofFloored(imprint.frames().getLast().pos());
                if (target.getServerWorld().isChunkLoaded(position)) {
                    return imprint.frames().getLast().pos();
                }
            }
        }
        for (AudioResidue residue : audioResidues.list(target.getUuid()).reversed()) {
            if (residue.dimension().equals(dimension) && target.getServerWorld().isChunkLoaded(residue.position())) {
                return residue.position().toCenterPos();
            }
        }
        return null;
    }

    private Vec3d chooseContradictionDestination(ServerPlayerEntity target, MemoryThreadContext context,
                                                 Vec3d routeEnd, EchoConfig config) {
        String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
        if (context != null && context.room() != null && context.room().valid()
                && context.room().dimension().equals(dimension)
                && target.getServerWorld().isChunkLoaded(context.room().center())) {
            return context.room().center().toCenterPos();
        }
        double maximumSquared = config.maximumEchoSpawnDistance() * config.maximumEchoSpawnDistance();
        return stageManager.memory(target.getUuid()).roomGraph().nodes().stream()
                .filter(RoomMemoryNode::valid)
                .filter(room -> room.dimension().equals(dimension))
                .filter(room -> target.getServerWorld().isChunkLoaded(room.center()))
                .filter(room -> room.center().getSquaredDistance(target.getBlockPos()) <= maximumSquared)
                .filter(room -> room.center().toCenterPos().squaredDistanceTo(routeEnd) >= 3.0D * 3.0D)
                .sorted(java.util.Comparator.comparingDouble(RoomMemoryNode::confidence).reversed())
                .map(room -> room.center().toCenterPos()).findFirst().orElse(null);
    }

    private ItemStack chooseContradictionItem(ServerPlayerEntity target, MemoryThreadContext context,
                                              PlayerRecording recording) {
        ItemStack related = context == null ? ItemStack.EMPTY : visualItem(context.thread().relatedItemId());
        ItemStack current = recording.latest() == null ? ItemStack.EMPTY : recording.latest().heldItemVisual();
        if (!related.isEmpty() && !ItemStack.areItemsEqual(related, current)) {
            return related;
        }
        for (PlayerHabitSummary.Habit habit : habits.habits(target.getUuid())) {
            if (!habit.visualItem().isEmpty() && !ItemStack.areItemsEqual(habit.visualItem(), current)) {
                return habit.visualItem().copyWithCount(1);
            }
        }
        for (RecordedFrame frame : recording.frames().reversed()) {
            if (!frame.heldItemVisual().isEmpty()
                    && !ItemStack.areItemsEqual(frame.heldItemVisual(), current)) {
                return frame.heldItemVisual().copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ContradictionPlan sanitizeContradictionPlan(ServerWorld world, ContradictionPlan plan) {
        List<RecordedFrame> primary = safeSequence(world, plan.primaryFrames());
        List<RecordedFrame> secondary = safeSequence(world, plan.secondaryFrames());
        if (primary.size() < 2 || (plan.paired() && secondary.size() < 2)
                || (!plan.secondaryFrames().isEmpty() && secondary.size() < 2)) {
            return null;
        }
        return new ContradictionPlan(plan.variant(), primary, secondary, plan.destination(), plan.delayTicks(),
                plan.paired(), plan.requiresUnobservedTransition(), plan.seed());
    }

    private static List<RecordedFrame> safeSequence(ServerWorld world, List<RecordedFrame> frames) {
        List<RecordedFrame> safe = new ArrayList<>(Math.min(frames.size(),
                ContradictionPlan.MAXIMUM_FRAMES_PER_SEQUENCE));
        for (RecordedFrame frame : frames) {
            if (!isSafeContradictionPosition(world, frame.pos())) {
                break;
            }
            safe.add(frame);
        }
        return List.copyOf(safe);
    }

    private static boolean isSafeContradictionPosition(ServerWorld world, Vec3d position) {
        BlockPos block = BlockPos.ofFloored(position);
        if (!world.isChunkLoaded(block)) {
            return false;
        }
        Box box = new Box(position.x - 0.32D, position.y, position.z - 0.32D,
                position.x + 0.32D, position.y + 1.8D, position.z + 0.32D);
        return world.isSpaceEmpty(box)
                && !world.getBlockState(block).isOf(Blocks.LAVA)
                && !world.getBlockState(block).isOf(Blocks.FIRE)
                && !world.getBlockState(block).isOf(Blocks.SOUL_FIRE)
                && !world.getBlockState(block.down()).getCollisionShape(world, block.down()).isEmpty();
    }

    private static boolean contradictionEnabled(ContradictionVariant variant, EchoConfig config) {
        if (!config.contradictoryMemoriesEnabled() || variant == null) {
            return false;
        }
        return switch (variant) {
            case SPLIT_MEMORY -> config.splitMemoryEnabled();
            case REPEATED_ENDING -> config.repeatedEndingEnabled();
            case WRONG_DESTINATION -> config.wrongDestinationEnabled();
            case MEMORY_ARRIVED_FIRST -> config.memoryArrivedFirstEnabled();
            case CONFLICTING_ITEM -> config.conflictingItemEnabled();
            case MISSING_SEGMENT -> config.missingSegmentEnabled();
            case CONFLICTING_COPIES -> true;
        };
    }

    private static ItemStack visualItem(String itemId) {
        Identifier id = Identifier.tryParse(itemId == null ? "" : itemId);
        return id != null && Registries.ITEM.containsId(id)
                ? new ItemStack(Registries.ITEM.get(id)) : ItemStack.EMPTY;
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
        long persistentNow = memoryTick(player);
        long lastPersistentStrong = stageManager.memory(player.getUuid()).lastStrongEventTick();
        long persistentStrongSilence = (long) EchoPresetManager.strongSilenceMinutes(
                config.strongEventSilenceMinutes(), config) * 60L * 20L;
        long joinGrace = (long) config.joinEventGraceMinutes() * 60L * 20L;
        long lastThreadFinale = stageManager.memory(player.getUuid()).significantEvents().stream()
                .filter(event -> event.eventType().equals("thread_completed"))
                .mapToLong(SignificantEventRecord::tick).max().orElse(Long.MIN_VALUE / 2);
        long finaleSilence = (long) EchoPresetManager.strongSilenceMinutes(
                config.memoryThreadStrongFinaleSilenceMinutes(), config) * 60L * 20L;
        if (now - state.lastJoinTick() < joinGrace || now - state.lastRespawnTick() < 20L * 30L
                || now - dimensionChangeTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2) < 100L
                || now - lastSleepTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2) < 200L
                || lastPersistentStrong > 0L && persistentNow - lastPersistentStrong < persistentStrongSilence
                || persistentNow - lastThreadFinale < finaleSilence) {
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
            pendingSoundObservations.remove(player.getUuid());
            activeThreadEvents.remove(player.getUuid());
            roomMemories.clearSession(player.getUuid());
            memoryThreads.pause(player.getUuid());
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
        PlayerEchoState state = stageManager.state(target.getUuid());
        state.setActiveEvent(EventDirectorPolicy.lockAfterAttempt(state.activeEvent(), true));
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
        return observedCallback(target, awardsProgress, null, MemoryObservationResult.DIRECT,
                "echo", 0.0F);
    }

    private Runnable observedCallback(ServerPlayerEntity target, boolean awardsProgress,
                                      MemoryThreadContext threadContext, MemoryObservationResult result,
                                      String eventType, float contaminationContribution) {
        return observedCallback(target, awardsProgress, threadContext, () -> result,
                eventType, contaminationContribution);
    }

    private Runnable observedCallback(ServerPlayerEntity target, boolean awardsProgress,
                                      MemoryThreadContext threadContext,
                                      Supplier<MemoryObservationResult> resultSupplier,
                                      String eventType, float contaminationContribution) {
        boolean[] handled = {false};
        return () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            MemoryObservationResult result = resultSupplier.get();
            if (result == null) {
                result = MemoryObservationResult.DIRECT;
            }
            eventHistory.markLastObserved(target.getUuid());
            if (EchoProtocol.config().observationProfileEnabled()) {
                ObservationMetric metric = switch (result) {
                    case FOLLOWED -> ObservationMetric.ECHO_FOLLOWED;
                    case APPROACHED -> ObservationMetric.APPROACH;
                    case RETREATED -> ObservationMetric.RETREAT;
                    case MISSED, NOT_PRESENT -> ObservationMetric.EVENT_MISSED;
                    default -> ObservationMetric.DIRECT_OBSERVATION;
                };
                stageManager.memory(target.getUuid()).recordObservation(metric, 1.0F, null);
            }
            if (awardsProgress && threadContext == null) {
                RoomMemoryNode room = roomMemories.currentRoom(target, 16.0D);
                long roomId = room == null ? 0L : room.id();
                String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
                boolean strong = eventType.equals("mimic") || eventType.equals("original")
                        || eventType.equals("panic_imprint") || eventType.startsWith("contradiction_");
                stageManager.memory(target.getUuid()).recordSignificantEvent(
                        new SignificantEventRecord(eventType, dimension, roomId, memoryTick(target), result, strong),
                        EchoProtocol.config().persistentSignificantEventMaximum());
            }
            if (threadContext != null) {
                memoryThreads.outcome(target, threadContext, result, eventType, contaminationContribution,
                        EchoProtocol.config(), memoryTick(target));
                activeThreadEvents.remove(target.getUuid(), threadContext);
                if (awardsProgress && eventType.equals("original")
                        && stageManager.memory(target.getUuid()).significantEvents().stream()
                        .anyMatch(event -> event.roomNodeId() == threadContext.thread().selectedRoomNodeId()
                                && event.eventType().equals("false_memory")
                                && event.observation() == MemoryObservationResult.FOLLOWED)) {
                    stageManager.grant(target, "you_led_it_here");
                }
                maybeSendMemoryFragment(target, threadContext, EchoProtocol.config());
            }
            if (awardsProgress) {
                stageManager.grant(target, "deja_vu");
                if (EchoProtocol.config().realPlayerSkins()
                        && target.getGameProfile().getProperties().containsKey("textures")) {
                    stageManager.grant(target, "familiar_face");
                }
            }
        };
    }

    private void tickPendingSoundObservations(MinecraftServer server, EchoConfig config) {
        long now = stageManager.tick();
        long persistentNow = memoryTick(server);
        pendingSoundObservations.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            PendingSoundObservation pending = entry.getValue();
            if (player == null || !player.isAlive()) {
                memoryThreads.fail(entry.getKey());
                return true;
            }
            double distance = player.getPos().distanceTo(pending.position());
            if (distance <= 3.5D || distance + 2.0D < pending.initialDistance()) {
                stageManager.memory(entry.getKey()).recordObservation(
                        ObservationMetric.SOUND_INVESTIGATED, 1.0F, (float) distance);
                memoryThreads.outcome(player, pending.threadContext(), MemoryObservationResult.INVESTIGATED_SOUND,
                        "audio_residue", 0.025F, config, persistentNow);
                activeThreadEvents.remove(entry.getKey(), pending.threadContext());
                return true;
            }
            if (now - pending.startedTick() >= 200L) {
                stageManager.memory(entry.getKey()).recordObservation(
                        ObservationMetric.SOUND_IGNORED, 1.0F, (float) distance);
                memoryThreads.outcome(player, pending.threadContext(), MemoryObservationResult.IGNORED_SOUND,
                        "audio_residue", 0.012F, config, persistentNow);
                activeThreadEvents.remove(entry.getKey(), pending.threadContext());
                return true;
            }
            return false;
        });
    }

    private record PendingSoundObservation(Vec3d position, double initialDistance, long startedTick,
                                           MemoryThreadContext threadContext) {
    }

    private void maybeSendMemoryFragment(ServerPlayerEntity target, MemoryThreadContext context,
                                         EchoConfig config) {
        if (!context.thread().awardsProgress() || !config.betaMemoryTextFragmentsEnabled()) {
            return;
        }
        long interval = (long) config.betaMemoryTextMinimumIntervalMinutes() * 60L * 20L;
        long previous = lastMemoryFragmentTicks.getOrDefault(target.getUuid(), Long.MIN_VALUE / 2);
        long reference = context.eventReference() ^ target.getUuid().getMostSignificantBits();
        if (stageManager.tick() - previous < interval || Math.floorMod(reference, 5L) != 0L) {
            return;
        }
        int fragment = Math.floorMod((int) (reference ^ (reference >>> 32)), 8);
        target.sendMessage(Text.translatable("text.echoprotocol.memory_fragment." + fragment), true);
        lastMemoryFragmentTicks.put(target.getUuid(), stageManager.tick());
    }

    private void finalizeMissingThreadEvents() {
        activeThreadEvents.entrySet().removeIf(entry -> {
            if (pendingSoundObservations.containsKey(entry.getKey())) {
                return false;
            }
            boolean entityActive = activeEchoes.getOrDefault(entry.getKey(), List.of()).stream()
                    .anyMatch(echo -> !echo.isRemoved());
            if (entityActive) {
                return false;
            }
            memoryThreads.missed(entry.getKey(), entry.getValue());
            stageManager.memory(entry.getKey()).recordObservation(ObservationMetric.EVENT_MISSED, 1.0F, null);
            return true;
        });
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
            stageManager.state(entry.getKey()).setActiveEvent(true);
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

    private static long memoryTick(MinecraftServer server) {
        return Math.max(0L, server.getOverworld().getTime());
    }

    private static long memoryTick(ServerPlayerEntity player) {
        return memoryTick(player.getServer());
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
        roomMemories.clearAll();
        memoryThreads.clearAll();
        pendingSoundObservations.clear();
        activeThreadEvents.clear();
        contradictionSessionCounts.clear();
        lastMemoryFragmentTicks.clear();
        EchoSoundPlayer.clearAll();
        lastGlobalMimicTick = -9999999L;
    }
}
