package dev.lazurite.quadz.client.mixin;

import dev.lazurite.quadz.client.QuadzClient;
import dev.lazurite.quadz.client.render.RenderHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // 1.21: GameRenderer.renderLevel now takes a DeltaTracker (no PoseStack/tickDelta/nanoTime).
    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void renderLevel$HEAD(DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderHooks.onRenderLevel(deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    // 1.21: camera yaw/pitch is no longer applied via PoseStack.mulPose inside renderLevel (it moved
    // to the Matrix4f view pipeline). require=0 keeps these optional; the FPV camera orientation is
    // re-expressed against the new pipeline in the CP2b render sub-gate.
    @ModifyArg(
            method = "renderLevel",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    ordinal = 2
            )
    )
    public Quaternionf renderLevel$multiplyYaw(Quaternionf quaternion) {
        return RenderHooks.onMultiplyYaw(quaternion);
    }

    @ModifyArg(
            method = "renderLevel",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    ordinal = 3
            )
    )
    public Quaternionf renderLevel$multiplyPitch(Quaternionf quaternion) {
        return RenderHooks.onMultiplyPitch(quaternion);
    }

    // 1.21: renderItemInHand(Camera, float, Matrix4f) — no PoseStack.
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void renderItemInHand$HEAD(Camera camera, float f, Matrix4f matrix4f, CallbackInfo ci) {
        if (QuadzClient.getQuadcopterFromCamera().isPresent()) {
            ci.cancel();
        }
    }

}
