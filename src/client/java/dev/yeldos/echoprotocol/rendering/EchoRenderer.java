package dev.yeldos.echoprotocol.rendering;

import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class EchoRenderer extends BipedEntityRenderer<EchoEntity, BipedEntityModel<EchoEntity>> {
    private static final Identifier FALLBACK_TEXTURE = EchoProtocol.id("textures/entity/echo.png");

    public EchoRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.0F);
        addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
    }

    @Override
    public void render(EchoEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !entity.visibleTo(client.player.getUuid()) || entity.getReplayOpacity() <= 0.01F) {
            return;
        }
        model.sneaking = entity.replaySneaking();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(EchoEntity entity) {
        return FALLBACK_TEXTURE;
    }

    @Override
    protected RenderLayer getRenderLayer(EchoEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        return RenderLayer.getEntityTranslucent(getTexture(entity));
    }
}
