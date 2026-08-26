package dev.yeldos.echoprotocol.rendering;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

public final class EchoRenderer extends BipedEntityRenderer<EchoEntity, PlayerEntityRenderState, PlayerEntityModel> {
    private final PlayerEntityModel classicModel;
    private final PlayerEntityModel slimModel;

    public EchoRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel(context.getPart(EntityModelLayers.PLAYER), false), 0.0F);
        this.classicModel = this.model;
        // Yarn 1.21.9 maps the slim player layer and its equipment-layer bundle
        // to the same field name. Constructing the canonical layer key avoids
        // selecting the equipment bundle while retaining vanilla model data.
        EntityModelLayer slimLayer = new EntityModelLayer(Identifier.ofVanilla("player_slim"), "main");
        this.slimModel = new PlayerEntityModel(context.getPart(slimLayer), true);
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
        echoState.texture = textures.body().texturePath();
        state.skinTextures = textures;
        echoState.slim = textures.model() == PlayerSkinType.SLIM;
        this.model = echoState.slim ? slimModel : classicModel;
        echoState.opacity = entity.getReplayOpacity();
        MinecraftClient client = MinecraftClient.getInstance();
        echoState.visible = client.player != null && entity.visibleTo(client.player.getUuid());
        echoState.echoType = entity.echoType();
        echoState.echoState = entity.echoState();
        // Retain the small existing light-floor for low-light readability. A restrained blue-white mix color
        // gives the skin a ghostly cast, while the normal translucent layer continues to use depth testing.
        state.light = LightmapTextureManager.applyEmission(state.light, 5);

        state.spectator = false;
        state.invisible = true;
        state.invisibleToPlayer = false;
        state.hatVisible = true;
        state.jacketVisible = true;
        state.leftSleeveVisible = true;
        state.rightSleeveVisible = true;
        state.leftPantsLegVisible = true;
        state.rightPantsLegVisible = true;
        state.isInSneakingPose = entity.replaySneaking();
        state.isSwimming = entity.replaySwimming();

    }

    @Override
    protected BipedEntityModel.ArmPose getArmPose(EchoEntity entity, Arm arm) {
        return arm == entity.getMainArm() && !entity.getHeldItemVisual().isEmpty()
                ? BipedEntityModel.ArmPose.ITEM
                : BipedEntityModel.ArmPose.EMPTY;
    }

    @Override
    protected int getMixColor(PlayerEntityRenderState state) {
        return mixColor(state);
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
        return renderLayer(state);
    }

    public static boolean shouldRender(PlayerEntityRenderState state) {
        EchoRenderState echoState = (EchoRenderState) state;
        return echoState.visible && echoState.opacity > 0.01F;
    }

    public static int mixColor(PlayerEntityRenderState state) {
        return EchoRenderColor.withOpacity(((EchoRenderState) state).opacity);
    }

    public static RenderLayer renderLayer(PlayerEntityRenderState state) {
        return RenderLayers.entityTranslucent(((EchoRenderState) state).texture);
    }

    public static void applyVisualOffset(PlayerEntityRenderState state, MatrixStack matrices) {
        EchoRenderState echoState = (EchoRenderState) state;
        if (echoState.echoType == EchoType.CORRUPTED && echoState.echoState != EchoState.REPLAYING) {
            float wobble = (float) Math.sin(echoState.age * 0.45F) * 0.012F;
            matrices.translate(wobble, 0.0F, -wobble);
        } else if (echoState.echoType == EchoType.MIMIC && echoState.echoState == EchoState.THREATENING) {
            float pulse = 1.0F + (float) Math.sin(echoState.age * 0.25F) * 0.012F;
            matrices.scale(pulse, pulse, pulse);
        }
    }

    private static final class EchoRenderState extends PlayerEntityRenderState {
        private Identifier texture;
        private EchoType echoType;
        private EchoState echoState;
        private float opacity;
        private boolean slim;
        private boolean visible;
    }

}
