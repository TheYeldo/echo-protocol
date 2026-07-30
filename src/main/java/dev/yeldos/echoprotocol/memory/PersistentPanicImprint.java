package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.panic.PanicImprint;
import dev.yeldos.echoprotocol.panic.PanicTriggerType;
import dev.yeldos.echoprotocol.recording.RecordedFrame;

import java.util.ArrayList;
import java.util.List;

public record PersistentPanicImprint(
        String dimension,
        PanicImprint.HealthCategory healthCategory,
        PanicTriggerType triggerType,
        long capturedTick,
        List<PersistentFrame> frames
) {
    public static final int MAXIMUM_FRAMES = 48;

    public PersistentPanicImprint {
        if (dimension == null || dimension.isBlank() || healthCategory == null || triggerType == null) {
            throw new IllegalArgumentException("invalid persistent panic imprint");
        }
        capturedTick = Math.max(0L, capturedTick);
        frames = frames == null ? List.of()
                : List.copyOf(frames.subList(0, Math.min(MAXIMUM_FRAMES, frames.size())));
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("panic imprint has no frames");
        }
    }

    public static PersistentPanicImprint from(PanicImprint imprint) {
        List<RecordedFrame> source = imprint.frames();
        List<PersistentFrame> compact = new ArrayList<>(Math.min(MAXIMUM_FRAMES, source.size()));
        long firstTick = source.getFirst().serverTick();
        if (source.size() <= MAXIMUM_FRAMES) {
            source.forEach(frame -> compact.add(PersistentFrame.from(frame, firstTick)));
        } else {
            for (int index = 0; index < MAXIMUM_FRAMES; index++) {
                int sourceIndex = Math.round(index * (source.size() - 1.0F) / (MAXIMUM_FRAMES - 1.0F));
                compact.add(PersistentFrame.from(source.get(sourceIndex), firstTick));
            }
        }
        return new PersistentPanicImprint(imprint.dimension(), imprint.healthCategory(), imprint.triggerType(),
                imprint.capturedTick(), compact);
    }

    public PanicImprint toRuntime() {
        long baseTick = Math.max(0L, capturedTick - frames.getLast().tickOffset());
        return new PanicImprint(frames.stream().map(frame -> frame.toRecordedFrame(baseTick)).toList(),
                dimension, healthCategory, triggerType, capturedTick);
    }
}
