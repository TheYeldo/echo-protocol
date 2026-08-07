package dev.yeldos.echoprotocol.thread;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.config.EchoPresetManager;
import dev.yeldos.echoprotocol.contamination.ContaminationSource;
import dev.yeldos.echoprotocol.contamination.ContaminationTier;
import dev.yeldos.echoprotocol.memory.PlayerMemoryState;
import dev.yeldos.echoprotocol.memory.SignificantEventRecord;
import dev.yeldos.echoprotocol.room.RoomMemoryManager;
import dev.yeldos.echoprotocol.room.RoomMemoryNode;
import dev.yeldos.echoprotocol.stage.EchoStage;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MemoryThreadManager {
    private final StageManager stageManager;
    private final RoomMemoryManager rooms;
    private final MemoryThreadPlanner planner = new MemoryThreadPlanner();
    private final Map<UUID, Long> lastPlanningAttemptTicks = new HashMap<>();
    private long eventSequence;

    public MemoryThreadManager(StageManager stageManager, RoomMemoryManager rooms) {
        this.stageManager = stageManager;
        this.rooms = rooms;
    }

    public void tick(MinecraftServer server, EchoConfig config, long tick) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerMemoryState memory = stageManager.memory(player.getUuid());
            memory.recoverContamination(tick, config);
            memory.decayObservationProfile(tick, config);
            MemoryThread active = memory.activeThread();
            if (active != null && active.stage() == MemoryThreadStage.PAUSED
                    && config.memoryThreadResumeAfterRestart()) {
                memory.resumeThread(active.resumedAfterRestart());
            }
            if (active == null && eligibleForPlanning(player, memory, config, tick)) {
                startPlanned(player, null, config, tick, true);
            }
        }
    }

    private boolean eligibleForPlanning(ServerPlayerEntity player, PlayerMemoryState memory, EchoConfig config, long tick) {
        if (!config.memoryThreadsEnabled() || stageManager.state(player.getUuid()).stage().id() < EchoStage.CORRUPTED_MEMORY.id()) {
            return false;
        }
        long baseInterval = (long) config.memoryThreadMinimumIntervalMinutes() * 60L * 20L;
        float activity = EchoPresetManager.values(config).threadActivityMultiplier();
        long interval = Math.max(20L * 60L, Math.round(baseInterval / Math.max(0.4F, activity)));
        long lastAttempt = lastPlanningAttemptTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2);
        long lastCompleted = memory.significantEvents().stream()
                .filter(event -> event.eventType().equals("thread_completed"))
                .mapToLong(SignificantEventRecord::tick).max().orElse(Long.MIN_VALUE / 2);
        return tick - lastAttempt >= interval && tick - lastCompleted >= interval;
    }

    public boolean startPlanned(ServerPlayerEntity player, MemoryThreadType requestedType, EchoConfig config,
                                long tick, boolean awardsProgress) {
        PlayerMemoryState memory = stageManager.memory(player.getUuid());
        if (!config.memoryThreadsEnabled() || memory.activeThread() != null) {
            return false;
        }
        lastPlanningAttemptTicks.put(player.getUuid(), tick);
        RoomMemoryNode room = chooseRoom(player, memory, config);
        MemoryThreadType type = requestedType == null ? chooseType(player, memory, room, tick) : requestedType;
        if (type == null || (room == null && requiresRoom(type))) {
            return false;
        }
        long roomId = room == null ? 0L : room.id();
        String dimension = room == null
                ? player.getEntityWorld().getRegistryKey().getValue().toString() : room.dimension();
        String relatedItem = memory.habits().stream()
                .filter(habit -> habit.dimension().equals(dimension) && !habit.visualItemId().isBlank())
                .map(habit -> habit.visualItemId()).findFirst().orElse("");
        long seed = player.getUuid().getMostSignificantBits() ^ player.getUuid().getLeastSignificantBits()
                ^ tick ^ (roomId * 31L) ^ type.ordinal();
        Optional<MemoryThread> planned = planner.plan(player.getUuid(), type, roomId, dimension, relatedItem,
                tick, seed, ThreadPlanningOptions.from(config), awardsProgress);
        return planned.isPresent() && memory.setActiveThread(planned.get());
    }

    public Optional<MemoryThreadContext> nextStep(ServerPlayerEntity player, EchoConfig config, long tick) {
        if (!config.memoryThreadsEnabled()) {
            return Optional.empty();
        }
        PlayerMemoryState memory = stageManager.memory(player.getUuid());
        MemoryThread thread = memory.activeThread();
        if (thread == null || thread.stage() != MemoryThreadStage.ACTIVE || thread.nextStep() == null) {
            return Optional.empty();
        }
        if (!thread.selectedDimension().isBlank()
                && !thread.selectedDimension().equals(player.getEntityWorld().getRegistryKey().getValue().toString())) {
            return Optional.empty();
        }
        RoomMemoryNode room = thread.selectedRoomNodeId() == 0L ? null
                : memory.roomGraph().node(thread.selectedRoomNodeId()).orElse(null);
        if (thread.selectedRoomNodeId() != 0L && (room == null || !room.valid())) {
            cancel(player.getUuid());
            return Optional.empty();
        }
        if (thread.nextStep().strong() && room != null
                && room.confidence() < config.roomMemoryStrongEventConfidence()) {
            fail(player.getUuid());
            return Optional.empty();
        }
        if (!featureEnabled(thread.nextStep(), config)) {
            fail(player.getUuid());
            return Optional.empty();
        }
        long reference = (++eventSequence << 20) ^ tick ^ thread.currentStep();
        return Optional.of(new MemoryThreadContext(thread, thread.nextStep(), room, reference));
    }

    public boolean eventStarted(UUID playerUuid, MemoryThreadContext context, long tick) {
        PlayerMemoryState memory = stageManager.memory(playerUuid);
        return memory.activeThread() == context.thread() && memory.threadEventStarted(context.eventReference(), tick);
    }

    public boolean outcome(ServerPlayerEntity player, MemoryObservationResult result, String eventType,
                           float contaminationContribution, EchoConfig config, long tick) {
        PlayerMemoryState memory = stageManager.memory(player.getUuid());
        MemoryThread thread = memory.activeThread();
        if (thread == null) {
            return false;
        }
        int previousStep = thread.currentStep();
        ContaminationTier before = memory.contamination().tier();
        float effectiveContribution = contaminationContribution
                * EchoPresetManager.values(config).contaminationGrowthMultiplier();
        boolean advanced = memory.applyThreadOutcome(result, effectiveContribution);
        if (!advanced) {
            return false;
        }
        if (thread.awardsProgress()) {
            memory.contributeContamination(thread.eventReferences().isEmpty() ? tick : thread.eventReferences().getLast(),
                    ContaminationSource.THREAD_ADVANCED, effectiveContribution, config, false);
        }
        long room = thread.selectedRoomNodeId();
        memory.recordSignificantEvent(new SignificantEventRecord(eventType, thread.selectedDimension(), room, tick,
                result, thread.nextStep() == null || previousStep == thread.totalSteps() - 1),
                config.persistentSignificantEventMaximum());
        if (thread.awardsProgress() && thread.currentStep() >= 3) {
            stageManager.grant(player, "a_pattern_emerges");
        }
        if (thread.awardsProgress() && thread.resumedAfterRestart()) {
            stageManager.grant(player, "not_forgotten");
        }
        if (thread.stage() == MemoryThreadStage.COMPLETED) {
            memory.recordSignificantEvent(new SignificantEventRecord("thread_completed", thread.selectedDimension(),
                    room, tick, result, true), config.persistentSignificantEventMaximum());
            if (thread.awardsProgress()) {
                stageManager.grant(player, "the_house_remembers");
            }
            memory.clearCompletedThread();
        }
        if (thread.awardsProgress() && before.ordinal() < ContaminationTier.DISTORTED.ordinal()
                && memory.contamination().tier().ordinal() >= ContaminationTier.DISTORTED.ordinal()) {
            stageManager.grant(player, "this_is_not_how_it_happened");
        }
        return true;
    }

    public boolean outcome(ServerPlayerEntity player, MemoryThreadContext context,
                           MemoryObservationResult result, String eventType,
                           float contaminationContribution, EchoConfig config, long tick) {
        MemoryThread active = stageManager.memory(player.getUuid()).activeThread();
        if (context == null || active != context.thread()
                || !active.eventReferences().contains(context.eventReference())) {
            return false;
        }
        return outcome(player, result, eventType, contaminationContribution, config, tick);
    }

    public void missed(UUID playerUuid, MemoryThreadContext context) {
        MemoryThread active = stageManager.memory(playerUuid).activeThread();
        if (context != null && active == context.thread()
                && active.eventReferences().contains(context.eventReference())) {
            stageManager.memory(playerUuid).applyThreadOutcome(MemoryObservationResult.MISSED, 0.0F);
            if (active.consecutiveFailures() >= 3) {
                stageManager.memory(playerUuid).clearThread();
            }
        }
    }

    public void fail(UUID playerUuid) {
        PlayerMemoryState memory = stageManager.memory(playerUuid);
        MemoryThread thread = memory.activeThread();
        if (thread == null) {
            return;
        }
        memory.applyThreadOutcome(MemoryObservationResult.EVENT_FAILED, 0.0F);
        if (thread.consecutiveFailures() >= 3) {
            memory.clearThread();
        }
    }

    public boolean advanceForAdmin(ServerPlayerEntity player, EchoConfig config, long tick) {
        MemoryThread thread = stageManager.memory(player.getUuid()).activeThread();
        if (thread == null || thread.nextStep() == null) {
            return false;
        }
        MemoryObservationResult result = thread.nextStep().eventType() == MemoryThreadEventType.AUDIO_RESIDUE
                ? MemoryObservationResult.INVESTIGATED_SOUND : MemoryObservationResult.DIRECT;
        return outcome(player, result, "admin_thread_advance", 0.0F, config, tick);
    }

    public boolean cancel(UUID playerUuid) {
        return stageManager.memory(playerUuid).clearThread();
    }

    public void pause(UUID playerUuid) {
        stageManager.memory(playerUuid).pauseThread();
    }

    public String status(UUID playerUuid) {
        MemoryThread thread = stageManager.memory(playerUuid).activeThread();
        if (thread == null) {
            return "inactive";
        }
        return "type=" + thread.type().commandName() + ", stage=" + thread.stage().name().toLowerCase(Locale.ROOT)
                + ", step=" + (thread.currentStep() + 1) + "/" + thread.totalSteps()
                + ", next=" + (thread.nextStep() == null ? "none" : thread.nextStep().eventType().name().toLowerCase(Locale.ROOT))
                + ", room=" + thread.selectedRoomNodeId() + ", failures=" + thread.consecutiveFailures();
    }

    public void clearSession(UUID playerUuid) {
        lastPlanningAttemptTicks.remove(playerUuid);
        pause(playerUuid);
    }

    public void clearAll() {
        lastPlanningAttemptTicks.clear();
    }

    private RoomMemoryNode chooseRoom(ServerPlayerEntity player, PlayerMemoryState memory, EchoConfig config) {
        RoomMemoryNode current = rooms.currentRoom(player, config.roomMemoryProbeRadius() + 4.0D);
        if (current != null && current.confidence() >= config.roomMemoryMinimumConfidence()) {
            return current;
        }
        String dimension = player.getEntityWorld().getRegistryKey().getValue().toString();
        return memory.roomGraph().nodes().stream().filter(RoomMemoryNode::valid)
                .filter(room -> room.dimension().equals(dimension))
                .filter(room -> room.confidence() >= config.roomMemoryMinimumConfidence())
                .sorted(Comparator.comparingDouble(RoomMemoryNode::confidence).thenComparingInt(RoomMemoryNode::visits).reversed())
                .findFirst().orElse(null);
    }

    private static MemoryThreadType chooseType(ServerPlayerEntity player, PlayerMemoryState memory,
                                               RoomMemoryNode room, long tick) {
        List<MemoryThreadType> candidates = new ArrayList<>();
        if (room != null) {
            candidates.add(MemoryThreadType.forRoom(room.type()));
        }
        if (!memory.panicImprints().isEmpty()) candidates.add(MemoryThreadType.PANIC);
        if (!memory.audioResidues().isEmpty()) candidates.add(MemoryThreadType.IGNORED_SOUND);
        candidates.add(MemoryThreadType.MISSING_ROUTE);
        MemoryThreadType last = memory.recentCompletedThreads().isEmpty()
                ? null : memory.recentCompletedThreads().getLast();
        candidates.removeIf(type -> type == last);
        if (candidates.isEmpty()) {
            return null;
        }
        long seed = player.getUuid().getMostSignificantBits() ^ player.getUuid().getLeastSignificantBits() ^ tick;
        return candidates.get(Math.floorMod((int) (seed ^ (seed >>> 32)), candidates.size()));
    }

    private static boolean requiresRoom(MemoryThreadType type) {
        return type != MemoryThreadType.PANIC && type != MemoryThreadType.MISSING_ROUTE
                && type != MemoryThreadType.FOLLOWED_ECHO && type != MemoryThreadType.IGNORED_SOUND;
    }

    private static boolean featureEnabled(MemoryThreadStep step, EchoConfig config) {
        return switch (step.eventType()) {
            case AUDIO_RESIDUE -> config.audioResidueEnabled() && config.threadAwareAudioResidueEnabled();
            case FALSE_MEMORY -> config.falseMemoriesEnabled() && config.threadAwareFalseMemoriesEnabled();
            case PERIPHERAL_ECHO -> config.peripheralEchoesEnabled() && config.threadAwarePeripheralEchoesEnabled();
            case ORIGINAL -> config.originalEnabled() && config.threadAwareOriginalEnabled();
            case PANIC_IMPRINT -> config.panicImprintsEnabled();
            case CONTRADICTION -> config.contradictoryMemoriesEnabled();
        };
    }
}
