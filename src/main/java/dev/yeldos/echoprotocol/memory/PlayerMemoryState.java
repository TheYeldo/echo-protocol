package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.audio.AudioResidue;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.contamination.ContaminationSource;
import dev.yeldos.echoprotocol.contamination.MemoryContaminationState;
import dev.yeldos.echoprotocol.profile.ObservationMetric;
import dev.yeldos.echoprotocol.profile.ObservationProfile;
import dev.yeldos.echoprotocol.room.RoomMemoryGraph;
import dev.yeldos.echoprotocol.thread.MemoryObservationResult;
import dev.yeldos.echoprotocol.thread.MemoryThread;
import dev.yeldos.echoprotocol.thread.MemoryThreadStage;
import dev.yeldos.echoprotocol.thread.MemoryThreadType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public final class PlayerMemoryState {
    public static final int MAXIMUM_COMPLETED_THREADS = 8;
    public static final int MAXIMUM_LOAD_WARNINGS = 16;

    private int schemaVersion = MemoryDataVersion.CURRENT;
    private final Deque<PersistentPanicImprint> panicImprints = new ArrayDeque<>();
    private final Deque<AudioResidue> audioResidues = new ArrayDeque<>();
    private final List<PersistentHabit> habits = new ArrayList<>();
    private final RoomMemoryGraph roomGraph = new RoomMemoryGraph();
    private MemoryThread activeThread;
    private final Deque<MemoryThreadType> recentCompletedThreads = new ArrayDeque<>();
    private MemoryContaminationState contamination;
    private ObservationProfile observationProfile = new ObservationProfile();
    private long lastStrongEventTick;
    private final Deque<SignificantEventRecord> significantEvents = new ArrayDeque<>();
    private final Deque<FalseMemorySeed> falseMemorySeeds = new ArrayDeque<>();
    private final List<String> loadWarnings = new ArrayList<>();
    private boolean loadedFromPreviousSession;
    private transient Runnable mutationListener = () -> { };

    public PlayerMemoryState(float initialContamination) {
        contamination = new MemoryContaminationState(initialContamination);
        roomGraph.setMutationListener(this::markDirty);
    }

    public PlayerMemoryState() {
        this(0.0F);
    }

    public void setMutationListener(Runnable listener) {
        mutationListener = listener == null ? () -> { } : listener;
        roomGraph.setMutationListener(this::markDirty);
    }

    public void addPanicImprint(PersistentPanicImprint imprint, int maximum) {
        if (imprint == null || maximum <= 0) {
            return;
        }
        panicImprints.addLast(imprint);
        trimDeque(panicImprints, Math.min(3, maximum));
        markDirty();
    }

    public int clearPanicImprints() {
        int count = panicImprints.size();
        panicImprints.clear();
        if (count > 0) markDirty();
        return count;
    }

    public void addAudioResidue(AudioResidue residue, int maximum) {
        if (residue == null || maximum <= 0) {
            return;
        }
        audioResidues.addLast(residue);
        trimDeque(audioResidues, Math.min(16, maximum));
        markDirty();
    }

    public int clearAudioResidues() {
        int count = audioResidues.size();
        audioResidues.clear();
        if (count > 0) markDirty();
        return count;
    }

    public void addHabit(PersistentHabit habit, int maximum) {
        if (habit == null || maximum <= 0) {
            return;
        }
        for (int index = 0; index < habits.size(); index++) {
            if (habits.get(index).matches(habit)) {
                habits.set(index, habits.get(index).reinforced(habit));
                trimHabits(maximum);
                markDirty();
                return;
            }
        }
        habits.add(habit);
        trimHabits(maximum);
        markDirty();
    }

    public int clearHabits() {
        int count = habits.size();
        habits.clear();
        if (count > 0) markDirty();
        return count;
    }

    public boolean setActiveThread(MemoryThread thread) {
        if (thread != null && thread.stage() != MemoryThreadStage.ACTIVE
                && thread.stage() != MemoryThreadStage.PAUSED) {
            return false;
        }
        if (activeThread != null && activeThread.stage() == MemoryThreadStage.ACTIVE && thread != null) {
            return false;
        }
        activeThread = thread;
        markDirty();
        return true;
    }

    public boolean threadEventStarted(long reference, long tick) {
        boolean changed = activeThread != null && activeThread.eventStarted(reference, tick);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean applyThreadOutcome(MemoryObservationResult result, float contribution) {
        if (activeThread == null) {
            return false;
        }
        boolean advanced = activeThread.applyOutcome(result, contribution);
        if (advanced) {
            if (activeThread.stage() == MemoryThreadStage.COMPLETED) {
                recentCompletedThreads.addLast(activeThread.type());
                trimDeque(recentCompletedThreads, MAXIMUM_COMPLETED_THREADS);
            }
            markDirty();
        } else if (result != null) {
            markDirty();
        }
        return advanced;
    }

    public boolean clearThread() {
        if (activeThread == null) {
            return false;
        }
        activeThread.cancel();
        activeThread = null;
        markDirty();
        return true;
    }

    public boolean pauseThread() {
        if (activeThread == null || activeThread.stage() != MemoryThreadStage.ACTIVE) {
            return false;
        }
        activeThread.pause();
        markDirty();
        return true;
    }

    public boolean resumeThread(boolean afterRestart) {
        if (activeThread == null || activeThread.stage() != MemoryThreadStage.PAUSED) {
            return false;
        }
        activeThread.resume(afterRestart);
        markDirty();
        return true;
    }

    public void clearCompletedThread() {
        if (activeThread != null && activeThread.stage() == MemoryThreadStage.COMPLETED) {
            activeThread = null;
            markDirty();
        }
    }

    public boolean contributeContamination(long contributionId, ContaminationSource source, float amount,
                                           EchoConfig config, boolean admin) {
        if (!config.memoryContaminationEnabled()) {
            return false;
        }
        boolean changed = contamination.contribute(contributionId, admin ? ContaminationSource.ADMIN : source,
                amount, config.memoryContaminationGrowthMultiplier(), config.memoryContaminationMaximum(),
                config.memoryContaminationAdminTestsAffectState());
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean recoverContamination(long tick, EchoConfig config) {
        boolean changed = config.memoryContaminationEnabled()
                && contamination.recover(tick, config.memoryContaminationPassiveRecoveryPerHour(),
                config.memoryContaminationMaximum());
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean reduceContamination(long contributionId, float amount, EchoConfig config) {
        boolean changed = config.memoryContaminationEnabled()
                && contamination.reduce(contributionId, amount, config.memoryContaminationMaximum());
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean setContamination(float value, EchoConfig config) {
        boolean changed = contamination.set(value, config.memoryContaminationMaximum());
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public void recordObservation(ObservationMetric metric, float amount, Float distance) {
        observationProfile.record(metric, amount);
        if (distance != null) {
            observationProfile.recordDistance(distance);
        }
        markDirty();
    }

    public boolean decayObservationProfile(long tick, EchoConfig config) {
        boolean changed = config.observationProfileEnabled()
                && observationProfile.decay(tick, config.observationProfileDecayPerHour());
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public void clearObservationProfile() {
        observationProfile = new ObservationProfile();
        markDirty();
    }

    public void recordSignificantEvent(SignificantEventRecord event, int maximum) {
        if (event == null || maximum <= 0) {
            return;
        }
        significantEvents.addLast(event);
        trimDeque(significantEvents, Math.min(24, maximum));
        if (event.strong()) {
            lastStrongEventTick = Math.max(lastStrongEventTick, event.tick());
        }
        markDirty();
    }

    public void addFalseMemorySeed(FalseMemorySeed seed, int maximum) {
        if (seed == null || maximum <= 0) {
            return;
        }
        falseMemorySeeds.addLast(seed);
        trimDeque(falseMemorySeeds, Math.min(4, maximum));
        markDirty();
    }

    public void applyBounds(EchoConfig config) {
        trimDeque(panicImprints, config.persistentPanicImprintMaximum());
        trimDeque(audioResidues, config.persistentAudioResidueMaximum());
        trimHabits(config.persistentHabitMaximum());
        roomGraph.trim(config.roomMemoryMaximumNodes(), config.roomMemoryMaximumEdges());
        trimDeque(recentCompletedThreads, MAXIMUM_COMPLETED_THREADS);
        trimDeque(significantEvents, config.persistentSignificantEventMaximum());
        trimDeque(falseMemorySeeds, config.persistentFalseMemorySeedMaximum());
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>(roomGraph.validate());
        if (schemaVersion != MemoryDataVersion.CURRENT) {
            errors.add("unexpected player memory version " + schemaVersion);
        }
        if (panicImprints.size() > 3 || audioResidues.size() > 16 || habits.size() > 16
                || significantEvents.size() > 24 || falseMemorySeeds.size() > 4
                || recentCompletedThreads.size() > MAXIMUM_COMPLETED_THREADS) {
            errors.add("one or more persistent collections exceed hard bounds");
        }
        if (activeThread != null && activeThread.totalSteps() > MemoryThread.MAXIMUM_STEPS) {
            errors.add("active thread exceeds step bound");
        }
        errors.addAll(loadWarnings);
        return List.copyOf(errors);
    }

    public void resetBetaData(float initialContamination) {
        panicImprints.clear();
        audioResidues.clear();
        habits.clear();
        roomGraph.clear();
        activeThread = null;
        recentCompletedThreads.clear();
        contamination = new MemoryContaminationState(initialContamination);
        observationProfile = new ObservationProfile();
        lastStrongEventTick = 0L;
        significantEvents.clear();
        falseMemorySeeds.clear();
        loadWarnings.clear();
        loadedFromPreviousSession = false;
        markDirty();
    }

    public void addLoadWarning(String warning) {
        if (warning != null && !warning.isBlank() && loadWarnings.size() < MAXIMUM_LOAD_WARNINGS) {
            loadWarnings.add(warning.substring(0, Math.min(160, warning.length())));
        }
    }

    public void markLoadedFromPreviousSession() { loadedFromPreviousSession = true; }
    public void restoreSchemaVersion(int version) { schemaVersion = version; }
    public void restorePanic(PersistentPanicImprint imprint) { if (panicImprints.size() < 3) panicImprints.addLast(imprint); }
    public void restoreAudio(AudioResidue residue) { if (audioResidues.size() < 16) audioResidues.addLast(residue); }
    public void restoreHabit(PersistentHabit habit) { if (habits.size() < 16) habits.add(habit); }
    public void restoreThread(MemoryThread thread) { activeThread = thread; }
    public void restoreCompletedThread(MemoryThreadType type) { if (recentCompletedThreads.size() < 8) recentCompletedThreads.addLast(type); }
    public void restoreContamination(MemoryContaminationState state) { contamination = state; }
    public void restoreObservationProfile(ObservationProfile profile) { observationProfile = profile; }
    public void restoreLastStrongEventTick(long tick) { lastStrongEventTick = Math.max(0L, tick); }
    public void restoreSignificantEvent(SignificantEventRecord event) { if (significantEvents.size() < 24) significantEvents.addLast(event); }
    public void restoreFalseMemorySeed(FalseMemorySeed seed) { if (falseMemorySeeds.size() < 4) falseMemorySeeds.addLast(seed); }

    public int schemaVersion() { return schemaVersion; }
    public List<PersistentPanicImprint> panicImprints() { return List.copyOf(panicImprints); }
    public List<AudioResidue> audioResidues() { return List.copyOf(audioResidues); }
    public List<PersistentHabit> habits() { return List.copyOf(habits); }
    public RoomMemoryGraph roomGraph() { return roomGraph; }
    public MemoryThread activeThread() { return activeThread; }
    public List<MemoryThreadType> recentCompletedThreads() { return List.copyOf(recentCompletedThreads); }
    public MemoryContaminationState contamination() { return contamination; }
    public ObservationProfile observationProfile() { return observationProfile; }
    public long lastStrongEventTick() { return lastStrongEventTick; }
    public List<SignificantEventRecord> significantEvents() { return List.copyOf(significantEvents); }
    public List<FalseMemorySeed> falseMemorySeeds() { return List.copyOf(falseMemorySeeds); }
    public boolean loadedFromPreviousSession() { return loadedFromPreviousSession; }
    public List<String> loadWarnings() { return List.copyOf(loadWarnings); }

    private void trimHabits(int maximum) {
        int limit = Math.max(0, Math.min(16, maximum));
        habits.sort(Comparator.comparingInt(PersistentHabit::observations)
                .thenComparingLong(PersistentHabit::lastSeenTick).reversed());
        while (habits.size() > limit) {
            habits.removeLast();
        }
    }

    private static <T> void trimDeque(Deque<T> deque, int maximum) {
        int limit = Math.max(0, maximum);
        while (deque.size() > limit) {
            deque.removeFirst();
        }
    }

    private void markDirty() { mutationListener.run(); }
}
