package dev.yeldos.echoprotocol.client;

import dev.yeldos.echoprotocol.entity.EchoEntities;
import dev.yeldos.echoprotocol.network.ClearSkinCachePayload;
import dev.yeldos.echoprotocol.rendering.EchoRenderer;
import dev.yeldos.echoprotocol.rendering.EchoSkinResolver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class EchoProtocolClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EchoEntities.ECHO, EchoRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(ClearSkinCachePayload.ID, (payload, context) ->
                context.client().execute(() -> EchoSkinResolver.clear(payload.playerUuid())));
    }
}
