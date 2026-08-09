package dev.yeldos.echoprotocol.recording;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Selects an authentic replay window while preferring movement the player actually performed. Long recording
 * buffers contain inventory, crafting, and AFK periods; choosing those uniformly made most natural manifestations
 * motionless. Discontinuous windows are never replayed because they represent teleports or stale route boundaries.
 */
public final class ReplaySegmentSelector {
    private static final double MAXIMUM_CONTINUOUS_STEP = 2.5D;

    private ReplaySegmentSelector() {
    }

    public static List<RecordedFrame> select(List<RecordedFrame> source, int minimumFrames, int maximumFrames,
                                             RandomGenerator random) {
        int minimum = Math.max(2, minimumFrames);
        int maximum = Math.min(source.size(), Math.max(minimum, maximumFrames));
        if (source.size() < minimum) {
            return List.of();
        }

        double[] distancePrefix = new double[source.size()];
        int[] movingPrefix = new int[source.size() + 1];
        int[] discontinuityPrefix = new int[source.size()];
        for (int index = 0; index < source.size(); index++) {
            movingPrefix[index + 1] = movingPrefix[index] + (source.get(index).walking() ? 1 : 0);
            if (index == 0) {
                continue;
            }
            double step = source.get(index - 1).pos().distanceTo(source.get(index).pos());
            distancePrefix[index] = distancePrefix[index - 1] + Math.min(step, MAXIMUM_CONTINUOUS_STEP);
            discontinuityPrefix[index] = discontinuityPrefix[index - 1]
                    + (step > MAXIMUM_CONTINUOUS_STEP ? 1 : 0);
        }

        Window meaningful = reservoirWindow(source.size(), minimum, maximum, random,
                (start, end) -> continuous(discontinuityPrefix, start, end)
                        && meaningful(distancePrefix, movingPrefix, start, end));
        if (meaningful != null) {
            return copy(source, meaningful);
        }

        // A truly idle recording is still an authentic memory. Fall back only to a continuous window so an
        // intentional standstill can manifest without replaying a teleport or dimension boundary.
        Window continuous = reservoirWindow(source.size(), minimum, maximum, random,
                (start, end) -> continuous(discontinuityPrefix, start, end));
        return continuous == null ? List.of() : copy(source, continuous);
    }

    private static Window reservoirWindow(int size, int minimum, int maximum, RandomGenerator random,
                                          WindowPredicate predicate) {
        Window selected = null;
        int matches = 0;
        for (int length = minimum; length <= maximum; length++) {
            for (int start = 0; start + length <= size; start++) {
                int end = start + length - 1;
                if (predicate.test(start, end) && random.nextInt(++matches) == 0) {
                    selected = new Window(start, length);
                }
            }
        }
        return selected;
    }

    private static boolean meaningful(double[] distancePrefix, int[] movingPrefix, int start, int end) {
        int length = end - start + 1;
        double distance = distancePrefix[end] - distancePrefix[start];
        int movingFrames = movingPrefix[end + 1] - movingPrefix[start];
        double minimumDistance = Math.max(1.5D, (length - 1) * 0.015D);
        int minimumMovingFrames = Math.max(3, length / 10);
        return distance >= minimumDistance && (movingFrames >= minimumMovingFrames
                || distance >= minimumDistance * 1.5D);
    }

    private static boolean continuous(int[] discontinuityPrefix, int start, int end) {
        return discontinuityPrefix[end] - discontinuityPrefix[start] == 0;
    }

    private static List<RecordedFrame> copy(List<RecordedFrame> source, Window window) {
        return new ArrayList<>(source.subList(window.start(), window.start() + window.length()));
    }

    @FunctionalInterface
    private interface WindowPredicate {
        boolean test(int start, int end);
    }

    private record Window(int start, int length) {
    }
}
