package dev.yeldos.echoprotocol.rendering;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class EchoRenderer extends BipedEntityRenderer<EchoEntity, PlayerEntityRenderState, PlayerEntityModel> {
    private final PlayerEntityModel classicModel;
    private final PlayerEntityModel slimModel;

    public EchoRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel(context.getPart(EntityModelLayers.PLAYER), false), 0.0F);
        this.classicModel = this.model;
        this.slimModel = new PlayerEntityModel(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new EchoRenderState();
    }

    @Override
    public void updateRenderState(EchoEntity entity, PlayerEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        EchoRenderState echoState = (EchoRenderState) state;
        SkinTextures textures = EchoSkinResolver.resolve(entity);
        echoState.texture = textures.texture();
        echoState.slim = textures.model() == SkinTextures.Model.SLIM;
        echoState.opacity = entity.getReplayOpacity();
        MinecraftClient client = MinecraftClient.getInstance();
        echoState.visible = client.player != null && entity.visibleTo(client.player.getUuid());
        echoState.echoType = entity.echoType();
        echoState.echoState = entity.echoState();

        state.spectator = false;
        state.hatVisible = true;
        state.jacketVisible = true;
        state.leftSleeveVisible = true;
        state.rightSleeveVisible = true;
        state.leftPantsLegVisible = true;
        state.rightPantsLegVisible = true;
        state.isInSneakingPose = entity.replaySneaking();
        state.isSwimming = entity.replaySwimming();

        ItemStack held = entity.getHeldItemVisual().copy();
        state.rightHandStack = held;
        state.rightHandItemModel = held.isEmpty() ? null
                : itemRenderer.getModel(held, entity, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND);
        state.leftHandStack = ItemStack.EMPTY;
        state.leftHandItemModel = null;
        state.mainHandState.empty = held.isEmpty();
        state.mainHandState.itemUseAction = held.isEmpty() ? null : held.getUseAction();
        state.mainHandState.hasChargedCrossbow = false;
        state.offHandState.empty = true;
        state.offHandState.itemUseAction = null;
        state.offHandState.hasChargedCrossbow = false;
    }

    @Override
    public void render(PlayerEntityRenderState state, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        EchoRenderState echoState = (EchoRenderState) state;
        if (!echoState.visible || echoState.opacity <= 0.01F) {
            return;
        }
        this.model = echoState.slim ? slimModel : classicModel;
        matrices.push();
        applyTypeVisualOffset(echoState, matrices);
        super.render(state, matrices, lightAware(vertexConsumers, echoState.opacity), light);
        matrices.pop();
    }

    private void applyTypeVisualOffset(EchoRenderState state, MatrixStack matrices) {
        if (state.echoType == EchoType.CORRUPTED && state.echoState != EchoState.REPLAYING) {
            float wobble = (float) Math.sin(state.age * 0.45F) * 0.012F;
            matrices.translate(wobble, 0.0F, -wobble);
        } else if (state.echoType == EchoType.MIMIC && state.echoState == EchoState.THREATENING) {
            float pulse = 1.0F + (float) Math.sin(state.age * 0.25F) * 0.012F;
            matrices.scale(pulse, pulse, pulse);
        }
    }

    private VertexConsumerProvider lightAware(VertexConsumerProvider vertexConsumers, float opacity) {
        return new GhostAlphaVertexConsumerProvider(vertexConsumers, opacity);
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        return ((EchoRenderState) state).texture;
    }

    @Override
    protected boolean hasLabel(EchoEntity livingEntity, double squaredDistanceToCamera) {
        return false;
    }

    @Override
    protected RenderLayer getRenderLayer(PlayerEntityRenderState state, boolean showBody,
                                         boolean translucent, boolean showOutline) {
        return RenderLayer.getEntityTranslucent(getTexture(state));
    }

    private static final class EchoRenderState extends PlayerEntityRenderState {
        private Identifier texture;
        private EchoType echoType;
        private EchoState echoState;
        private float opacity;
        private boolean slim;
        private boolean visible;
    }

    private record GhostAlphaVertexConsumerProvider(VertexConsumerProvider delegate, float alpha) implements VertexConsumerProvider {
        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            return new GhostAlphaVertexConsumer(delegate.getBuffer(layer), alpha);
        }
    }

    private static final class GhostAlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        private GhostAlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, Math.round(alpha * this.alpha));
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }
    }
}
