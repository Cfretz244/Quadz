package dev.lazurite.quadz.client.mixin;

import dev.lazurite.corduroy.api.ViewStack;
import dev.lazurite.quadz.client.QuadzClient;
import dev.lazurite.quadz.client.render.RenderHooks;
import dev.lazurite.quadz.common.entity.Quadcopter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    // 1.21.3: Minecraft no longer holds a profiler field; use the global Profiler scope.
    @Inject(method = "runTick", at = @At("TAIL"))
    public void runTick$render(boolean bl, CallbackInfo ci) {
        RenderHooks.onRenderMinecraft(Profiler.get());
    }

    @Inject(method = "setCameraEntity", at = @At("HEAD"))
    public void setCameraEntity(Entity entity, CallbackInfo ci) {
        if (QuadzClient.getQuadcopterFromCamera().isEmpty() && entity instanceof Quadcopter quadcopter) {
            ViewStack.getInstance().push(quadcopter.getView());
        } else {
            ViewStack.getInstance().pop();
        }
    }

}
