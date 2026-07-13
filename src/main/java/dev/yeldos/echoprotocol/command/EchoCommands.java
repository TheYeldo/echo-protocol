package dev.yeldos.echoprotocol.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.EchoEventDirector;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.stage.EchoStage;
import dev.yeldos.echoprotocol.stage.StageManager;
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
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> forceReplay(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director))))
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

    private static void setDebug(boolean enabled) {
        EchoConfig updated = EchoProtocol.config().withDebugLogging(enabled);
        EchoProtocol.reloadConfig();
        if (updated.debugLogging()) {
            EchoProtocol.LOGGER.info("Debug logging enabled.");
        }
    }
}
