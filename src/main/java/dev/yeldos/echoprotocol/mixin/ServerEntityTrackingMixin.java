package dev.yeldos.echoprotocol.mixin;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.world.ServerChunkLoadingManager$EntityTracker")
abstract class ServerEntityTrackingMixin {
    @Shadow @Final private Entity entity;

    @Shadow public abstract void stopTracking(ServerPlayerEntity player);

    @Inject(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void echoprotocol$restrictPrivateEchoTracking(ServerPlayerEntity player, CallbackInfo ci) {
        if (entity instanceof EchoEntity echo && !echo.visibleTo(player.getUuid())) {
            stopTracking(player);
            ci.cancel();
        }
    }
}
