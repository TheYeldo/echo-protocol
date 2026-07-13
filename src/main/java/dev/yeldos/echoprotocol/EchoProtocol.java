package dev.yeldos.echoprotocol;

import dev.yeldos.echoprotocol.command.EchoCommands;
import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.EchoEventDirector;
import dev.yeldos.echoprotocol.entity.EchoEntities;
import dev.yeldos.echoprotocol.event.EchoEventHooks;
import dev.yeldos.echoprotocol.recording.RecordingManager;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoProtocol implements ModInitializer {
    public static final String MOD_ID = "echoprotocol";
    public static final Logger LOGGER = LoggerFactory.getLogger("Echo Protocol");

    private static EchoConfig config;
    private static RecordingManager recordingManager;
    private static StageManager stageManager;
    private static EchoEventDirector eventDirector;

    @Override
    public void onInitialize() {
        config = EchoConfig.load();
        EchoEntities.register();

        recordingManager = new RecordingManager();
        stageManager = new StageManager();
        eventDirector = new EchoEventDirector(recordingManager, stageManager);

        EchoCommands.register(recordingManager, stageManager, eventDirector);
        EchoEventHooks.register(recordingManager, stageManager);

        ServerLifecycleEvents.SERVER_STARTED.register(EchoProtocol::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(EchoProtocol::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(EchoProtocol::onServerTick);
    }

    private static void onServerStarted(MinecraftServer server) {
        stageManager.load(server);
    }

    private static void onServerStopping(MinecraftServer server) {
        stageManager.save(server);
        recordingManager.clearAll();
        eventDirector.clear();
    }

    private static void onServerTick(MinecraftServer server) {
        if (!config.enabled()) {
            return;
        }
        recordingManager.tick(server, config);
        stageManager.tick(server, config);
        eventDirector.tick(server, config);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static EchoConfig config() {
        return config;
    }

    public static void reloadConfig() {
        config = EchoConfig.load();
    }
}
