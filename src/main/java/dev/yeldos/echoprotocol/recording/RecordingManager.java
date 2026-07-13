package dev.yeldos.echoprotocol.recording;

import dev.yeldos.echoprotocol.config.EchoConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RecordingManager {
    private final Map<UUID, PlayerRecording> recordings = new HashMap<>();
    private long tick;

    public void tick(MinecraftServer server, EchoConfig config) {
        tick++;
        int capacity = config.maxFrames();
        if (tick % config.recordingSampleIntervalTicks() != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) {
                continue;
            }
            PlayerRecording recording = recording(player.getUuid());
            recording.resize(capacity);
            recording.add(RecordedFrame.capture(player, tick, recording.latest()));
            if (config.soundEchoes()) {
                RecordedFrame latest = recording.latest();
                if (latest != null && latest.walking() && latest.onGround() && tick % 8 == 0) {
                    recording.addSound(SoundMarker.of(tick, player.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_STONE_STEP, 0.35F, 0.85F));
                }
            }
        }
    }

    public PlayerRecording recording(UUID playerUuid) {
        return recordings.computeIfAbsent(playerUuid, ignored -> new PlayerRecording());
    }

    public PlayerRecording get(UUID playerUuid) {
        return recordings.get(playerUuid);
    }

    public void recordSound(ServerPlayerEntity player, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        recording(player.getUuid()).addSound(SoundMarker.of(tick, pos, sound, volume, pitch));
    }

    public void recordChat(ServerPlayerEntity player, String message) {
        recording(player.getUuid()).addChat(message);
    }

    public long tick() {
        return tick;
    }

    public void clear(UUID playerUuid) {
        recordings.remove(playerUuid);
    }

    public void clearAll() {
        recordings.clear();
    }
}
