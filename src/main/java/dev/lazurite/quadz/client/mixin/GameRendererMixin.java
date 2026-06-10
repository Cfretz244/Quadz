package dev.lazurite.quadz.client.mixin;

import dev.lazurite.quadz.client.QuadzClient;
import dev.lazurite.quadz.client.render.RenderHooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // 1.21: GameRenderer.renderLevel now takes a DeltaTracker (no PoseStack/tickDelta/nanoTime).
    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void renderLevel$HEAD(DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderHooks.onRenderLevel(deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    // 1.21.5+: renderItemInHand(float, boolean, Matrix4f).
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void renderItemInHand$HEAD(float f, boolean bl, Matrix4f matrix4f, CallbackInfo ci) {
        if (QuadzClient.getQuadcopterFromCamera().isPresent()) {
            ci.cancel();
        }
    }

    // FPV post effects run where vanilla processes its own post chain: after the world (and
    // entity outlines), before the GUI pass — so the OSD stays sharp on top of fisheye/static.
    @Inject(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V",
                    shift = At.Shift.AFTER
            )
    )
    public void render$postEffects(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        QuadzClient.getQuadcopterFromCamera().ifPresent(quadcopter -> quadcopter.getView().onPostEffectRender());
    }

}
