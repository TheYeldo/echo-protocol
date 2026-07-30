package dev.yeldos.echoprotocol.panic;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.recording.PlayerRecording;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.memory.PersistentPanicImprint;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.UseAction;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public final class PanicImprintManager {
    private final RecordingManager recordings;
    private final StageManager stageManager;
    private final Map<UUID, Deque<PanicImprint>> imprints = new HashMap<>();
    private final Map<UUID, Long> lastCaptureTicks = new HashMap<>();
    private final Map<UUID, Boolean> wasOnFire = new HashMap<>();
    private final Map<UUID, Boolean> wasDrowning = new HashMap<>();
    private final Map<UUID, Float> lastHealth = new HashMap<>();
    private final Map<UUID, Float> maximumFall = new HashMap<>();
    private final Map<UUID, Long> recentDamageTicks = new HashMap<>();
    private final Set<UUID> hydrated = new HashSet<>();

    public PanicImprintManager(RecordingManager recordings, StageManager stageManager) {
        this.recordings = recordings;
        this.stageManager = stageManager;
    }

    public void tick(MinecraftServer server, EchoConfig config, long tick) {
        if (!config.panicImprintsEnabled()) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            float health = player.getHealth();
            float previousHealth = lastHealth.getOrDefault(uuid, health);
            if (health > 0.0F && health <= config.panicImprintHealthThreshold()
                    && player.isUsingItem() && player.getActiveItem().getUseAction() == UseAction.EAT) {
                capture(player, PanicTriggerType.URGENT_EATING, config, tick, false);
            } else if (health > 0.0F && health <= config.panicImprintHealthThreshold()
                    && previousHealth > config.panicImprintHealthThreshold()) {
                capture(player, PanicTriggerType.LOW_HEALTH, config, tick, false);
            } else if (health > 0.0F && previousHealth - health >= 6.0F) {
                capture(player, PanicTriggerType.RAPID_HEALTH_LOSS, config, tick, false);
            }
            boolean onFire = player.isOnFire();
            if (wasOnFire.getOrDefault(uuid, false) && !onFire && health > 0.0F) {
                capture(player, PanicTriggerType.ESCAPED_FIRE, config, tick, false);
            }
            boolean drowning = player.getAir() <= 30;
            if (drowning && !wasDrowning.getOrDefault(uuid, false)) {
                capture(player, PanicTriggerType.DROWNING, config, tick, false);
            }
            float maxFall = Math.max(maximumFall.getOrDefault(uuid, 0.0F), player.fallDistance);
            if (player.isOnGround() && maxFall >= 8.0F && health > 0.0F) {
                capture(player, PanicTriggerType.LARGE_FALL, config, tick, false);
                maxFall = 0.0F;
            }
            if (tick % 20L == 0L && player.getServerWorld().getEntitiesByClass(HostileEntity.class,
                    player.getBoundingBox().expand(12.0D), hostile -> hostile.getTarget() == player).size() >= 3) {
                capture(player, PanicTriggerType.HOSTILE_CHASE, config, tick, false);
            }
            lastHealth.put(uuid, health);
            wasOnFire.put(uuid, onFire);
            wasDrowning.put(uuid, drowning);
            maximumFall.put(uuid, maxFall);
        }
    }

    public void observeDamage(ServerPlayerEntity player, DamageSource source, float baseDamage,
                              boolean blocked, EchoConfig config, long tick) {
        recentDamageTicks.put(player.getUuid(), tick);
        PanicTriggerType type = null;
        if (blocked) {
            type = PanicTriggerType.SHIELD_BLOCK;
        } else if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            type = PanicTriggerType.SURVIVED_EXPLOSION;
        } else if (source.isIn(DamageTypeTags.IS_FALL) && baseDamage >= 6.0F) {
            type = PanicTriggerType.LARGE_FALL;
        } else if (baseDamage >= 6.0F) {
            type = PanicTriggerType.RAPID_HEALTH_LOSS;
        }
        if (type != null && player.getHealth() > 0.0F) {
            capture(player, type, config, tick, false);
        }
    }

    public boolean capture(ServerPlayerEntity player, PanicTriggerType type, EchoConfig config, long tick, boolean forced) {
        if (!config.panicImprintsEnabled()) {
            return false;
        }
        long cooldown = (long) config.panicImprintMinimumEventIntervalMinutes() * 60L * 20L;
        if (!forced && tick - lastCaptureTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2) < cooldown) {
            return false;
        }
        PlayerRecording recording = recordings.get(player.getUuid());
        if (recording == null || recording.size() < 10) {
            return false;
        }
        List<RecordedFrame> all = recording.frames();
        int wanted = Math.min(all.size(), Math.max(20, 8 * 20 / config.recordingSampleIntervalTicks()));
        List<RecordedFrame> frames = new ArrayList<>(all.subList(all.size() - wanted, all.size()));
        PanicImprint.HealthCategory category = player.getHealth() <= 4.0F
                ? PanicImprint.HealthCategory.CRITICAL
                : player.getHealth() <= config.panicImprintHealthThreshold()
                ? PanicImprint.HealthCategory.LOW : PanicImprint.HealthCategory.STABLE;
        PanicImprint imprint = new PanicImprint(frames,
                player.getServerWorld().getRegistryKey().getValue().toString(), category, type,
                tick);
        hydrate(player.getUuid());
        Deque<PanicImprint> saved = imprints.computeIfAbsent(player.getUuid(), ignored -> new ArrayDeque<>());
        saved.addLast(imprint);
        while (saved.size() > config.panicImprintMaximumSaved()) {
            saved.removeFirst();
        }
        lastCaptureTicks.put(player.getUuid(), tick);
        stageManager.memory(player.getUuid()).addPanicImprint(PersistentPanicImprint.from(imprint),
                config.persistentPanicImprintMaximum());
        return true;
    }

    public PanicImprint latest(UUID playerUuid) {
        hydrate(playerUuid);
        Deque<PanicImprint> saved = imprints.get(playerUuid);
        return saved == null ? null : saved.peekLast();
    }

    public List<PanicImprint> list(UUID playerUuid) {
        hydrate(playerUuid);
        Deque<PanicImprint> saved = imprints.get(playerUuid);
        return saved == null ? List.of() : List.copyOf(saved);
    }

    public int clear(UUID playerUuid) {
        hydrate(playerUuid);
        Deque<PanicImprint> removed = imprints.remove(playerUuid);
        lastCaptureTicks.remove(playerUuid);
        hydrated.add(playerUuid);
        int persistent = stageManager.memory(playerUuid).clearPanicImprints();
        return Math.max(persistent, removed == null ? 0 : removed.size());
    }

    public boolean recentlyDamaged(UUID playerUuid, long tick, int withinTicks) {
        return tick - recentDamageTicks.getOrDefault(playerUuid, Long.MIN_VALUE / 2) <= withinTicks;
    }

    public void disconnect(UUID playerUuid) {
        imprints.remove(playerUuid);
        lastCaptureTicks.remove(playerUuid);
        lastHealth.remove(playerUuid);
        wasOnFire.remove(playerUuid);
        wasDrowning.remove(playerUuid);
        maximumFall.remove(playerUuid);
        recentDamageTicks.remove(playerUuid);
        hydrated.remove(playerUuid);
    }

    public void clearAll() {
        imprints.clear();
        lastCaptureTicks.clear();
        lastHealth.clear();
        wasOnFire.clear();
        wasDrowning.clear();
        maximumFall.clear();
        recentDamageTicks.clear();
        hydrated.clear();
    }

    private void hydrate(UUID playerUuid) {
        if (!hydrated.add(playerUuid)) {
            return;
        }
        Deque<PanicImprint> saved = imprints.computeIfAbsent(playerUuid, ignored -> new ArrayDeque<>());
        for (PersistentPanicImprint persistent : stageManager.memory(playerUuid).panicImprints()) {
            try {
                saved.addLast(persistent.toRuntime());
            } catch (RuntimeException ignored) {
                // The persistent codec already reports malformed entries; keep the remaining imprints usable.
            }
        }
    }
}
