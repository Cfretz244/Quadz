package dev.lazurite.quadz.client.mixin;

import dev.lazurite.quadz.client.QuadzClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Completes moving the ambient soundscape to the drone's POV (alongside the underwater + bubble-column
 * mixins). Vanilla's biome/cave "mood" and biome loop/additions ambience is sampled and positioned at
 * the player avatar ({@code this.player} getX/getEyeY/getZ/position). While piloting, redirect those
 * position reads to the drone so the biome the drone is in — and where its mood/one-shot sounds emit —
 * follows the camera, not the pilot's body. Level is unchanged (drone shares the pilot's level), so the
 * biome is simply sampled at the drone's coordinates. No-op when not piloting.
 */
@Mixin(BiomeAmbientSoundsHandler.class)
public class BiomeAmbientSoundsHandlerMixin {

    private Entity quadz$soundSource(LocalPlayer player) {
        return QuadzClient.getQuadcopterFromCamera().map(q -> (Entity) q).orElse(player);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double quadz$biomeX(LocalPlayer player) {
        return quadz$soundSource(player).getX();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEyeY()D"))
    private double quadz$biomeEyeY(LocalPlayer player) {
        return quadz$soundSource(player).getEyeY();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double quadz$biomeZ(LocalPlayer player) {
        return quadz$soundSource(player).getZ();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 quadz$biomePosition(LocalPlayer player) {
        return quadz$soundSource(player).position();
    }
}
