package dev.yeldos.echoprotocol.sound;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.EchoType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EchoSoundPlayer {
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private EchoSoundPlayer() {
    }

    public static void playSpawnProfile(ServerPlayerEntity target, EchoType type, EchoConfig config, Vec3d pos) {
        if (!config.echoSoundEffects() && !config.soundEchoes()) {
            return;
        }
        switch (type) {
            case MEMORY -> play(target, "memory_ambient", SoundEvents.AMBIENT_CAVE.value(), config, pos, config.memoryEchoVolume(), 0.65F, 80);
            case CORRUPTED -> {
                if (config.staticEffectsEnabled()) {
                    play(target, "corrupted_static", SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, config, pos, config.corruptedEchoVolume(), 0.55F, 100);
                }
                play(target, "corrupted_ambient", SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), config, pos, config.corruptedEchoVolume() * 0.35F, 0.6F, 120);
            }
            case MIMIC -> {
                if (config.breathingEnabled()) {
                    play(target, "mimic_breath", SoundEvents.ENTITY_WARDEN_SNIFF, config, pos, config.mimicEchoVolume() * 0.35F, 0.8F, 80);
                }
                play(target, "mimic_pulse", SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, config, pos, config.mimicEchoVolume() * 0.25F, 0.55F, 160);
            }
            case ORIGINAL -> {
                play(target, "original_arrive", SoundEvents.BLOCK_RESPAWN_ANCHOR_AMBIENT, config, pos, 0.42F, 0.55F, 200);
                play(target, "original_presence", SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, config, pos, 0.18F, 0.65F, 100);
            }
        }
    }

    public static void playUnseenMove(ServerPlayerEntity target, EchoConfig config, Vec3d pos) {
        play(target, "unseen_move", SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, config, pos, config.mimicEchoVolume() * 0.25F, 0.75F, 60);
    }

    public static void playThreat(ServerPlayerEntity target, EchoConfig config, Vec3d pos) {
        play(target, "threat", SoundEvents.ENTITY_WARDEN_HEARTBEAT, config, pos, config.mimicEchoVolume() * 0.55F, 0.7F, 80);
    }

    public static void playOriginalConfrontation(ServerPlayerEntity target, EchoConfig config, Vec3d pos) {
        play(target, "original_confront", SoundEvents.ENTITY_WARDEN_HEARTBEAT, config, pos, 0.35F, 0.45F, 200);
        play(target, "original_darkness", SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), config, pos, 0.25F, 0.5F, 200);
    }

    public static void playDisappear(ServerPlayerEntity target, EchoType type, EchoConfig config, Vec3d pos) {
        float volume = switch (type) {
            case MEMORY -> config.memoryEchoVolume();
            case CORRUPTED -> config.corruptedEchoVolume();
            case MIMIC -> config.mimicEchoVolume();
            case ORIGINAL -> 0.55F;
        };
        play(target, "disappear_" + type.name(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, config, pos, volume * 0.35F, 0.7F, 60);
    }

    private static void play(ServerPlayerEntity target, String key, SoundEvent sound, EchoConfig config, Vec3d pos, float volume, float pitch, int cooldownTicks) {
        long now = target.getServerWorld().getTime();
        String cooldownKey = target.getUuid() + ":" + key;
        if (now - COOLDOWNS.getOrDefault(cooldownKey, -999999L) < cooldownTicks) {
            return;
        }
        COOLDOWNS.put(cooldownKey, now);
        float finalVolume = Math.max(0.0F, Math.min(1.0F, volume * config.echoMasterVolume()));
        if (finalVolume <= 0.0F) {
            return;
        }
        if (config.sharedEchoes()) {
            target.getServerWorld().playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, finalVolume, pitch);
        } else {
            target.playSoundToPlayer(sound, SoundCategory.PLAYERS, finalVolume, pitch);
        }
    }

    public static void clear(UUID uuid) {
        COOLDOWNS.keySet().removeIf(key -> key.startsWith(uuid.toString() + ":"));
    }
}
