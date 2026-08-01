package dev.yeldos.echoprotocol.habit;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.memory.PersistentHabit;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public final class PlayerHabitTracker {
    private final Map<UUID, PlayerHabitSummary> summaries = new HashMap<>();
    private final Set<UUID> hydrated = new HashSet<>();
    private final StageManager stageManager;

    public PlayerHabitTracker(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    public void tick(MinecraftServer server, EchoConfig config, long tick) {
        if (!config.borrowedHabitsEnabled() || tick % 600L != 0L) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            record(player, HabitType.IDLE, player.getBlockPos(), player.getMainHandStack(), config, tick);
            if (!player.getMainHandStack().isEmpty()) {
                record(player, HabitType.FREQUENT_ITEM, player.getBlockPos(), player.getMainHandStack(), config, tick);
            }
        }
    }

    public void record(ServerPlayerEntity player, HabitType type, BlockPos position, ItemStack item,
                       EchoConfig config, long tick) {
        if (!config.borrowedHabitsEnabled()) {
            return;
        }
        PlayerHabitSummary summary = summary(player.getUuid());
        summary.observe(type,
                player.getWorld().getRegistryKey().getValue().toString(), position, item, tick,
                config.maximumTrackedHabits());
        PlayerHabitSummary.Habit updated = summary.entries().stream()
                .filter(candidate -> candidate.type() == type
                        && candidate.dimension().equals(player.getWorld().getRegistryKey().getValue().toString())
                        && candidate.position().getSquaredDistance(position) <= 25.0D)
                .findFirst().orElse(null);
        if (updated != null) {
            String itemId = updated.visualItem().isEmpty() ? ""
                    : Registries.ITEM.getId(updated.visualItem().getItem()).toString();
            stageManager.memory(player.getUuid()).addHabit(new PersistentHabit(updated.type(), updated.dimension(),
                    updated.position(), itemId, 1, updated.lastSeenTick()), config.persistentHabitMaximum());
        }
    }

    public PlayerHabitSummary summary(UUID playerUuid) {
        PlayerHabitSummary summary = summaries.computeIfAbsent(playerUuid, ignored -> new PlayerHabitSummary());
        if (hydrated.add(playerUuid)) {
            stageManager.memory(playerUuid).habits().forEach(summary::restore);
        }
        return summary;
    }

    public List<PlayerHabitSummary.Habit> habits(UUID playerUuid) {
        return summary(playerUuid).entries();
    }

    public int clear(UUID playerUuid) {
        PlayerHabitSummary removed = summaries.remove(playerUuid);
        hydrated.add(playerUuid);
        int persistent = stageManager.memory(playerUuid).clearHabits();
        return Math.max(persistent, removed == null ? 0 : removed.clear());
    }

    public void clearAll() {
        summaries.clear();
        hydrated.clear();
    }

    public void disconnect(UUID playerUuid) {
        summaries.remove(playerUuid);
        hydrated.remove(playerUuid);
    }
}
