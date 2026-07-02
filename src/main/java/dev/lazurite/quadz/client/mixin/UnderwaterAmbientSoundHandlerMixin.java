package dev.lazurite.quadz.client.mixin;

import dev.lazurite.quadz.client.QuadzClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * While piloting an FPV drone the audio listener sits at the drone camera (SoundEngine drives it from
 * Camera#position), but vanilla's underwater ambient loop is gated on the <em>player avatar's</em>
 * submersion ({@code this.player.isUnderWater()}). That's why the underwater ambience tracked the
 * pilot's body instead of the drone. Redirect the gate to the drone so the ambience starts when the
 * drone dives and stops when it surfaces. The loop is a listener-relative sound, so this on/off gate
 * is the only thing that matters. No-op (returns the player's own state) when not piloting.
 */
@Mixin(UnderwaterAmbientSoundHandler.class)
public class UnderwaterAmbientSoundHandlerMixin {

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z")
    )
    private boolean quadz$underwaterFollowsDrone(LocalPlayer player) {
        return QuadzClient.getQuadcopterFromCamera().map(q -> (Entity) q).orElse(player).isUnderWater();
    }
}
