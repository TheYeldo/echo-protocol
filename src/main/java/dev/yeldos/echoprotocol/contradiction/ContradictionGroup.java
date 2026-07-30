package dev.yeldos.echoprotocol.contradiction;

import java.util.ArrayList;
import java.util.List;

public final class ContradictionGroup {
    private final List<Runnable> cleanupActions = new ArrayList<>(2);
    private boolean finished;

    public void register(Runnable cleanup) {
        if (cleanup != null && cleanupActions.size() < 2) {
            cleanupActions.add(cleanup);
        }
    }

    public void finishAll() {
        if (finished) {
            return;
        }
        finished = true;
        List.copyOf(cleanupActions).forEach(Runnable::run);
    }

    public int size() { return cleanupActions.size(); }
    public boolean finished() { return finished; }
}
