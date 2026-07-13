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

    public EchoStage stage() {
        return stage;
    }

    public void setStage(EchoStage stage) {
        this.stage = stage;
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
    }

    public long nextOriginalEventTick() {
        return nextOriginalEventTick;
    }

    public void setNextOriginalEventTick(long nextOriginalEventTick) {
        this.nextOriginalEventTick = Math.max(0L, nextOriginalEventTick);
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
    }

    public void setStageOneEvents(int stageOneEvents) {
        this.stageOneEvents = Math.max(0, stageOneEvents);
    }

    public int totalEvents() {
        return totalEvents;
    }

    public void incrementTotalEvents() {
        totalEvents++;
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
    }

    public void setOriginalEvents(int originalEvents) {
        this.originalEvents = Math.max(0, originalEvents);
    }

    public boolean mimicIndependentActionSeen() {
        return mimicIndependentActionSeen;
    }

    public void setMimicIndependentActionSeen(boolean mimicIndependentActionSeen) {
        this.mimicIndependentActionSeen = mimicIndependentActionSeen;
    }

    public void incrementEchoEvent(dev.yeldos.echoprotocol.echo.EchoType type) {
        switch (type) {
            case MEMORY -> memoryEvents++;
            case CORRUPTED -> corruptedEvents++;
            case MIMIC -> mimicEvents++;
            case ORIGINAL -> originalEvents++;
        }
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
}
