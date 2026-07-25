package dev.yeldos.echoprotocol.director;

import dev.yeldos.echoprotocol.config.EchoConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EchoEventHistory {
    public record Entry(EventCategory category, long tick, boolean observed) {
    }

    private final Map<UUID, Deque<Entry>> entries = new HashMap<>();
    private final Map<UUID, Long> lastStrongTicks = new HashMap<>();

    public void record(UUID playerUuid, EventCategory category, long tick, boolean observed, EchoConfig config) {
        Deque<Entry> history = entries.computeIfAbsent(playerUuid, ignored -> new ArrayDeque<>());
        history.addLast(new Entry(category, tick, observed));
        while (history.size() > config.recentEventHistorySize()) {
            history.removeFirst();
        }
        if (category.intensity() == EventIntensity.STRONG) {
            lastStrongTicks.put(playerUuid, tick);
        }
    }

    public EventCategory last(UUID playerUuid) {
        Deque<Entry> history = entries.get(playerUuid);
        return history == null || history.isEmpty() ? null : history.peekLast().category();
    }

    public boolean lastObserved(UUID playerUuid) {
        Deque<Entry> history = entries.get(playerUuid);
        return history == null || history.isEmpty() || history.peekLast().observed();
    }

    public void markLastObserved(UUID playerUuid) {
        Deque<Entry> history = entries.get(playerUuid);
        if (history == null || history.isEmpty()) {
            return;
        }
        Entry last = history.removeLast();
        history.addLast(new Entry(last.category(), last.tick(), true));
    }

    public int recentCount(UUID playerUuid, EventCategory category) {
        Deque<Entry> history = entries.get(playerUuid);
        if (history == null) {
            return 0;
        }
        int count = 0;
        for (Entry entry : history) {
            if (entry.category() == category) {
                count++;
            }
        }
        return count;
    }

    public boolean strongSilenceActive(UUID playerUuid, long tick, EchoConfig config) {
        long last = lastStrongTicks.getOrDefault(playerUuid, Long.MIN_VALUE / 2);
        return tick - last < (long) config.strongEventSilenceMinutes() * 60L * 20L;
    }

    public List<Entry> entries(UUID playerUuid) {
        Deque<Entry> history = entries.get(playerUuid);
        return history == null ? List.of() : List.copyOf(history);
    }

    public int clear(UUID playerUuid) {
        Deque<Entry> removed = entries.remove(playerUuid);
        lastStrongTicks.remove(playerUuid);
        return removed == null ? 0 : removed.size();
    }

    public void clearAll() {
        entries.clear();
        lastStrongTicks.clear();
    }
}
