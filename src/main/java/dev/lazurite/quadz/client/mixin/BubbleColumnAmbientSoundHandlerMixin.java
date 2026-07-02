package dev.lazurite.quadz.client.mixin;

import dev.lazurite.quadz.client.QuadzClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Companion to {@link UnderwaterAmbientSoundHandlerMixin}: the bubble-column whirlpool/upwards
 * "inside" sounds are also driven by the player avatar — vanilla scans {@code player.getBoundingBox()}
 * for a bubble column and plays the sound at the player. While piloting, redirect both the scan box
 * and the sound emitter to the drone so the cue triggers from (and plays at) the drone camera, not the
 * pilot's body. No-op when not piloting.
 */
@Mixin(BubbleColumnAmbientSoundHandler.class)
public class BubbleColumnAmbientSoundHandlerMixin {

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getBoundingBox()Lnet/minecraft/world/phys/AABB;")
    )
    private AABB quadz$bubbleScanAtDrone(LocalPlayer player) {
        return QuadzClient.getQuadcopterFromCamera().map(q -> (Entity) q).orElse(player).getBoundingBox();
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V")
    )
    private void quadz$bubbleSoundAtDrone(LocalPlayer player, SoundEvent sound, float volume, float pitch) {
        QuadzClient.getQuadcopterFromCamera().map(q -> (Entity) q).orElse(player).playSound(sound, volume, pitch);
    }
}
