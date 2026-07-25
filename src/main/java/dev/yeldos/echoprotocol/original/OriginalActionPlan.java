package dev.yeldos.echoprotocol.original;

import java.util.List;

public record OriginalActionPlan(List<OriginalMovementSegment> segments) {
    public static final int MAXIMUM_SEGMENTS = 12;

    public OriginalActionPlan {
        segments = List.copyOf(segments.subList(0, Math.min(MAXIMUM_SEGMENTS, segments.size())));
    }
}
