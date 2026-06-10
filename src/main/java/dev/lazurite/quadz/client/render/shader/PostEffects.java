package dev.lazurite.quadz.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.mixin.PostChainAccessor;
import dev.lazurite.quadz.client.mixin.PostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryStack;

/**
 * Replaces Satin's ManagedShaderEffect on the 1.21.5+ pipeline (Satin's last release targets
 * 1.21.4). Chains load through the vanilla ShaderManager (assets/quadz/post_effect/<id>.json);
 * per-frame uniforms work by swapping each pass's baked immutable std140 UBO for a writable
 * {@link GpuBuffer} that we re-write before processing. PostPass.close() owns whatever buffer
 * sits in its customUniforms map, so replaced buffers stay leak-free across resource reloads.
 *
 * Rendered from GameRendererMixin at the same point vanilla runs its own post chain (after
 * doEntityOutline, before the GUI pass) — so the OSD stays sharp on top of the effects.
 */
public final class PostEffects {

    private static final ResourceLocation STATIC_ID = ResourceLocation.fromNamespaceAndPath(Quadz.MODID, "static");
    private static final ResourceLocation FISHEYE_ID = ResourceLocation.fromNamespaceAndPath(Quadz.MODID, "fisheye");
    private static final String STATIC_BLOCK = "StaticConfig";
    private static final String FISHEYE_BLOCK = "FisheyeConfig";
    private static final int BUFFER_USAGE = GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST;

    public static boolean staticEnabled;
    public static boolean fisheyeEnabled;
    public static float staticAmount;
    public static float staticTime;
    public static float fisheyeAmount;

    private static PostChain staticChain;
    private static PostChain fisheyeChain;
    private static GpuBuffer staticBuffer;
    private static GpuBuffer fisheyeBuffer;

    private PostEffects() {
    }

    public static void render() {
        final var minecraft = Minecraft.getInstance();

        if (fisheyeEnabled) {
            final var chain = minecraft.getShaderManager().getPostChain(FISHEYE_ID, LevelTargetBundle.MAIN_TARGETS);

            if (chain != null) {
                if (chain != fisheyeChain) {
                    fisheyeChain = chain;
                    fisheyeBuffer = swapUniformBuffer(chain, FISHEYE_BLOCK, "quadz fisheye config");
                }

                if (fisheyeBuffer != null) {
                    writeFloats(fisheyeBuffer, fisheyeAmount);
                    chain.process(minecraft.getMainRenderTarget(), GraphicsResourceAllocator.UNPOOLED);
                }
            }
        }

        if (staticEnabled) {
            final var chain = minecraft.getShaderManager().getPostChain(STATIC_ID, LevelTargetBundle.MAIN_TARGETS);

            if (chain != null) {
                if (chain != staticChain) {
                    staticChain = chain;
                    staticBuffer = swapUniformBuffer(chain, STATIC_BLOCK, "quadz static config");
                }

                if (staticBuffer != null) {
                    writeFloats(staticBuffer, staticAmount, staticTime);
                    chain.process(minecraft.getMainRenderTarget(), GraphicsResourceAllocator.UNPOOLED);
                }
            }
        }
    }

    /**
     * Replaces the baked (immutable) uniform-block buffer of the pass declaring {@code blockName}
     * with a writable one. Returns null if no pass declares the block (e.g. broken shader).
     */
    private static GpuBuffer swapUniformBuffer(PostChain chain, String blockName, String label) {
        for (final var pass : ((PostChainAccessor) chain).getPasses()) {
            final var uniforms = ((PostPassAccessor) pass).getCustomUniforms();
            final var baked = uniforms.get(blockName);

            if (baked != null) {
                final var buffer = RenderSystem.getDevice().createBuffer(() -> label, BUFFER_USAGE, baked.size());
                uniforms.put(blockName, buffer);
                baked.close();
                return buffer;
            }
        }

        Quadz.LOGGER.warn("Post effect uniform block {} not found; effect disabled", blockName);
        return null;
    }

    private static void writeFloats(GpuBuffer buffer, float... values) {
        try (final var stack = MemoryStack.stackPush()) {
            final var builder = Std140Builder.onStack(stack, buffer.size());

            for (final var value : values) {
                builder.putFloat(value);
            }

            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), builder.get());
        }
    }
}
