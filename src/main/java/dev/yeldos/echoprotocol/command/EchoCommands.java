package dev.yeldos.echoprotocol.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.EchoEventDirector;
import dev.yeldos.echoprotocol.echo.EchoType;
import dev.yeldos.echoprotocol.echo.OriginalEventKind;
import dev.yeldos.echoprotocol.original.OriginalMovementMode;
import dev.yeldos.echoprotocol.network.ClearSkinCachePayload;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.stage.EchoStage;
import dev.yeldos.echoprotocol.stage.FamiliarLocation;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
                                                .then(CommandManager.argument("stage", IntegerArgumentType.integer(0, 3))
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
                        .then(CommandManager.literal("original")
                                .then(CommandManager.literal("spawn")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> forceOriginal(context.getSource(), EntityArgumentType.getPlayer(context, "player"), director, null))))
                                .then(CommandManager.literal("event")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("event", StringArgumentType.word())
                                                        .executes(context -> forceOriginalEvent(context.getSource(), EntityArgumentType.getPlayer(context, "player"),
                                                                director, StringArgumentType.getString(context, "event"))))))
                                .then(CommandManager.literal("confront")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    if (director.spawnOriginalConfrontation(player, EchoProtocol.config())) {
                                                        context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.original_confront",
                                                                player.getName().getString()), true);
                                                        return 1;
                                                    }
                                                    context.getSource().sendError(Text.translatable("text.echoprotocol.command.original_failed",
                                                            player.getName().getString()));
                                                    return 0;
                                                })))
                                .then(CommandManager.literal("movement-test")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                                        .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(
                                                                java.util.Arrays.stream(OriginalMovementMode.values())
                                                                        .map(OriginalMovementMode::commandName), builder))
                                                        .executes(context -> originalMovementTest(context.getSource(),
                                                                EntityArgumentType.getPlayer(context, "player"), director,
                                                                StringArgumentType.getString(context, "mode"))))))
                                .then(CommandManager.literal("status")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    String status = director.originalStatus(player);
                                                    context.getSource().sendFeedback(() -> Text.translatable(
                                                            "text.echoprotocol.command.original_status",
                                                            player.getName().getString(), status), false);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("stop")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    int stopped = director.stopEvents(player);
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.original_stop",
                                                            player.getName().getString(), stopped), true);
                                                    return stopped;
                                                }))))
                        .then(CommandManager.literal("familiar")
                                .then(CommandManager.literal("list")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> listFamiliar(context.getSource(), EntityArgumentType.getPlayer(context, "player"), stageManager))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    int cleared = stageManager.clearFamiliarLocations(player);
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.familiar_clear",
                                                            player.getName().getString(), cleared), true);
                                                    return cleared;
                                                })))
                                .then(CommandManager.literal("add-current")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    if (!EchoProtocol.config().originalFamiliarLocationsEnabled()) {
                                                        return featureDisabled(context.getSource(), "familiar locations");
                                                    }
                                                    int count = stageManager.addCurrentFamiliarLocation(player, EchoProtocol.config());
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.familiar_add_current",
                                                            player.getName().getString(), count), true);
                                                    return 1;
                                                }))))
                        .then(CommandManager.literal("false-memory")
                                .then(CommandManager.literal("spawn")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> falseMemory(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director, false))))
                                .then(CommandManager.literal("test-deviation")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> falseMemory(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director, true)))))
                        .then(CommandManager.literal("panic")
                                .then(CommandManager.literal("list")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> listPanic(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director))))
                                .then(CommandManager.literal("capture-current")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> panicCapture(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director))))
                                .then(CommandManager.literal("replay")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> panicReplay(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    int count = director.clearPanic(player.getUuid());
                                                    context.getSource().sendFeedback(() -> Text.translatable(
                                                            "text.echoprotocol.command.panic_clear", player.getName().getString(), count), true);
                                                    return count;
                                                }))))
                        .then(CommandManager.literal("peripheral")
                                .then(CommandManager.literal("spawn")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> peripheral(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director)))))
                        .then(CommandManager.literal("audio-residue")
                                .then(CommandManager.literal("play")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> audioResidue(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director)))))
                        .then(CommandManager.literal("habits")
                                .then(CommandManager.literal("list")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> listHabits(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    int count = director.clearHabits(player.getUuid());
                                                    context.getSource().sendFeedback(() -> Text.translatable(
                                                            "text.echoprotocol.command.habits_clear", player.getName().getString(), count), true);
                                                    return count;
                                                }))))
                        .then(CommandManager.literal("director")
                                .then(CommandManager.literal("status")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    context.getSource().sendFeedback(() -> Text.literal("Echo Protocol: "
                                                            + director.status(player, EchoProtocol.config())), false);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("history")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> listHistory(context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"), director))))
                                .then(CommandManager.literal("clear-history")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    int count = director.clearHistory(player.getUuid());
                                                    context.getSource().sendFeedback(() -> Text.translatable(
                                                            "text.echoprotocol.command.director_clear", player.getName().getString(), count), true);
                                                    return count;
                                                }))))
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
                                                    int recipients = 0;
                                                    for (ServerPlayerEntity recipient : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                                                        if (ServerPlayNetworking.canSend(recipient, ClearSkinCachePayload.ID)) {
                                                            ServerPlayNetworking.send(recipient, new ClearSkinCachePayload(player.getUuid()));
                                                            recipients++;
                                                        }
                                                    }
                                                    if (recipients == 0) {
                                                        context.getSource().sendError(Text.translatable("text.echoprotocol.command.skin_clear_cache_unavailable",
                                                                player.getName().getString()));
                                                        return 0;
                                                    }
                                                    int sent = recipients;
                                                    context.getSource().sendFeedback(() -> Text.translatable("text.echoprotocol.command.skin_clear_cache",
                                                            player.getName().getString(), sent), true);
                                                    return sent;
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
                                            director.clearPlayer(player);
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
        if (!EchoProtocol.config().memoryEchoEnabled()) {
            return featureDisabled(source, "memory Echo");
        }
        if (director.spawnReplay(player, true, EchoProtocol.config())) {
            source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.spawned", player.getName().getString()), true);
            return 1;
        }
        source.sendError(Text.translatable("text.echoprotocol.command.no_recording", player.getName().getString()));
        return 0;
    }

    private static int forceTypedReplay(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                        EchoEventDirector director, EchoType type, boolean hostile) {
        if (!typeEnabled(type, EchoProtocol.config())) {
            return featureDisabled(source, type.name().toLowerCase(Locale.ROOT) + " Echo");
        }
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

    private static int forceOriginal(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                     EchoEventDirector director, OriginalEventKind eventKind) {
        if (director.spawnOriginal(player, true, eventKind, EchoProtocol.config())) {
            source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.original_spawn",
                    player.getName().getString(), eventKind == null ? "auto" : eventKind.commandName()), true);
            return 1;
        }
        source.sendError(Text.translatable("text.echoprotocol.command.original_failed", player.getName().getString()));
        return 0;
    }

    private static int forceOriginalEvent(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                          EchoEventDirector director, String eventName) {
        try {
            return forceOriginal(source, player, director, OriginalEventKind.fromCommand(eventName));
        } catch (IllegalArgumentException exception) {
            String allowed = java.util.Arrays.stream(OriginalEventKind.values())
                    .map(OriginalEventKind::commandName)
                    .collect(Collectors.joining(", "));
            source.sendError(Text.translatable("text.echoprotocol.command.original_unknown_event", eventName, allowed));
            return 0;
        }
    }

    private static int originalMovementTest(net.minecraft.server.command.ServerCommandSource source,
                                            ServerPlayerEntity player, EchoEventDirector director, String modeName) {
        try {
            OriginalMovementMode mode = OriginalMovementMode.fromCommand(modeName);
            if (director.spawnOriginalMovementTest(player, mode, EchoProtocol.config())) {
                source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.original_movement_test",
                        player.getName().getString(), mode.commandName()), true);
                return 1;
            }
            source.sendError(Text.translatable("text.echoprotocol.command.original_failed",
                    player.getName().getString()));
            return 0;
        } catch (IllegalArgumentException exception) {
            String allowed = java.util.Arrays.stream(OriginalMovementMode.values())
                    .map(OriginalMovementMode::commandName).collect(Collectors.joining(", "));
            source.sendError(Text.translatable("text.echoprotocol.command.original_unknown_movement",
                    modeName, allowed));
            return 0;
        }
    }

    private static int listFamiliar(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                    StageManager stageManager) {
        List<FamiliarLocation> locations = stageManager.familiarLocations(player);
        if (locations.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.familiar_empty",
                    player.getName().getString()), false);
            return 0;
        }
        String preview = locations.stream()
                .limit(8)
                .map(location -> String.format(Locale.ROOT, "%s %s %d,%d,%d visits=%d",
                        location.type().name().toLowerCase(Locale.ROOT), location.dimension(),
                        location.pos().getX(), location.pos().getY(), location.pos().getZ(), location.visits()))
                .collect(Collectors.joining("; "));
        source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.familiar_list",
                player.getName().getString(), locations.size(), preview), false);
        return locations.size();
    }

    private static int testSound(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                 EchoEventDirector director, EchoType type) {
        if (director.playDebugSound(player, type)) {
            source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.sound_test",
                    player.getName().getString(), type.name().toLowerCase(java.util.Locale.ROOT)), false);
            return 1;
        }
        source.sendError(Text.translatable("text.echoprotocol.command.sound_test_failed",
                player.getName().getString(), type.name().toLowerCase(java.util.Locale.ROOT)));
        return 0;
    }

    private static int falseMemory(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                   EchoEventDirector director, boolean forceDeviation) {
        if (!EchoProtocol.config().falseMemoriesEnabled()) {
            return featureDisabled(source, "false memories");
        }
        return simpleSpawn(source, player,
                director.spawnFalseMemory(player, true, forceDeviation, EchoProtocol.config()),
                forceDeviation ? "false-memory deviation" : "false-memory");
    }

    private static int panicCapture(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                    EchoEventDirector director) {
        if (!EchoProtocol.config().panicImprintsEnabled()) {
            return featureDisabled(source, "panic imprints");
        }
        return simpleSpawn(source, player, director.capturePanic(player, EchoProtocol.config()), "panic capture");
    }

    private static int panicReplay(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                   EchoEventDirector director) {
        if (!EchoProtocol.config().panicImprintsEnabled()) {
            return featureDisabled(source, "panic imprints");
        }
        if (director.panicImprints(player.getUuid()).isEmpty()) {
            source.sendError(Text.translatable("text.echoprotocol.command.no_panic", player.getName().getString()));
            return 0;
        }
        return simpleSpawn(source, player, director.replayPanic(player, EchoProtocol.config(), true), "panic replay");
    }

    private static int peripheral(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                  EchoEventDirector director) {
        if (!EchoProtocol.config().peripheralEchoesEnabled()) {
            return featureDisabled(source, "peripheral echoes");
        }
        return simpleSpawn(source, player, director.spawnPeripheral(player, EchoProtocol.config(), true), "peripheral");
    }

    private static int audioResidue(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                    EchoEventDirector director) {
        if (!EchoProtocol.config().audioResidueEnabled()) {
            return featureDisabled(source, "audio residue");
        }
        if (director.audioResidues(player.getUuid()).isEmpty()) {
            source.sendError(Text.translatable("text.echoprotocol.command.no_audio_residue", player.getName().getString()));
            return 0;
        }
        return simpleSpawn(source, player, director.playAudioResidue(player, EchoProtocol.config(), true), "audio-residue");
    }

    private static int listPanic(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                 EchoEventDirector director) {
        var imprints = director.panicImprints(player.getUuid());
        String preview = imprints.stream().limit(8)
                .map(imprint -> imprint.triggerType().name().toLowerCase(Locale.ROOT) + ":"
                        + imprint.healthCategory().name().toLowerCase(Locale.ROOT) + "@" + imprint.capturedTick())
                .collect(Collectors.joining(", "));
        source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.panic_list",
                player.getName().getString(), imprints.size(), preview), false);
        return imprints.size();
    }

    private static int listHabits(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                  EchoEventDirector director) {
        var habits = director.habits(player.getUuid());
        String preview = habits.stream().limit(12)
                .map(habit -> habit.type().name().toLowerCase(Locale.ROOT) + "@"
                        + habit.position().getX() + "," + habit.position().getY() + "," + habit.position().getZ()
                        + " x" + habit.observations())
                .collect(Collectors.joining("; "));
        source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.habits_list",
                player.getName().getString(), habits.size(), preview), false);
        return habits.size();
    }

    private static int listHistory(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                   EchoEventDirector director) {
        var history = director.history(player.getUuid());
        String preview = history.stream().limit(12)
                .map(entry -> entry.category().name().toLowerCase(Locale.ROOT) + "@" + entry.tick())
                .collect(Collectors.joining(", "));
        source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.director_history",
                player.getName().getString(), history.size(), preview), false);
        return history.size();
    }

    private static int simpleSpawn(net.minecraft.server.command.ServerCommandSource source, ServerPlayerEntity player,
                                   boolean success, String event) {
        if (success) {
            source.sendFeedback(() -> Text.translatable("text.echoprotocol.command.v04_success",
                    event, player.getName().getString()), true);
            return 1;
        }
        source.sendError(Text.translatable("text.echoprotocol.command.v04_failed", event, player.getName().getString()));
        return 0;
    }

    private static int featureDisabled(net.minecraft.server.command.ServerCommandSource source, String feature) {
        source.sendError(Text.translatable("text.echoprotocol.command.feature_disabled", feature));
        return 0;
    }

    private static boolean typeEnabled(EchoType type, EchoConfig config) {
        return switch (type) {
            case MEMORY -> config.memoryEchoEnabled();
            case CORRUPTED -> config.corruptedEchoEnabled();
            case MIMIC -> config.mimicEchoEnabled();
            case FALSE_MEMORY -> config.falseMemoriesEnabled();
            case ORIGINAL -> config.originalEnabled();
        };
    }

    private static void setDebug(boolean enabled) {
        EchoConfig updated = EchoProtocol.config().withDebugLogging(enabled);
        EchoProtocol.applyConfig(updated);
        if (updated.debugLogging()) {
            EchoProtocol.LOGGER.info("Debug logging enabled.");
        }
    }
}
