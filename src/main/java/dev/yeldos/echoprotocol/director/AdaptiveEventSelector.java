package dev.yeldos.echoprotocol.director;

import dev.yeldos.echoprotocol.config.EchoConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class AdaptiveEventSelector {
    public record Candidate(EventCategory category, int weight) {
    }

    public EventCategory select(UUID playerUuid, long tick, List<Candidate> candidates,
                                EchoEventHistory history, EchoConfig config) {
        List<Candidate> adjusted = new ArrayList<>();
        int total = 0;
        EventCategory last = history.last(playerUuid);
        for (Candidate candidate : candidates) {
            int weight = Math.max(0, candidate.weight());
            if (config.adaptiveEventDirector()) {
                int recent = history.recentCount(playerUuid, candidate.category());
                weight = weight / (1 + recent * 2);
                if (config.preventRepeatedEvents() && candidate.category() == last) {
                    weight = 0;
                }
                if (candidate.category().intensity() == EventIntensity.STRONG
                        && history.strongSilenceActive(playerUuid, tick, config)) {
                    weight = 0;
                }
                if (!history.lastObserved(playerUuid) && candidate.category().intensity() != EventIntensity.SUBTLE) {
                    weight /= 2;
                }
            }
            if (weight > 0) {
                adjusted.add(new Candidate(candidate.category(), weight));
                total += weight;
            }
        }
        if (total <= 0) {
            return null;
        }
        long seed = playerUuid.getMostSignificantBits() ^ playerUuid.getLeastSignificantBits() ^ (tick * 0x9E3779B97F4A7C15L);
        int pick = new Random(seed).nextInt(total);
        for (Candidate candidate : adjusted) {
            pick -= candidate.weight();
            if (pick < 0) {
                return candidate.category();
            }
        }
        return adjusted.get(adjusted.size() - 1).category();
    }
}
