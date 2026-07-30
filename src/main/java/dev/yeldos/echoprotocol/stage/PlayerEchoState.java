package dev.yeldos.echoprotocol.stage;

import java.util.ArrayList;
import java.util.List;

public final class PlayerEchoState {
    private EchoStage stage = EchoStage.OBSERVATION;
    private long playTicks;
    private long nextEventTick;
    private long nextOriginalEventTick;
    private long lastJoinTick;
    private long lastRespawnTick;
    private int stageOneEvents;
    private int totalEvents;
    private int memoryEvents;
    private int corruptedEvents;
    private int mimicEvents;
    private int originalEvents;
    private boolean mimicIndependentActionSeen;
    private boolean activeEvent;
    private final List<FamiliarLocation> familiarLocations = new ArrayList<>();
    private transient Runnable persistentMutationListener = () -> { };

    public void setPersistentMutationListener(Runnable listener) {
        persistentMutationListener = listener == null ? () -> { } : listener;
    }

    public void markPersistentChanged() {
        persistentMutationListener.run();
    }

    public EchoStage stage() {
        return stage;
    }

    public void setStage(EchoStage stage) {
        if (this.stage != stage) {
            this.stage = stage;
            markPersistentChanged();
        }
    }

    public long playTicks() {
        return playTicks;
    }

    public void addPlayTick() {
        playTicks++;
    }

    public void setPlayTicks(long playTicks) {
        this.playTicks = Math.max(0L, playTicks);
    }

    public long nextEventTick() {
        return nextEventTick;
    }

    public void setNextEventTick(long nextEventTick) {
        this.nextEventTick = nextEventTick;
        markPersistentChanged();
    }

    public long nextOriginalEventTick() {
        return nextOriginalEventTick;
    }

    public void setNextOriginalEventTick(long nextOriginalEventTick) {
        this.nextOriginalEventTick = Math.max(0L, nextOriginalEventTick);
        markPersistentChanged();
    }

    public long lastJoinTick() {
        return lastJoinTick;
    }

    public void setLastJoinTick(long lastJoinTick) {
        this.lastJoinTick = lastJoinTick;
    }

    public long lastRespawnTick() {
        return lastRespawnTick;
    }

    public void setLastRespawnTick(long lastRespawnTick) {
        this.lastRespawnTick = lastRespawnTick;
    }

    public int stageOneEvents() {
        return stageOneEvents;
    }

    public void incrementStageOneEvents() {
        stageOneEvents++;
        markPersistentChanged();
    }

    public void setStageOneEvents(int stageOneEvents) {
        this.stageOneEvents = Math.max(0, stageOneEvents);
    }

    public int totalEvents() {
        return totalEvents;
    }

    public void incrementTotalEvents() {
        totalEvents++;
        markPersistentChanged();
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = Math.max(0, totalEvents);
    }

    public int memoryEvents() {
        return memoryEvents;
    }

    public void setMemoryEvents(int memoryEvents) {
        this.memoryEvents = Math.max(0, memoryEvents);
    }

    public int corruptedEvents() {
        return corruptedEvents;
    }

    public void setCorruptedEvents(int corruptedEvents) {
        this.corruptedEvents = Math.max(0, corruptedEvents);
    }

    public int mimicEvents() {
        return mimicEvents;
    }

    public void setMimicEvents(int mimicEvents) {
        this.mimicEvents = Math.max(0, mimicEvents);
    }

    public int originalEvents() {
        return originalEvents;
    }

    public void incrementOriginalEvents() {
        originalEvents++;
        markPersistentChanged();
    }

    public void setOriginalEvents(int originalEvents) {
        this.originalEvents = Math.max(0, originalEvents);
    }

    public boolean mimicIndependentActionSeen() {
        return mimicIndependentActionSeen;
    }

    public void setMimicIndependentActionSeen(boolean mimicIndependentActionSeen) {
        if (this.mimicIndependentActionSeen != mimicIndependentActionSeen) {
            this.mimicIndependentActionSeen = mimicIndependentActionSeen;
            markPersistentChanged();
        }
    }

    public void incrementEchoEvent(dev.yeldos.echoprotocol.echo.EchoType type) {
        switch (type) {
            case MEMORY -> memoryEvents++;
            case CORRUPTED -> corruptedEvents++;
            case MIMIC -> mimicEvents++;
            case ORIGINAL -> originalEvents++;
            case FALSE_MEMORY -> memoryEvents++;
        }
        markPersistentChanged();
    }

    public boolean activeEvent() {
        return activeEvent;
    }

    public void setActiveEvent(boolean activeEvent) {
        this.activeEvent = activeEvent;
    }

    public List<FamiliarLocation> familiarLocations() {
        return familiarLocations;
    }

    public PlayerEchoStateSnapshot snapshot(long currentTick) {
        return new PlayerEchoStateSnapshot(stage.id(), playTicks,
                StageManager.remainingDelay(currentTick, nextEventTick),
                StageManager.remainingDelay(currentTick, nextOriginalEventTick),
                stageOneEvents, totalEvents, memoryEvents, corruptedEvents, mimicEvents, originalEvents,
                mimicIndependentActionSeen,
                familiarLocations.stream().map(FamiliarLocationSnapshot::from).toList());
    }

    public void restore(PlayerEchoStateSnapshot snapshot, long currentTick) {
        this.stage = EchoStage.fromId(snapshot.stage());
        this.playTicks = snapshot.playTicks();
        this.nextEventTick = StageManager.restoreDeadline(currentTick, snapshot.nextEventDelay());
        this.nextOriginalEventTick = StageManager.restoreDeadline(currentTick, snapshot.nextOriginalEventDelay());
        this.stageOneEvents = snapshot.stageOneEvents();
        this.totalEvents = snapshot.totalEvents();
        this.memoryEvents = snapshot.memoryEvents();
        this.corruptedEvents = snapshot.corruptedEvents();
        this.mimicEvents = snapshot.mimicEvents();
        this.originalEvents = snapshot.originalEvents();
        this.mimicIndependentActionSeen = snapshot.mimicIndependentActionSeen();
        this.familiarLocations.clear();
        snapshot.familiarLocations().stream().map(FamiliarLocationSnapshot::toLocation)
                .forEach(this.familiarLocations::add);
    }
}
