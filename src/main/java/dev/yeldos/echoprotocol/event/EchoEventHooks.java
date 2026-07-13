package dev.yeldos.echoprotocol.event;

import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.stage.FamiliarLocationType;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.NetherPortalBlock;
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
                if (state.getBlock() instanceof ChestBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.CHEST, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_CHEST_OPEN, 0.35F, 0.8F);
                } else if (state.getBlock() instanceof BedBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.BED, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_WOOL_PLACE, 0.2F, 0.7F);
                } else if (state.getBlock() instanceof CraftingTableBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.CRAFTING, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 0.2F, 0.9F);
                } else if (state.getBlock() instanceof AbstractFurnaceBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.FURNACE, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, 0.2F, 0.75F);
                } else if (state.getBlock() instanceof DoorBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.DOORWAY, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_WOODEN_DOOR_OPEN, 0.35F, 0.85F);
                } else if (state.getBlock() instanceof NetherPortalBlock || state.isOf(Blocks.END_PORTAL) || state.isOf(Blocks.END_GATEWAY)) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.PORTAL, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_PORTAL_AMBIENT, 0.2F, 0.65F);
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
