package dev.yeldos.echoprotocol.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.EchoEventDirector;
import dev.yeldos.echoprotocol.echo.EchoType;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.stage.EchoStage;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class EchoCommands {
    private EchoCommands() {
    }

    public static void register(RecordingManager recordingManager, StageManager stageManager, EchoEventDirector director) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("echo_protocol")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("stage")
                                .then(CommandManager.literal("get")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.stage_get",
                                                            player.getName().getString(), stageManager.state(player.getUuid()).stage().id()), false);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("stage", IntegerArgumentType.integer(0, 2))
                                                        .executes(context -> {
                                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                            int stage = IntegerArgumentType.getInteger(context, "stage");
                                                            stageManager.state(player.getUuid()).setStage(EchoStage.fromId(stage));
                                                            context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.stage_set",
                                                                    player.getName().getString(), stage), true);
                                                            return 1;
                                                        })))))
                        .then(CommandManager.literal("spawn")
                                .then(CommandManager.literal("memory")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> forceTypedReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.MEMORY, false))))
                                .then(CommandManager.literal("corrupted")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> forceTypedReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.CORRUPTED, false))))
                                .then(CommandManager.literal("mimic")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> forceTypedReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.MIMIC, false))))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> forceReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director))))
                        .then(CommandManager.literal("event")
                                .then(CommandManager.literal("stop")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    int stopped = director.stopEvents(player);
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.event_stop",
                                                            player.getName().getString(), stopped), true);
                                                    return stopped;
                                                }))))
                        .then(CommandManager.literal("mimic")
                                .then(CommandManager.literal("hostile")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> setMimicMode(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, true))))
                                .then(CommandManager.literal("harmless")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> setMimicMode(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, false)))))
                        .then(CommandManager.literal("skin")
                                .then(CommandManager.literal("status")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    boolean hasTextures = player.getGameProfile().getProperties().containsKey("textures");
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.skin_status",
                                                            player.getName().getString(), hasTextures, EchoProtocol.config().realPlayerSkins(), EchoProtocol.config().skinCacheEnabled()), false);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("clear-cache")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.skin_clear_cache",
                                                            player.getName().getString()), true);
                                                    return 1;
                                                }))))
                        .then(CommandManager.literal("visual")
                                .then(CommandManager.literal("memory")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> forceTypedReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.MEMORY, false))))
                                .then(CommandManager.literal("corrupted")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> forceTypedReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.CORRUPTED, false))))
                                .then(CommandManager.literal("mimic")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> forceTypedReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.MIMIC, false)))))
                        .then(CommandManager.literal("position")
                                .then(CommandManager.literal("test")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    boolean found = SafeEchoPositionFinder.findSpawn(player.getServerWorld(), player,
                                                            player.getPos().subtract(player.getRotationVec(1.0F).multiply(EchoProtocol.config().minimumEchoSpawnDistance())),
                                                            EchoProtocol.config()).isPresent();
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.position_test",
                                                            player.getName().getString(), found), false);
                                                    return found ? 1 : 0;
                                                }))))
                        .then(CommandManager.literal("sound")
                                .then(CommandManager.literal("test")
                                        .then(CommandManager.literal("memory")
                                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                        .executes(context -> testSound(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.MEMORY))))
                                        .then(CommandManager.literal("corrupted")
                                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                        .executes(context -> testSound(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.CORRUPTED))))
                                        .then(CommandManager.literal("mimic")
                                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                                        .executes(context -> testSound(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, EchoType.MIMIC)))))
                        )
                        .then(CommandManager.literal("replay")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> forceReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director))))
                        .then(CommandManager.literal("clear")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                            recordingManager.clear(player.getUuid());
                                            stageManager.clear(player.getUuid());
                                            context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.cleared",
                                                    player.getName().getString()), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("reload")
                                .executes(context -> {
                                    EchoProtocol.reloadConfig();
                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.reload"), true);
                                    return 1;
                                }))
                        .then(CommandManager.literal("debug")
                                .then(CommandManager.literal("on")
                                        .executes(context -> {
                                            setDebug(true);
                                            context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.debug_on"), true);
                                            return 1;
                                        }))
                                .then(CommandManager.literal("off")
                                        .executes(context -> {
                                            setDebug(false);
                                            context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.debug_off"), true);
                                            return 1;
                                        })))
        ));
    }

    private static int forceReplay(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player, EchoEventDirector director) {
        if (director.spawnReplay(player, true, EchoProtocol.config())) {
            source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.spawned", player.getName().getString()), true);
            return 1;
        }
        source.sendError(Text.translatable("text.echoprotocol.command.no_recording", player.getName().getString()));
        return 0;
    }

    private static int forceTypedReplay(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                        EchoEventDirector director, EchoType type, boolean hostile) {
        if (director.spawnEcho(player, type, true, hostile, EchoProtocol.config())) {
            source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.spawned_type",
                    player.getName().getString(), type.name().toLowerCase(java.util.Locale.ROOT)), true);
            return 1;
        }
        source.sendError(Text.translatable("text.echoprotocol.command.no_recording", player.getName().getString()));
        return 0;
    }

    private static int setMimicMode(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                    EchoEventDirector director, boolean hostile) {
        if (director.setMimicHostile(player, hostile)) {
            source.sendFeedback(() -> Text.translatable(hostile
                    ? "text.echoprotocol.command.mimic_hostile"
                    : "text.echoprotocol.command.mimic_harmless", player.getName().getString()), true);
            return 1;
        }
        source.sendError(Text.translatable("text.echoprotocol.command.no_active_mimic", player.getName().getString()));
        return 0;
    }

    private static int testSound(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                 EchoEventDirector director, EchoType type) {
        director.playDebugSound(player, type);
        source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.sound_test",
                player.getName().getString(), type.name().toLowerCase(java.util.Locale.ROOT)), false);
        return 1;
    }

    private static void setDebug(boolean enabled) {
        EchoConfig updated = EchoProtocol.config().withDebugLogging(enabled);
        EchoProtocol.reloadConfig();
        if (updated.debugLogging()) {
            EchoProtocol.LOGGER.info("Debug logging enabled.");
        }
    }
}
