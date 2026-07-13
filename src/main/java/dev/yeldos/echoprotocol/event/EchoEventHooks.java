package dev.yeldos.echoprotocol.event;

import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

public final class EchoEventHooks {
    private EchoEventHooks() {
    }

    public static void register(RecordingManager recordingManager, StageManager stageManager) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> stageManager.markJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> stageManager.save(server));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> stageManager.markRespawn(newPlayer));

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                recordingManager.recordSound(serverPlayer, pos, SoundEvents.BLOCK_STONE_HIT, 0.45F, 0.8F);
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                var state = world.getBlockState(hitResult.getBlockPos());
                if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)) {
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_CHEST_OPEN, 0.35F, 0.8F);
                } else if (state.isOf(Blocks.OAK_DOOR) || state.isOf(Blocks.IRON_DOOR)) {
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_WOODEN_DOOR_OPEN, 0.35F, 0.85F);
                } else {
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.2F, 0.75F);
                }
            }
            return ActionResult.PASS;
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
                recordingManager.recordChat(sender, message.getContent().getString()));
    }
}
