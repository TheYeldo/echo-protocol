package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.entity.EchoEntities;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.recording.PlayerRecording;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.recording.SoundMarker;
import dev.yeldos.echoprotocol.stage.EchoStage;
import dev.yeldos.echoprotocol.stage.PlayerEchoState;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class EchoEventDirector {
    private final RecordingManager recordingManager;
    private final StageManager stageManager;

    public EchoEventDirector(RecordingManager recordingManager, StageManager stageManager) {
        this.recordingManager = recordingManager;
        this.stageManager = stageManager;
    }

    public void tick(MinecraftServer server, EchoConfig config) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerEchoState state = stageManager.state(player.getUuid());
            if (state.stage() == EchoStage.OBSERVATION || !canRunEvent(player, state)) {
                continue;
            }
            if (stageManager.tick() >= state.nextEventTick()) {
                boolean spawned = spawnReplay(player, false, config);
                stageManager.scheduleNextEvent(state, config);
                if (spawned && state.stage() == EchoStage.DEJA_VU) {
                    state.incrementStageOneEvents();
                }
            }
        }
    }

    public boolean spawnReplay(ServerPlayerEntity target, boolean forced, EchoConfig config) {
        PlayerRecording recording = recordingManager.get(target.getUuid());
        if (recording == null) {
            return false;
        }
        int minFrames = Math.max(2, config.minimumReplaySeconds() * 20 / config.recordingSampleIntervalTicks());
        int maxFrames = Math.max(minFrames, config.maximumReplaySeconds() * 20 / config.recordingSampleIntervalTicks());
        List<RecordedFrame> segment = recording.randomSegment(minFrames, maxFrames);
        if (segment.isEmpty()) {
            return false;
        }
        RecordedFrame start = segment.get(0);
        ServerWorld world = target.getServerWorld();
        EchoEntity echo = new EchoEntity(EchoEntities.ECHO, world);
        PlayerEchoState state = stageManager.state(target.getUuid());
        boolean corrupted = state.stage() == EchoStage.CORRUPTED_MEMORY && ThreadLocalRandom.current().nextBoolean();
        List<RecordedFrame> frames = corrupted ? corruptSegment(segment, target) : segment;
        echo.configure(target.getUuid(), config.sharedEchoes(), frames, config.recordingSampleIntervalTicks(), config, corrupted);
        echo.refreshPositionAndAngles(start.x(), start.y(), start.z(), start.bodyYaw(), start.pitch());
        world.spawnEntity(echo);

        state.incrementTotalEvents();
        stageManager.grant(target, "deja_vu");
        stageManager.grant(target, "that_was_me");
        if (corrupted) {
            stageManager.grant(target, "it_saw_me");
        }
        if (forced || ThreadLocalRandom.current().nextInt(6) == 0) {
            playMemorySound(target, recording, config);
        }
        if (config.torchFlicker()) {
            spawnTargetedParticles(target, new Vec3d(start.x(), start.y() + 1.1D, start.z()));
        }
        if (state.stage() == EchoStage.CORRUPTED_MEMORY && config.chatEchoes()) {
            maybeEchoChat(target, recording);
        }
        return true;
    }

    private boolean canRunEvent(ServerPlayerEntity player, PlayerEchoState state) {
        long now = stageManager.tick();
        if (now - state.lastJoinTick() < 20L * 60L || now - state.lastRespawnTick() < 20L * 30L) {
            return false;
        }
        return !player.isSleeping() && !state.activeEvent() && !player.isSpectator();
    }

    private static List<RecordedFrame> corruptSegment(List<RecordedFrame> original, ServerPlayerEntity target) {
        if (original.size() < 4) {
            return original;
        }
        int trim = ThreadLocalRandom.current().nextInt(0, Math.max(1, original.size() / 3));
        List<RecordedFrame> frames = original.subList(0, original.size() - trim);
        if (ThreadLocalRandom.current().nextBoolean()) {
            return frames.reversed();
        }
        return frames;
    }

    private static void playMemorySound(ServerPlayerEntity target, PlayerRecording recording, EchoConfig config) {
        if (!config.soundEchoes()) {
            return;
        }
        List<SoundMarker> markers = recording.soundMarkers();
        if (markers.isEmpty()) {
            return;
        }
        SoundMarker marker = markers.get(ThreadLocalRandom.current().nextInt(markers.size()));
        if (target.squaredDistanceTo(marker.x(), marker.y(), marker.z()) > 64.0D * 64.0D) {
            return;
        }
        if (config.sharedEchoes()) {
            target.getServerWorld().playSound(null, marker.x(), marker.y(), marker.z(), marker.sound(), SoundCategory.PLAYERS,
                    Math.min(marker.volume(), 0.6F), marker.pitch());
        } else {
            target.playSoundToPlayer(marker.sound(), SoundCategory.PLAYERS, Math.min(marker.volume(), 0.6F), marker.pitch());
        }
    }

    private static void spawnTargetedParticles(ServerPlayerEntity player, Vec3d pos) {
        player.getServerWorld().spawnParticles(player, ParticleTypes.SCULK_SOUL, true, pos.x, pos.y, pos.z,
                8, 0.15D, 0.35D, 0.15D, 0.01D);
    }

    private static void maybeEchoChat(ServerPlayerEntity target, PlayerRecording recording) {
        if (ThreadLocalRandom.current().nextInt(4) != 0) {
            return;
        }
        String message = recording.randomChat();
        if (!message.isBlank()) {
            target.sendMessage(Text.translatable("text.echoprotocol.chat_echo", target.getGameProfile().getName(), message), false);
        }
    }

    public void clear() {
    }
}
