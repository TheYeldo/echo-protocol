package dev.yeldos.echoprotocol.thread;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class MemoryThread {
    public static final int MAXIMUM_STEPS = 4;
    public static final int MAXIMUM_EVENT_REFERENCES = 4;

    private final UUID targetPlayerUuid;
    private final MemoryThreadType type;
    private final long selectedRoomNodeId;
    private final String selectedDimension;
    private final String relatedItemId;
    private final List<MemoryThreadStep> steps;
    private final long creationTick;
    private int currentStep;
    private long lastEventTick;
    private MemoryObservationResult lastObservationResult;
    private float contaminationContribution;
    private final Deque<Long> eventReferences = new ArrayDeque<>();
    private MemoryThreadStage stage;
    private int consecutiveFailures;
    private boolean resumedAfterRestart;
    private final boolean awardsProgress;

    public MemoryThread(UUID targetPlayerUuid, MemoryThreadType type, long selectedRoomNodeId,
                        String selectedDimension, String relatedItemId, List<MemoryThreadStep> steps,
                        long creationTick, int currentStep, long lastEventTick,
                        MemoryObservationResult lastObservationResult, float contaminationContribution,
                        List<Long> eventReferences, MemoryThreadStage stage, int consecutiveFailures,
                        boolean resumedAfterRestart, boolean awardsProgress) {
        if (targetPlayerUuid == null || type == null || steps == null || steps.size() < 2) {
            throw new IllegalArgumentException("invalid memory thread");
        }
        this.targetPlayerUuid = targetPlayerUuid;
        this.type = type;
        this.selectedRoomNodeId = Math.max(0L, selectedRoomNodeId);
        this.selectedDimension = selectedDimension == null ? "" : selectedDimension;
        this.relatedItemId = relatedItemId == null ? "" : relatedItemId;
        this.steps = List.copyOf(steps.subList(0, Math.min(MAXIMUM_STEPS, steps.size())));
        this.creationTick = Math.max(0L, creationTick);
        this.currentStep = Math.max(0, Math.min(currentStep, this.steps.size()));
        this.lastEventTick = Math.max(0L, lastEventTick);
        this.lastObservationResult = lastObservationResult == null ? MemoryObservationResult.NOT_PRESENT : lastObservationResult;
        this.contaminationContribution = clamp(contaminationContribution);
        if (eventReferences != null) {
            eventReferences.stream().skip(Math.max(0, eventReferences.size() - MAXIMUM_EVENT_REFERENCES))
                    .forEach(this.eventReferences::addLast);
        }
        this.stage = stage == null ? MemoryThreadStage.ACTIVE : stage;
        this.consecutiveFailures = Math.max(0, Math.min(3, consecutiveFailures));
        this.resumedAfterRestart = resumedAfterRestart;
        this.awardsProgress = awardsProgress;
        if (this.currentStep >= this.steps.size() && this.stage == MemoryThreadStage.ACTIVE) {
            this.stage = MemoryThreadStage.COMPLETED;
        }
    }

    public MemoryThread(UUID targetPlayerUuid, MemoryThreadType type, long selectedRoomNodeId,
                        String selectedDimension, String relatedItemId, List<MemoryThreadStep> steps,
                        long creationTick) {
        this(targetPlayerUuid, type, selectedRoomNodeId, selectedDimension, relatedItemId, steps, creationTick,
                0, 0L, MemoryObservationResult.NOT_PRESENT, 0.0F, List.of(), MemoryThreadStage.ACTIVE, 0, false, true);
    }

    public boolean eventStarted(long eventReference, long tick) {
        if (stage != MemoryThreadStage.ACTIVE || currentStep >= steps.size()) {
            return false;
        }
        lastEventTick = Math.max(lastEventTick, tick);
        lastObservationResult = MemoryObservationResult.NOT_PRESENT;
        eventReferences.addLast(eventReference);
        while (eventReferences.size() > MAXIMUM_EVENT_REFERENCES) {
            eventReferences.removeFirst();
        }
        return true;
    }

    public boolean applyOutcome(MemoryObservationResult result, float contribution) {
        if (stage != MemoryThreadStage.ACTIVE || currentStep >= steps.size()) {
            return false;
        }
        lastObservationResult = result == null ? MemoryObservationResult.EVENT_FAILED : result;
        if (lastObservationResult == MemoryObservationResult.EVENT_FAILED) {
            consecutiveFailures = Math.min(3, consecutiveFailures + 1);
            return false;
        }
        if (!steps.get(currentStep).accepts(lastObservationResult)) {
            consecutiveFailures = Math.min(3, consecutiveFailures + 1);
            return false;
        }
        currentStep++;
        contaminationContribution = clamp(contaminationContribution + Math.max(0.0F, contribution));
        consecutiveFailures = 0;
        if (currentStep >= steps.size()) {
            stage = MemoryThreadStage.COMPLETED;
        }
        return true;
    }

    public void pause() {
        if (stage == MemoryThreadStage.ACTIVE) {
            stage = MemoryThreadStage.PAUSED;
        }
    }

    public void resume(boolean afterRestart) {
        if (stage == MemoryThreadStage.PAUSED) {
            stage = MemoryThreadStage.ACTIVE;
            resumedAfterRestart |= afterRestart;
        }
    }

    public void cancel() {
        if (stage != MemoryThreadStage.COMPLETED) {
            stage = MemoryThreadStage.CANCELLED;
        }
    }

    public MemoryThreadStep nextStep() {
        return currentStep < steps.size() ? steps.get(currentStep) : null;
    }

    public UUID targetPlayerUuid() { return targetPlayerUuid; }
    public MemoryThreadType type() { return type; }
    public long selectedRoomNodeId() { return selectedRoomNodeId; }
    public String selectedDimension() { return selectedDimension; }
    public String relatedItemId() { return relatedItemId; }
    public List<MemoryThreadStep> steps() { return steps; }
    public long creationTick() { return creationTick; }
    public int currentStep() { return currentStep; }
    public int totalSteps() { return steps.size(); }
    public long lastEventTick() { return lastEventTick; }
    public MemoryObservationResult lastObservationResult() { return lastObservationResult; }
    public float contaminationContribution() { return contaminationContribution; }
    public List<Long> eventReferences() { return List.copyOf(eventReferences); }
    public MemoryThreadStage stage() { return stage; }
    public int consecutiveFailures() { return consecutiveFailures; }
    public boolean resumedAfterRestart() { return resumedAfterRestart; }
    public boolean awardsProgress() { return awardsProgress; }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }
}
