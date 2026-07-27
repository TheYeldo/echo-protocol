package dev.yeldos.echoprotocol.panic;

import dev.yeldos.echoprotocol.recording.RecordedFrame;

import java.util.List;

public record PanicImprint(
        List<RecordedFrame> frames,
        String dimension,
        HealthCategory healthCategory,
        PanicTriggerType triggerType,
        long capturedTick
) {
    public static final int MAXIMUM_FRAMES = 8 * 20;

    public enum HealthCategory {
        CRITICAL,
        LOW,
        STABLE
    }

    public PanicImprint {
        int first = Math.max(0, frames.size() - MAXIMUM_FRAMES);
        frames = List.copyOf(frames.subList(first, frames.size()));
    }
}
