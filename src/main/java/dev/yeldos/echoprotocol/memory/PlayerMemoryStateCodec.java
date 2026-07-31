package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.audio.AudioResidue;
import dev.yeldos.echoprotocol.audio.AudioResidueEvent;
import dev.yeldos.echoprotocol.contamination.MemoryContaminationState;
import dev.yeldos.echoprotocol.contradiction.ContradictionVariant;
import dev.yeldos.echoprotocol.habit.HabitType;
import dev.yeldos.echoprotocol.panic.PanicImprint;
import dev.yeldos.echoprotocol.panic.PanicTriggerType;
import dev.yeldos.echoprotocol.profile.ObservationMetric;
import dev.yeldos.echoprotocol.profile.ObservationProfile;
import dev.yeldos.echoprotocol.room.RoomMemoryEdge;
import dev.yeldos.echoprotocol.room.RoomMemoryNode;
import dev.yeldos.echoprotocol.room.RoomMemoryType;
import dev.yeldos.echoprotocol.room.RoomObservationSource;
import dev.yeldos.echoprotocol.thread.MemoryObservationResult;
import dev.yeldos.echoprotocol.thread.MemoryThread;
import dev.yeldos.echoprotocol.thread.MemoryThreadEventType;
import dev.yeldos.echoprotocol.thread.MemoryThreadStage;
import dev.yeldos.echoprotocol.thread.MemoryThreadStep;
import dev.yeldos.echoprotocol.thread.MemoryThreadType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.yeldos.echoprotocol.memory.NbtCompat.*;

public final class PlayerMemoryStateCodec {
    private PlayerMemoryStateCodec() {
    }

