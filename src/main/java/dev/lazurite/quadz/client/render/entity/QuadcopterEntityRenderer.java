package dev.lazurite.quadz.client.render.entity;

import com.jme3.math.Quaternion;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.lazurite.form.impl.client.render.TemplatedEntityRenderer;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class QuadcopterEntityRenderer extends TemplatedEntityRenderer<Quadcopter> {

    public QuadcopterEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    // 1.21.2: render is state-based; the live entity is the `animatable` field and the frame's
    // partial tick is captured by GeoEntityRenderer.
    @Override
    public void render(EntityRenderState state, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        final var quadcopterEntity = this.animatable;

        if (quadcopterEntity == null) {
            return;
        }

        float temp = quadcopterEntity.yBodyRot;
        quadcopterEntity.yBodyRot = 0;
        quadcopterEntity.yBodyRotO = 0;

        stack.pushPose();
        stack.mulPose(Convert.toMinecraft(quadcopterEntity.getPhysicsRotation(new Quaternion(), this.partialTick)));
        stack.translate(0, -quadcopterEntity.getBoundingBox().getYsize() / 2, 0);
        super.render(state, stack, bufferIn, packedLightIn);
        stack.popPose();

        quadcopterEntity.yBodyRot = temp;
    }

    // Replaces the removed Entity.noCulling flag (set in the Quadcopter constructor pre-1.21.2).
    @Override
    protected boolean affectedByCulling(Quadcopter quadcopter) {
        return false;
    }

}
