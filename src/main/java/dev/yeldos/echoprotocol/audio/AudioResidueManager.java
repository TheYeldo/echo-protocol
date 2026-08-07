package dev.yeldos.echoprotocol.audio;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
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
import java.util.HashSet;
import java.util.Set;

public final class AudioResidueManager {
    private static final int MAX_CAPTURED_PER_PLAYER = 24;
    private final StageManager stageManager;
    private final Map<UUID, Deque<AudioResidue>> residues = new HashMap<>();
    private final Map<UUID, Integer> sessionPlays = new HashMap<>();
    private final Map<UUID, Long> lastPlayTicks = new HashMap<>();
    private final Map<UUID, Long> lastPersistentCaptureTicks = new HashMap<>();
    private final Map<UUID, Vec3d> lastPlaybackPositions = new HashMap<>();
    private final Set<UUID> hydrated = new HashSet<>();

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
        hydrate(player.getUuid());
        Deque<AudioResidue> history = residues.computeIfAbsent(player.getUuid(), ignored -> new ArrayDeque<>());
        history.addLast(new AudioResidue(id, event,
                player.getEntityWorld().getRegistryKey().getValue().toString(), position, volume, pitch, tick));
        while (history.size() > MAX_CAPTURED_PER_PLAYER) {
            history.removeFirst();
        }
        long persistenceInterval = event == AudioResidueEvent.FOOTSTEP ? 20L * 60L : 20L * 10L;
        long previousPersistentCapture = lastPersistentCaptureTicks.getOrDefault(
                player.getUuid(), Long.MIN_VALUE / 2);
        if (tick - previousPersistentCapture >= persistenceInterval) {
            stageManager.memory(player.getUuid()).addAudioResidue(history.peekLast(),
                    config.persistentAudioResidueMaximum());
            lastPersistentCaptureTicks.put(player.getUuid(), tick);
        }
    }

    public boolean play(ServerPlayerEntity target, EchoConfig config, long tick, boolean forced, boolean awardsProgress) {
        if (!config.audioResidueEnabled()) {
            return reject(config, target, "feature disabled");
        }
        if (!forced && sessionPlays.getOrDefault(target.getUuid(), 0) >= config.audioResidueMaximumPerSession()) {
            return reject(config, target, "session maximum reached");
        }
        long cooldown = (long) config.audioResidueMinimumIntervalMinutes() * 60L * 20L;
        if (!forced && tick - lastPlayTicks.getOrDefault(target.getUuid(), Long.MIN_VALUE / 2) < cooldown) {
            return reject(config, target, "cooldown active");
        }
        AudioResidue residue = latestInDimension(target);
        if (residue == null || !Registries.SOUND_EVENT.containsId(residue.soundId())) {
            return reject(config, target, "no allowed captured sound in the current loaded dimension");
        }
        Optional<Vec3d> safe = SafeEchoPositionFinder.findSpawn(target.getEntityWorld(), target,
                residue.position().toCenterPos(), config);
        if (safe.isEmpty()) {
            return reject(config, target, "no valid empty playback position");
        }
        SoundEvent sound = Registries.SOUND_EVENT.get(residue.soundId());
        Vec3d pos = safe.get();
        float volume = Math.min(0.55F, residue.volume()) * config.echoMasterVolume();
        if (volume <= 0.0F) {
            return reject(config, target, "master volume is muted");
        }
        if (config.sharedEchoes()) {
            target.getEntityWorld().playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, residue.pitch());
        } else {
            target.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(sound),
                    SoundCategory.PLAYERS, pos.x, pos.y, pos.z, volume, residue.pitch(),
                    target.getRandom().nextLong()));
        }
        sessionPlays.merge(target.getUuid(), 1, Integer::sum);
        lastPlayTicks.put(target.getUuid(), tick);
        lastPlaybackPositions.put(target.getUuid(), pos);
        if (awardsProgress) {
            stageManager.grant(target, "not_my_footsteps");
        }
        return true;
    }

    public List<AudioResidue> list(UUID playerUuid) {
        hydrate(playerUuid);
        Deque<AudioResidue> history = residues.get(playerUuid);
        return history == null ? List.of() : List.copyOf(history);
    }

    public int sessionPlays(UUID playerUuid) {
        return sessionPlays.getOrDefault(playerUuid, 0);
    }

    public Optional<Vec3d> lastPlaybackPosition(UUID playerUuid) {
        return Optional.ofNullable(lastPlaybackPositions.get(playerUuid));
    }

    public boolean isEligible(ServerPlayerEntity target, EchoConfig config, long tick) {
        long cooldown = (long) config.audioResidueMinimumIntervalMinutes() * 60L * 20L;
        return config.audioResidueEnabled()
                && sessionPlays.getOrDefault(target.getUuid(), 0) < config.audioResidueMaximumPerSession()
                && tick - lastPlayTicks.getOrDefault(target.getUuid(), Long.MIN_VALUE / 2) >= cooldown
                && latestInDimension(target) != null;
    }

    public void disconnect(UUID playerUuid) {
        residues.remove(playerUuid);
        sessionPlays.remove(playerUuid);
        lastPlayTicks.remove(playerUuid);
        lastPersistentCaptureTicks.remove(playerUuid);
        lastPlaybackPositions.remove(playerUuid);
        hydrated.remove(playerUuid);
    }

    public void clearAll() {
        residues.clear();
        sessionPlays.clear();
        lastPlayTicks.clear();
        lastPersistentCaptureTicks.clear();
        lastPlaybackPositions.clear();
        hydrated.clear();
    }

    private AudioResidue latestInDimension(ServerPlayerEntity target) {
        hydrate(target.getUuid());
        Deque<AudioResidue> history = residues.get(target.getUuid());
        if (history == null) {
            return null;
        }
        String dimension = target.getEntityWorld().getRegistryKey().getValue().toString();
        for (AudioResidue residue : history.reversed()) {
            if (dimension.equals(residue.dimension())
                    && target.getEntityWorld().isChunkLoaded(residue.position())
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

    private static boolean reject(EchoConfig config, ServerPlayerEntity target, String reason) {
        if (config.debugLogging()) {
            EchoProtocol.LOGGER.debug("Audio Residue rejected for {}: {}", target.getUuid(), reason);
        }
        return false;
    }

    public int clear(UUID playerUuid) {
        hydrate(playerUuid);
        Deque<AudioResidue> removed = residues.remove(playerUuid);
        lastPersistentCaptureTicks.remove(playerUuid);
        hydrated.add(playerUuid);
        int persistent = stageManager.memory(playerUuid).clearAudioResidues();
        return Math.max(persistent, removed == null ? 0 : removed.size());
    }

    private void hydrate(UUID playerUuid) {
        if (!hydrated.add(playerUuid)) {
            return;
        }
        Deque<AudioResidue> history = residues.computeIfAbsent(playerUuid, ignored -> new ArrayDeque<>());
        for (AudioResidue residue : stageManager.memory(playerUuid).audioResidues()) {
            history.addLast(residue);
        }
        while (history.size() > MAX_CAPTURED_PER_PLAYER) {
            history.removeFirst();
        }
    }
}
