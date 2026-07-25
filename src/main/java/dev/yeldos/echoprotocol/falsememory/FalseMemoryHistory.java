package dev.yeldos.echoprotocol.falsememory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FalseMemoryHistory {
    public record Entry(String signature, List<FalseMemoryDeviation> deviations, long tick) {
    }

    private final Map<UUID, Deque<Entry>> entries = new HashMap<>();

    public boolean recordAndCheckConflict(UUID playerUuid, FalseMemoryPlan plan, long tick, int maximum) {
        Deque<Entry> history = entries.computeIfAbsent(playerUuid, ignored -> new ArrayDeque<>());
        boolean conflict = history.stream().anyMatch(entry -> entry.signature().equals(plan.relatedSignature())
                && !entry.deviations().equals(plan.deviations()));
        history.addLast(new Entry(plan.relatedSignature(), plan.deviations(), tick));
        while (history.size() > Math.max(2, maximum)) {
            history.removeFirst();
        }
        return conflict;
    }

    public List<Entry> entries(UUID playerUuid) {
        Deque<Entry> history = entries.get(playerUuid);
        return history == null ? List.of() : List.copyOf(history);
    }

    public void clear(UUID playerUuid) {
        entries.remove(playerUuid);
    }

    public void clearAll() {
        entries.clear();
    }
}