    public static NbtCompound write(PlayerMemoryState state) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("DataVersion", MemoryDataVersion.CURRENT);
        nbt.put("PanicImprints", writePanic(state.panicImprints()));
        nbt.put("AudioResidues", writeAudio(state.audioResidues()));
        nbt.put("Habits", writeHabits(state.habits()));
        nbt.put("RoomNodes", writeRoomNodes(state.roomGraph().nodes()));
        nbt.put("RoomEdges", writeRoomEdges(state.roomGraph().edges()));
        if (state.activeThread() != null) {
            nbt.put("ActiveThread", writeThread(state.activeThread()));
        }
        NbtList completed = new NbtList();
        for (MemoryThreadType type : state.recentCompletedThreads()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Type", type.name());
            completed.add(entry);
        }
        nbt.put("CompletedThreads", completed);
        nbt.put("Contamination", writeContamination(state.contamination()));
        nbt.put("ObservationProfile", writeProfile(state.observationProfile()));
        nbt.putLong("LastStrongEventTick", state.lastStrongEventTick());
        nbt.put("SignificantEvents", writeSignificantEvents(state.significantEvents()));
        nbt.put("FalseMemorySeeds", writeSeeds(state.falseMemorySeeds()));
        return nbt;
    }

    public static PlayerMemoryState read(NbtCompound nbt) {
        int version = nbt.getInt("DataVersion").orElse(1);
        if (version > MemoryDataVersion.CURRENT) {
            throw new UnsupportedMemoryVersionException(version);
        }
        PlayerMemoryState state = new PlayerMemoryState();
        state.restoreSchemaVersion(MemoryDataVersion.CURRENT);
        readEntries(nbt, "PanicImprints", state, PlayerMemoryStateCodec::readPanic, state::restorePanic);
        readEntries(nbt, "AudioResidues", state, PlayerMemoryStateCodec::readAudio, state::restoreAudio);
        readEntries(nbt, "Habits", state, PlayerMemoryStateCodec::readHabit, state::restoreHabit);
        readEntries(nbt, "RoomNodes", state, PlayerMemoryStateCodec::readRoomNode, state.roomGraph()::restore);
        readEntries(nbt, "RoomEdges", state, PlayerMemoryStateCodec::readRoomEdge, state.roomGraph()::restore);
        if (containsType(nbt, "ActiveThread", NbtElement.COMPOUND_TYPE)) {
            try {
                state.restoreThread(readThread(getCompound(nbt, "ActiveThread")));
            } catch (RuntimeException exception) {
                state.addLoadWarning("active thread skipped: " + simpleMessage(exception));
            }
        }
        if (containsType(nbt, "CompletedThreads", NbtElement.LIST_TYPE)) {
            NbtList list = getList(nbt, "CompletedThreads");
            for (int index = 0; index < Math.min(8, list.size()); index++) {
                try {
                    state.restoreCompletedThread(MemoryThreadType.valueOf(getString(getCompound(list, index), "Type")));
                } catch (RuntimeException exception) {
                    state.addLoadWarning("completed thread entry skipped");
                }
            }
        }
        if (containsType(nbt, "Contamination", NbtElement.COMPOUND_TYPE)) {
            try {
                state.restoreContamination(readContamination(getCompound(nbt, "Contamination")));
            } catch (RuntimeException exception) {
                state.addLoadWarning("contamination state skipped");
            }
        }
        if (containsType(nbt, "ObservationProfile", NbtElement.COMPOUND_TYPE)) {
            try {
                state.restoreObservationProfile(readProfile(getCompound(nbt, "ObservationProfile")));
            } catch (RuntimeException exception) {
                state.addLoadWarning("observation profile skipped");
            }
        }
        state.restoreLastStrongEventTick(getLong(nbt, "LastStrongEventTick"));
        readEntries(nbt, "SignificantEvents", state, PlayerMemoryStateCodec::readSignificantEvent,
                state::restoreSignificantEvent);
        readEntries(nbt, "FalseMemorySeeds", state, PlayerMemoryStateCodec::readSeed,
                state::restoreFalseMemorySeed);
        state.markLoadedFromPreviousSession();
        return state;
    }

    private static NbtList writePanic(List<PersistentPanicImprint> imprints) {
        NbtList list = new NbtList();
        for (PersistentPanicImprint imprint : imprints.stream().limit(3).toList()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Dimension", imprint.dimension());
            entry.putString("Health", imprint.healthCategory().name());
            entry.putString("Trigger", imprint.triggerType().name());
            entry.putLong("CapturedTick", imprint.capturedTick());
            NbtList frames = new NbtList();
            for (PersistentFrame frame : imprint.frames()) {
                NbtCompound frameNbt = new NbtCompound();
                frameNbt.putLong("TickOffset", frame.tickOffset());
                putPosition(frameNbt, frame.x(), frame.y(), frame.z());
                frameNbt.putFloat("BodyYaw", frame.bodyYaw());
                frameNbt.putFloat("HeadYaw", frame.headYaw());
                frameNbt.putFloat("Pitch", frame.pitch());
                frameNbt.putBoolean("Walking", frame.walking());
                frameNbt.putBoolean("Sprinting", frame.sprinting());
                frameNbt.putBoolean("Sneaking", frame.sneaking());
                frameNbt.putBoolean("Swimming", frame.swimming());
                frameNbt.putBoolean("Crawling", frame.crawling());
                frameNbt.putBoolean("Jumping", frame.jumping());
                frameNbt.putBoolean("OnGround", frame.onGround());
                frames.add(frameNbt);
            }
            entry.put("Frames", frames);
            list.add(entry);
        }
        return list;
    }

    private static PersistentPanicImprint readPanic(NbtCompound entry) {
        NbtList framesNbt = getList(entry, "Frames");
        List<PersistentFrame> frames = new ArrayList<>();
        for (int index = 0; index < Math.min(PersistentPanicImprint.MAXIMUM_FRAMES, framesNbt.size()); index++) {
            NbtCompound frame = getCompound(framesNbt, index);
            frames.add(new PersistentFrame(getLong(frame, "TickOffset"), getDouble(frame, "X"), getDouble(frame, "Y"),
                    getDouble(frame, "Z"), getFloat(frame, "BodyYaw"), getFloat(frame, "HeadYaw"),
                    getFloat(frame, "Pitch"), getBoolean(frame, "Walking"), getBoolean(frame, "Sprinting"),
                    getBoolean(frame, "Sneaking"), getBoolean(frame, "Swimming"), getBoolean(frame, "Crawling"),
                    getBoolean(frame, "Jumping"), getBoolean(frame, "OnGround")));
        }
        return new PersistentPanicImprint(required(entry, "Dimension"),
                PanicImprint.HealthCategory.valueOf(required(entry, "Health")),
                PanicTriggerType.valueOf(required(entry, "Trigger")), getLong(entry, "CapturedTick"), frames);
    }

    private static NbtList writeAudio(List<AudioResidue> residues) {
        NbtList list = new NbtList();
        for (AudioResidue residue : residues.stream().limit(16).toList()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Sound", residue.soundId().toString());
            entry.putString("Event", residue.event().name());
            entry.putString("Dimension", residue.dimension());
            putBlockPos(entry, residue.position());
            entry.putFloat("Volume", residue.volume());
            entry.putFloat("Pitch", residue.pitch());
            entry.putLong("CapturedTick", residue.capturedTick());
            list.add(entry);
        }
        return list;
    }

    private static AudioResidue readAudio(NbtCompound entry) {
        Identifier sound = Identifier.tryParse(required(entry, "Sound"));
        if (sound == null) {
            throw new IllegalArgumentException("invalid sound identifier");
        }
        return new AudioResidue(sound, AudioResidueEvent.valueOf(required(entry, "Event")),
                required(entry, "Dimension"), getBlockPos(entry), getFloat(entry, "Volume"),
                getFloat(entry, "Pitch"), getLong(entry, "CapturedTick"));
    }

    private static NbtList writeHabits(List<PersistentHabit> habits) {
        NbtList list = new NbtList();
        for (PersistentHabit habit : habits.stream().limit(16).toList()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Type", habit.type().name());
            entry.putString("Dimension", habit.dimension());
            putBlockPos(entry, habit.position());
            entry.putString("VisualItem", habit.visualItemId());
            entry.putInt("Observations", habit.observations());
            entry.putLong("LastSeenTick", habit.lastSeenTick());
            list.add(entry);
        }
        return list;
    }

    private static PersistentHabit readHabit(NbtCompound entry) {
        return new PersistentHabit(HabitType.valueOf(required(entry, "Type")), required(entry, "Dimension"),
                getBlockPos(entry), getString(entry, "VisualItem"), getInt(entry, "Observations"),
                getLong(entry, "LastSeenTick"));
    }

    private static NbtList writeRoomNodes(List<RoomMemoryNode> nodes) {
        NbtList list = new NbtList();
        for (RoomMemoryNode node : nodes.stream().limit(RoomMemoryGraphBounds.NODES).toList()) {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Id", node.id());
            entry.putString("Dimension", node.dimension());
            putBlockPos(entry, node.center());
            entry.putString("Type", node.type().name());
            entry.putFloat("Confidence", node.confidence());
            entry.putInt("Visits", node.visits());
            entry.putLong("LastVisitedTick", node.lastVisitedTick());
            entry.putString("FamiliarLocationId", node.familiarLocationId());
            entry.putString("Source", node.source().name());
            entry.putBoolean("Valid", node.valid());
            NbtList associated = new NbtList();
            for (BlockPos position : node.associatedBlocks()) {
                NbtCompound block = new NbtCompound();
                putBlockPos(block, position);
                associated.add(block);
            }
            entry.put("AssociatedBlocks", associated);
            list.add(entry);
        }
        return list;
    }

    private static RoomMemoryNode readRoomNode(NbtCompound entry) {
        NbtList associatedNbt = getList(entry, "AssociatedBlocks");
        List<BlockPos> associated = new ArrayList<>();
        for (int index = 0; index < Math.min(RoomMemoryNode.MAXIMUM_ASSOCIATED_BLOCKS, associatedNbt.size()); index++) {
            associated.add(getBlockPos(getCompound(associatedNbt, index)));
        }
        return new RoomMemoryNode(getLong(entry, "Id"), required(entry, "Dimension"), getBlockPos(entry),
                RoomMemoryType.valueOf(required(entry, "Type")), getFloat(entry, "Confidence"),
                getInt(entry, "Visits"), getLong(entry, "LastVisitedTick"), getString(entry, "FamiliarLocationId"),
                associated, RoomObservationSource.valueOf(required(entry, "Source")), getBoolean(entry, "Valid"));
    }

    private static NbtList writeRoomEdges(List<RoomMemoryEdge> edges) {
        NbtList list = new NbtList();
        for (RoomMemoryEdge edge : edges.stream().limit(RoomMemoryGraphBounds.EDGES).toList()) {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Source", edge.sourceNodeId());
            entry.putLong("Destination", edge.destinationNodeId());
            if (edge.transitionPosition() != null) {
                NbtCompound transition = new NbtCompound();
                putBlockPos(transition, edge.transitionPosition());
                entry.put("Transition", transition);
            }
            entry.putInt("Count", edge.transitionCount());
            entry.putLong("LastUsedTick", edge.lastUsedTick());
            entry.putFloat("Confidence", edge.confidence());
            entry.putDouble("Distance", edge.approximateTravelDistance());
            list.add(entry);
        }
        return list;
    }

    private static RoomMemoryEdge readRoomEdge(NbtCompound entry) {
        BlockPos transition = containsType(entry, "Transition", NbtElement.COMPOUND_TYPE)
                ? getBlockPos(getCompound(entry, "Transition")) : null;
        return new RoomMemoryEdge(getLong(entry, "Source"), getLong(entry, "Destination"), transition,
                getInt(entry, "Count"), getLong(entry, "LastUsedTick"), getFloat(entry, "Confidence"),
                getDouble(entry, "Distance"));
    }

    private static NbtCompound writeThread(MemoryThread thread) {
        NbtCompound nbt = new NbtCompound();
        putUuid(nbt, "Player", thread.targetPlayerUuid());
        nbt.putString("Type", thread.type().name());
        nbt.putLong("Room", thread.selectedRoomNodeId());
        nbt.putString("Dimension", thread.selectedDimension());
        nbt.putString("Item", thread.relatedItemId());
        nbt.putLong("Created", thread.creationTick());
        nbt.putInt("CurrentStep", thread.currentStep());
        nbt.putLong("LastEvent", thread.lastEventTick());
        nbt.putString("Observation", thread.lastObservationResult().name());
        nbt.putFloat("Contribution", thread.contaminationContribution());
        nbt.putString("Stage", thread.stage().name());
        nbt.putInt("Failures", thread.consecutiveFailures());
        nbt.putBoolean("Resumed", thread.resumedAfterRestart());
        nbt.putBoolean("AwardsProgress", thread.awardsProgress());
        NbtList steps = new NbtList();
        for (MemoryThreadStep step : thread.steps()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Event", step.eventType().name());
            entry.putString("Variant", step.contradictionVariant() == null ? "" : step.contradictionVariant().name());
            entry.putInt("Outcomes", outcomeMask(step.acceptedOutcomes()));
            entry.putBoolean("Strong", step.strong());
            steps.add(entry);
        }
        nbt.put("Steps", steps);
        nbt.putLongArray("References", thread.eventReferences().stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }

    private static MemoryThread readThread(NbtCompound nbt) {
        NbtList stepsNbt = getList(nbt, "Steps");
        List<MemoryThreadStep> steps = new ArrayList<>();
        for (int index = 0; index < Math.min(MemoryThread.MAXIMUM_STEPS, stepsNbt.size()); index++) {
            NbtCompound entry = getCompound(stepsNbt, index);
            String variantName = getString(entry, "Variant");
            ContradictionVariant variant = variantName.isBlank() ? null : ContradictionVariant.valueOf(variantName);
            steps.add(new MemoryThreadStep(MemoryThreadEventType.valueOf(required(entry, "Event")), variant,
                    outcomes(getInt(entry, "Outcomes")), getBoolean(entry, "Strong")));
        }
        UUID uuid = getUuid(nbt, "Player");
        MemoryThreadStage restoredStage = MemoryThreadStage.valueOf(required(nbt, "Stage"));
        if (restoredStage == MemoryThreadStage.COMPLETED || restoredStage == MemoryThreadStage.CANCELLED) {
            throw new IllegalArgumentException("inactive thread cannot be restored");
        }
        if (restoredStage == MemoryThreadStage.ACTIVE) {
            restoredStage = MemoryThreadStage.PAUSED;
        }
        return new MemoryThread(uuid, MemoryThreadType.valueOf(required(nbt, "Type")), getLong(nbt, "Room"),
                getString(nbt, "Dimension"), getString(nbt, "Item"), steps, getLong(nbt, "Created"),
                getInt(nbt, "CurrentStep"), getLong(nbt, "LastEvent"),
                MemoryObservationResult.valueOf(required(nbt, "Observation")), getFloat(nbt, "Contribution"),
                java.util.Arrays.stream(getLongArray(nbt, "References")).boxed().toList(),
                restoredStage, getInt(nbt, "Failures"), true,
                !nbt.contains("AwardsProgress") || getBoolean(nbt, "AwardsProgress"));
    }

    private static NbtCompound writeContamination(MemoryContaminationState state) {
        NbtCompound nbt = new NbtCompound();
        nbt.putFloat("Value", state.value());
        nbt.putLong("LastRecovery", state.lastRecoveryTick());
        nbt.putLong("LastContribution", state.lastContributionId());
        return nbt;
    }

    private static MemoryContaminationState readContamination(NbtCompound nbt) {
        return new MemoryContaminationState(getFloat(nbt, "Value"), getLong(nbt, "LastRecovery"),
                getLong(nbt, "LastContribution"));
    }

    private static NbtCompound writeProfile(ObservationProfile profile) {
        NbtCompound nbt = new NbtCompound();
        float[] counters = profile.counters();
        for (ObservationMetric metric : ObservationMetric.values()) {
            nbt.putFloat(metric.name(), counters[metric.ordinal()]);
        }
        nbt.putInt("Samples", profile.samples());
        nbt.putFloat("PreferredDistance", profile.preferredDistance());
        nbt.putInt("DistanceSamples", profile.distanceSamples());
        nbt.putLong("LastDecay", profile.lastDecayTick());
        return nbt;
    }

    private static ObservationProfile readProfile(NbtCompound nbt) {
        float[] counters = new float[ObservationMetric.values().length];
        for (ObservationMetric metric : ObservationMetric.values()) {
            counters[metric.ordinal()] = getFloat(nbt, metric.name());
        }
        return new ObservationProfile(counters, getInt(nbt, "Samples"), getFloat(nbt, "PreferredDistance"),
                getInt(nbt, "DistanceSamples"), getLong(nbt, "LastDecay"));
    }

    private static NbtList writeSignificantEvents(List<SignificantEventRecord> events) {
        NbtList list = new NbtList();
        for (SignificantEventRecord event : events.stream().limit(24).toList()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Event", event.eventType());
            entry.putString("Dimension", event.dimension());
            entry.putLong("Room", event.roomNodeId());
            entry.putLong("Tick", event.tick());
            entry.putString("Observation", event.observation().name());
            entry.putBoolean("Strong", event.strong());
            list.add(entry);
        }
        return list;
    }

    private static SignificantEventRecord readSignificantEvent(NbtCompound entry) {
        return new SignificantEventRecord(required(entry, "Event"), getString(entry, "Dimension"),
                getLong(entry, "Room"), getLong(entry, "Tick"),
                MemoryObservationResult.valueOf(required(entry, "Observation")), getBoolean(entry, "Strong"));
    }

    private static NbtList writeSeeds(List<FalseMemorySeed> seeds) {
        NbtList list = new NbtList();
        for (FalseMemorySeed seed : seeds.stream().limit(4).toList()) {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Seed", seed.seed());
            entry.putString("Signature", seed.routeSignature());
            entry.putString("Variant", seed.preferredVariant() == null ? "" : seed.preferredVariant().name());
            entry.putLong("Room", seed.roomNodeId());
            entry.putLong("Created", seed.createdTick());
            list.add(entry);
        }
        return list;
    }

    private static FalseMemorySeed readSeed(NbtCompound entry) {
        String variantName = getString(entry, "Variant");
        return new FalseMemorySeed(getLong(entry, "Seed"), getString(entry, "Signature"),
                variantName.isBlank() ? null : ContradictionVariant.valueOf(variantName),
                getLong(entry, "Room"), getLong(entry, "Created"));
    }

    private static int outcomeMask(Set<MemoryObservationResult> outcomes) {
        int mask = 0;
        for (MemoryObservationResult outcome : outcomes) {
            mask |= 1 << outcome.ordinal();
        }
        return mask;
    }

    private static Set<MemoryObservationResult> outcomes(int mask) {
        EnumSet<MemoryObservationResult> outcomes = EnumSet.noneOf(MemoryObservationResult.class);
        for (MemoryObservationResult result : MemoryObservationResult.values()) {
            if ((mask & (1 << result.ordinal())) != 0) {
                outcomes.add(result);
            }
        }
        return outcomes.isEmpty() ? Set.of(MemoryObservationResult.COMPLETED) : outcomes;
    }

    private static void putBlockPos(NbtCompound nbt, BlockPos position) {
        nbt.putInt("X", position.getX());
        nbt.putInt("Y", position.getY());
        nbt.putInt("Z", position.getZ());
    }

    private static BlockPos getBlockPos(NbtCompound nbt) {
        return new BlockPos(getInt(nbt, "X"), getInt(nbt, "Y"), getInt(nbt, "Z"));
    }

    private static void putPosition(NbtCompound nbt, double x, double y, double z) {
        nbt.putDouble("X", x);
        nbt.putDouble("Y", y);
        nbt.putDouble("Z", z);
    }

    private static String required(NbtCompound nbt, String key) {
        String value = getString(nbt, key);
        if (value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }

    private static String simpleMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static <T> void readEntries(NbtCompound root, String key, PlayerMemoryState state,
                                        EntryReader<T> reader, java.util.function.Consumer<T> consumer) {
        if (!containsType(root, key, NbtElement.LIST_TYPE)) {
            return;
        }
        NbtList list = getList(root, key);
        int maximum = switch (key) {
            case "PanicImprints" -> 3;
            case "AudioResidues", "Habits" -> 16;
            case "RoomNodes" -> RoomMemoryGraphBounds.NODES;
            case "RoomEdges" -> RoomMemoryGraphBounds.EDGES;
            case "SignificantEvents" -> 24;
            case "FalseMemorySeeds" -> 4;
            default -> list.size();
        };
        for (int index = 0; index < Math.min(maximum, list.size()); index++) {
            try {
                consumer.accept(reader.read(getCompound(list, index)));
            } catch (RuntimeException exception) {
                state.addLoadWarning(key + "[" + index + "] skipped: " + simpleMessage(exception));
            }
        }
    }

    @FunctionalInterface
    private interface EntryReader<T> {
        T read(NbtCompound nbt);
    }

    private static final class RoomMemoryGraphBounds {
        private static final int NODES = 20;
        private static final int EDGES = 48;
    }

    public static final class UnsupportedMemoryVersionException extends IllegalArgumentException {
        private final int version;

        public UnsupportedMemoryVersionException(int version) {
            super("unsupported future memory data version " + version);
            this.version = version;
        }

        public int version() { return version; }
    }
}
