package dev.yeldos.echoprotocol.mixin;

import dev.yeldos.echoprotocol.rendering.EchoRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    private static final int VANILLA_TRANSLUCENT_TINT = 0x26FFFFFF;

    @ModifyConstant(method = "render", constant = @Constant(intValue = VANILLA_TRANSLUCENT_TINT))
    private int echoprotocol$preserveConfiguredOpacity(int vanillaTint) {
        // Echo render states deliberately use Minecraft's translucent-entity path,
        // but the vanilla path multiplies every invisible entity by a fixed 15%
        // alpha. The Echo renderer already supplies its tracked, fade-aware alpha.
        return (Object) this instanceof EchoRenderer ? 0xFFFFFFFF : vanillaTint;
    }

    @Inject(method = "getMixColor", at = @At("HEAD"), cancellable = true)
    private void echoprotocol$applyOpacity(LivingEntityRenderState state,
                                           CallbackInfoReturnable<Integer> callback) {
        if ((Object) this instanceof EchoRenderer && state instanceof PlayerEntityRenderState playerState) {
            callback.setReturnValue(EchoRenderer.mixColor(playerState));
        }
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void echoprotocol$useTranslucentLayer(LivingEntityRenderState state, boolean showBody,
                                                   boolean translucent, boolean showOutline,
                                                   CallbackInfoReturnable<RenderLayer> callback) {
        if ((Object) this instanceof EchoRenderer && state instanceof PlayerEntityRenderState playerState) {
            callback.setReturnValue(EchoRenderer.renderLayer(playerState));
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void echoprotocol$prepareEchoRender(LivingEntityRenderState state, MatrixStack matrices,
                                                 OrderedRenderCommandQueue commandQueue,
                                                 CameraRenderState cameraState, CallbackInfo callback) {
        if ((Object) this instanceof EchoRenderer && state instanceof PlayerEntityRenderState playerState) {
            if (!EchoRenderer.shouldRender(playerState)) {
                callback.cancel();
                return;
            }
            matrices.push();
            EchoRenderer.applyVisualOffset(playerState, matrices);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void echoprotocol$finishEchoRender(LivingEntityRenderState state, MatrixStack matrices,
                                                OrderedRenderCommandQueue commandQueue,
                                                CameraRenderState cameraState, CallbackInfo callback) {
        if ((Object) this instanceof EchoRenderer && state instanceof PlayerEntityRenderState playerState
                && EchoRenderer.shouldRender(playerState)) {
            matrices.pop();
        }
    }
}
