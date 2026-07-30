package dev.yeldos.echoprotocol.event;

import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.audio.AudioResidueEvent;
import dev.yeldos.echoprotocol.echo.EchoEventDirector;
import dev.yeldos.echoprotocol.habit.HabitType;
import dev.yeldos.echoprotocol.stage.FamiliarLocationType;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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

    public static void register(RecordingManager recordingManager, StageManager stageManager, EchoEventDirector director) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> stageManager.markJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            director.onDisconnect(handler.player);
            stageManager.save(server);
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            stageManager.markRespawn(newPlayer);
            director.onRespawn(newPlayer);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayerEntity player) {
                director.observeDamage(player, source, baseDamage, blocked);
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                recordingManager.recordSound(serverPlayer, pos, SoundEvents.BLOCK_STONE_HIT, 0.45F, 0.8F);
                director.recordInteraction(serverPlayer, HabitType.FREQUENT_ITEM, pos, serverPlayer.getMainHandStack(),
                        SoundEvents.BLOCK_STONE_HIT, 0.45F, 0.8F, AudioResidueEvent.TOOL);
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                var state = world.getBlockState(hitResult.getBlockPos());
                if (state.getBlock() instanceof ChestBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.CHEST, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_CHEST_OPEN, 0.35F, 0.8F);
                    director.recordInteraction(serverPlayer, HabitType.STORAGE, hitResult.getBlockPos(), serverPlayer.getMainHandStack(),
                            SoundEvents.BLOCK_CHEST_OPEN, 0.35F, 0.8F, AudioResidueEvent.CONTAINER);
                } else if (state.getBlock() instanceof BedBlock) {
                    director.noteSleep(serverPlayer);
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.BED, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_WOOL_PLACE, 0.2F, 0.7F);
                    director.recordInteraction(serverPlayer, HabitType.SLEEPING, hitResult.getBlockPos(), serverPlayer.getMainHandStack(),
                            SoundEvents.BLOCK_WOOL_PLACE, 0.2F, 0.7F, AudioResidueEvent.ITEM);
                } else if (state.getBlock() instanceof CraftingTableBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.CRAFTING, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 0.2F, 0.9F);
                    director.recordInteraction(serverPlayer, HabitType.CRAFTING, hitResult.getBlockPos(), serverPlayer.getMainHandStack(),
                            SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 0.2F, 0.9F, AudioResidueEvent.CRAFTING);
                } else if (state.getBlock() instanceof AbstractFurnaceBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.FURNACE, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, 0.2F, 0.75F);
                    director.recordInteraction(serverPlayer, HabitType.FURNACE, hitResult.getBlockPos(), serverPlayer.getMainHandStack(),
                            SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, 0.2F, 0.75F, AudioResidueEvent.FURNACE);
                } else if (state.getBlock() instanceof DoorBlock) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.DOORWAY, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_WOODEN_DOOR_OPEN, 0.35F, 0.85F);
                    director.recordInteraction(serverPlayer, HabitType.ENTRY_ROUTE, hitResult.getBlockPos(), serverPlayer.getMainHandStack(),
                            SoundEvents.BLOCK_WOODEN_DOOR_OPEN, 0.35F, 0.85F, AudioResidueEvent.DOOR);
                } else if (state.getBlock() instanceof NetherPortalBlock || state.isOf(Blocks.END_PORTAL) || state.isOf(Blocks.END_GATEWAY)) {
                    stageManager.recordFamiliarLocation(serverPlayer, FamiliarLocationType.PORTAL, hitResult.getBlockPos(), EchoProtocol.config());
                    recordingManager.recordSound(serverPlayer, hitResult.getBlockPos(), SoundEvents.BLOCK_PORTAL_AMBIENT, 0.2F, 0.65F);
                    director.recordInteraction(serverPlayer, HabitType.PORTAL, hitResult.getBlockPos(), serverPlayer.getMainHandStack(),
                            SoundEvents.BLOCK_PORTAL_AMBIENT, 0.2F, 0.65F, AudioResidueEvent.ITEM);
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
