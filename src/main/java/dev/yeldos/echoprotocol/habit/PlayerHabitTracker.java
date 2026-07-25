package dev.yeldos.echoprotocol.habit;

import dev.yeldos.echoprotocol.config.EchoConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerHabitTracker {
    private final Map<UUID, PlayerHabitSummary> summaries = new HashMap<>();

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
        summary(player.getUuid()).observe(type,
                player.getServerWorld().getRegistryKey().getValue().toString(), position, item, tick,
                config.maximumTrackedHabits());
    }

    public PlayerHabitSummary summary(UUID playerUuid) {
        return summaries.computeIfAbsent(playerUuid, ignored -> new PlayerHabitSummary());
    }

    public List<PlayerHabitSummary.Habit> habits(UUID playerUuid) {
        return summary(playerUuid).entries();
    }

    public int clear(UUID playerUuid) {
        PlayerHabitSummary removed = summaries.remove(playerUuid);
        return removed == null ? 0 : removed.clear();
    }

    public void clearAll() {
        summaries.clear();
    }
}
