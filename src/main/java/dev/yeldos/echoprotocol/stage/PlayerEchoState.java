package dev.yeldos.echoprotocol.stage;

public final class PlayerEchoState {
    private EchoStage stage = EchoStage.OBSERVATION;
    private long playTicks;
    private long nextEventTick;
    private long lastJoinTick;
    private long lastRespawnTick;
    private int stageOneEvents;
    private int totalEvents;
    private boolean activeEvent;

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

    public boolean activeEvent() {
        return activeEvent;
    }

    public void setActiveEvent(boolean activeEvent) {
        this.activeEvent = activeEvent;
    }
}
