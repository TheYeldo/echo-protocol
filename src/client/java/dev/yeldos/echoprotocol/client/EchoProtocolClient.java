package dev.yeldos.echoprotocol.client;

import dev.yeldos.echoprotocol.entity.EchoEntities;
import dev.yeldos.echoprotocol.rendering.EchoRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class EchoProtocolClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EchoEntities.ECHO, EchoRenderer::new);
    }
}
