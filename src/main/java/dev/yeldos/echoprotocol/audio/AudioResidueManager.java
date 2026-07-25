package dev.yeldos.echoprotocol.audio;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AudioResidueManager {
    private static final int MAX_CAPTURED_PER_PLAYER = 24;
    private final StageManager stageManager;
    private final Map<UUID, Deque<AudioResidue>> residues = new HashMap<>();
    private final Map<UUID, Integer> sessionPlays = new HashMap<>();
    private final Map<UUID, Long> lastPlayTicks = new HashMap<>();

    public AudioResidueManager(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    public void capture(ServerPlayerEntity player, BlockPos position, SoundEvent sound, float volume, float pitch,
                        AudioResidueEvent event, EchoConfig config, long tick) {
        if (!config.audioResidueEnabled()) {
            return;
        }
        Identifier id = Registries.SOUND_EVENT.getId(sound);
        if (id == null || !Registries.SOUND_EVENT.containsId(id) || !isSafeSound(id)) {
            return;
        }
        Deque<AudioResidue> history = residues.computeIfAbsent(player.getUuid(), ignored -> new ArrayDeque<>());
        history.addLast(new AudioResidue(id, event,
                player.getServerWorld().getRegistryKey().getValue().toString(), position, volume, pitch, tick));
        while (history.size() > MAX_CAPTURED_PER_PLAYER) {
            history.removeFirst();
        }
    }

    public boolean play(ServerPlayerEntity target, EchoConfig config, long tick, boolean forced) {
        if (!config.audioResidueEnabled() && !forced) {
            return false;
        }
        if (!forced && sessionPlays.getOrDefault(target.getUuid(), 0) >= config.audioResidueMaximumPerSession()) {
            return false;
        }
        long cooldown = (long) config.audioResidueMinimumIntervalMinutes() * 60L * 20L;
        if (!forced && tick - lastPlayTicks.getOrDefault(target.getUuid(), Long.MIN_VALUE / 2) < cooldown) {
            return false;
        }
        AudioResidue residue = latestInDimension(target);
        if (residue == null || !Registries.SOUND_EVENT.containsId(residue.soundId())) {
            return false;
        }
        Optional<Vec3d> safe = SafeEchoPositionFinder.findSpawn(target.getServerWorld(), target,
                residue.position().toCenterPos(), config);
        if (safe.isEmpty()) {
            return false;
        }
        SoundEvent sound = Registries.SOUND_EVENT.get(residue.soundId());
        Vec3d pos = safe.get();
        float volume = Math.min(0.55F, residue.volume()) * config.echoMasterVolume();
        if (config.sharedEchoes()) {
            target.getServerWorld().playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, residue.pitch());
        } else {
            target.playSoundToPlayer(sound, SoundCategory.PLAYERS, volume, residue.pitch());
        }
        sessionPlays.merge(target.getUuid(), 1, Integer::sum);
        lastPlayTicks.put(target.getUuid(), tick);
        stageManager.grant(target, "not_my_footsteps");
        return true;
    }

    public List<AudioResidue> list(UUID playerUuid) {
        Deque<AudioResidue> history = residues.get(playerUuid);
        return history == null ? List.of() : List.copyOf(history);
    }

    public int sessionPlays(UUID playerUuid) {
        return sessionPlays.getOrDefault(playerUuid, 0);
    }

    public void disconnect(UUID playerUuid) {
        residues.remove(playerUuid);
        sessionPlays.remove(playerUuid);
        lastPlayTicks.remove(playerUuid);
    }

    public void clearAll() {
        residues.clear();
        sessionPlays.clear();
        lastPlayTicks.clear();
    }

    private AudioResidue latestInDimension(ServerPlayerEntity target) {
        Deque<AudioResidue> history = residues.get(target.getUuid());
        if (history == null) {
            return null;
        }
        String dimension = target.getServerWorld().getRegistryKey().getValue().toString();
        for (AudioResidue residue : history.reversed()) {
            if (dimension.equals(residue.dimension())
                    && target.getServerWorld().isChunkLoaded(residue.position())
                    && residue.position().getSquaredDistance(target.getBlockPos()) <= 64.0D * 64.0D) {
                return residue;
            }
        }
        return null;
    }

    private static boolean isSafeSound(Identifier id) {
        String path = id.getPath();
        return path.contains("step") || path.contains("door") || path.contains("chest")
                || path.contains("furnace") || path.contains("item") || path.contains("bow")
                || path.contains("shield") || path.contains("eat") || path.contains("attack")
                || path.contains("stonecutter") || path.contains("pickup");
    }
}
