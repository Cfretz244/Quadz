package dev.lazurite.quadz.client.mixin;

import dev.lazurite.quadz.client.QuadzClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Completes moving the ambient soundscape to the drone's POV (with the underwater + bubble-column
 * mixins). Vanilla's biome/cave ambience is driven by the player avatar's position. Two parts:
 *
 * <ol>
 *   <li><b>Biome selection + loop sounds</b> read {@code this.player.position()} in {@code tick()} —
 *       a normal, stable target. Redirected to the drone so the ambience matches the biome the drone
 *       is flying through. This is the important half and applies with the default (required) hook.</li>
 *   <li><b>The mood one-shot</b> (the occasional cave "creepy" sound) positions itself off
 *       {@code getX/getEyeY/getZ} inside the synthetic lambda {@code lambda$tick$1}. Targeting a
 *       compiler-generated lambda is toolchain-fragile (its runtime name depends on remapping), so
 *       these redirects are {@code require = 0}: if the hook can't bind, the mood sound simply keeps
 *       emitting from the avatar (harmless) instead of failing to apply and crashing world load — the
 *       exact failure mode that took down the first attempt at this feature.</li>
 * </ol>
 *
 * All redirects are no-ops when not piloting.
 */
@Mixin(BiomeAmbientSoundsHandler.class)
public class BiomeAmbientSoundsHandlerMixin {

    @Unique
    private Entity quadz$soundSource(LocalPlayer player) {
        return QuadzClient.getQuadcopterFromCamera().map(q -> (Entity) q).orElse(player);
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 quadz$biomeSampleAtDrone(LocalPlayer player) {
        return quadz$soundSource(player).position();
    }

    @Redirect(
            method = "lambda$tick$1",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D")
    )
    private double quadz$moodX(LocalPlayer player) {
        return quadz$soundSource(player).getX();
    }

    @Redirect(
            method = "lambda$tick$1",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEyeY()D")
    )
    private double quadz$moodEyeY(LocalPlayer player) {
        return quadz$soundSource(player).getEyeY();
    }

    @Redirect(
            method = "lambda$tick$1",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D")
    )
    private double quadz$moodZ(LocalPlayer player) {
        return quadz$soundSource(player).getZ();
    }
}
