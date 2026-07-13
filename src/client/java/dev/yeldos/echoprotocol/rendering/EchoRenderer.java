package dev.yeldos.echoprotocol.rendering;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class EchoRenderer extends BipedEntityRenderer<EchoEntity, PlayerEntityModel<EchoEntity>> {
    private final PlayerEntityModel<EchoEntity> classicModel;
    private final PlayerEntityModel<EchoEntity> slimModel;

    public EchoRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.0F);
        this.classicModel = this.model;
        this.slimModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
        addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
    }

    @Override
    public void render(EchoEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !entity.visibleTo(client.player.getUuid()) || entity.getReplayOpacity() <= 0.01F) {
            return;
        }
        SkinTextures textures = EchoSkinResolver.resolve(entity);
        this.model = textures.model() == SkinTextures.Model.SLIM ? slimModel : classicModel;
        applyPlayerLayerVisibility(entity);
        matrices.push();
        applyTypeVisualOffset(entity, matrices, tickDelta);
        super.render(entity, yaw, tickDelta, matrices, lightAware(vertexConsumers, entity), light);
        matrices.pop();
    }

    private void applyPlayerLayerVisibility(EchoEntity entity) {
        model.setVisible(true);
        model.hat.visible = true;
        model.jacket.visible = true;
        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;
        model.sneaking = entity.replaySneaking();
        BipedEntityModel.ArmPose armPose = entity.getHeldItemVisual().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
        model.rightArmPose = armPose;
        model.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
    }

    private void applyTypeVisualOffset(EchoEntity entity, MatrixStack matrices, float tickDelta) {
        if (entity.echoType() == EchoType.CORRUPTED && entity.echoState() != EchoState.REPLAYING) {
            float wobble = (float) Math.sin((entity.age + tickDelta) * 0.45F) * 0.012F;
            matrices.translate(wobble, 0.0F, -wobble);
        } else if (entity.echoType() == EchoType.MIMIC && entity.echoState() == EchoState.THREATENING) {
            float pulse = 1.0F + (float) Math.sin((entity.age + tickDelta) * 0.25F) * 0.012F;
            matrices.scale(pulse, pulse, pulse);
        }
    }

    private net.minecraft.client.render.VertexConsumerProvider lightAware(
            net.minecraft.client.render.VertexConsumerProvider vertexConsumers, EchoEntity entity) {
        return vertexConsumers;
    }

    @Override
    public Identifier getTexture(EchoEntity entity) {
        return EchoSkinResolver.resolve(entity).texture();
    }

    @Override
    protected boolean hasLabel(EchoEntity livingEntity) {
        return false;
    }

    @Override
    protected RenderLayer getRenderLayer(EchoEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        return RenderLayer.getEntityTranslucent(getTexture(entity));
    }
}
