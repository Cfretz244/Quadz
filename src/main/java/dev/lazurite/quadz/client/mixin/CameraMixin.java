package dev.lazurite.quadz.client.mixin;

import dev.lazurite.quadz.client.Config;
import dev.lazurite.quadz.client.QuadzClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    // Adjustable FPV field of view. While viewing through a quadcopter, override the camera FOV
    // with the user's configured value (Config.fpvFov, 90-160). In 26.1 the ~110 FOV cap lives in
    // the Options.fov() IntRange, and Camera#calculateFov just reads that already-capped integer;
    // overriding the *result* here is what lets the drone view exceed 110 to mimic a real FPV cam.
    // FOV is a purely client-side render concern (not spectator-synced), so no packet is needed —
    // we only gate on getQuadcopterFromCamera(), so line-of-sight / normal gameplay FOV is untouched.
    // Returned raw (no fovModifier lerp / death-or-fluid modifier) so the FPV cam FOV stays fixed,
    // which is how a real FPV camera behaves.
    @Inject(method = "calculateFov", at = @At("HEAD"), cancellable = true)
    private void quadz$overrideFpvFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (QuadzClient.getQuadcopterFromCamera().isPresent()) {
            cir.setReturnValue((float) Config.fpvFov);
        }
    }
}
