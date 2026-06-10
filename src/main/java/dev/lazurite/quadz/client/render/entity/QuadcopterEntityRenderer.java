package dev.lazurite.quadz.client.render.entity;

import com.jme3.math.Quaternion;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.lazurite.form.impl.client.render.TemplatedEntityRenderer;
import dev.lazurite.form.impl.client.render.TemplatedEntityRenderState;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Quaternionf;
import com.geckolib.constant.dataticket.DataTicket;

public class QuadcopterEntityRenderer extends TemplatedEntityRenderer<Quadcopter> {

    // GeckoLib 5: the render pass no longer sees the entity, so the interpolated physics
    // rotation and model offset are carried through the render state.
    private static final DataTicket<Quaternionf> PHYSICS_ROTATION = DataTicket.create("quadz_physics_rotation", Quaternionf.class);
    private static final DataTicket<Float> HALF_HEIGHT = DataTicket.create("quadz_half_height", Float.class);

    public QuadcopterEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    // GeckoLib 5.5: the hook carries the partial tick directly.
    @Override
    public void addRenderData(Quadcopter quadcopter, Void relatedObject, TemplatedEntityRenderState renderState, float partialTick) {
        super.addRenderData(quadcopter, relatedObject, renderState, partialTick);

        renderState.addGeckolibData(PHYSICS_ROTATION, Convert.toMinecraft(quadcopter.getPhysicsRotation(new Quaternion(), partialTick)));
        renderState.addGeckolibData(HALF_HEIGHT, (float) (quadcopter.getBoundingBox().getYsize() / 2));

        // Replaces the old yBodyRot=0 hack: the model's orientation comes purely from physics.
        renderState.bodyRot = 0;
        renderState.yRot = 0;
    }

    // 26.1: entity rendering is submit-based.
    @Override
    public void submit(TemplatedEntityRenderState renderState, PoseStack stack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        stack.pushPose();
        stack.mulPose(renderState.getOrDefaultGeckolibData(PHYSICS_ROTATION, new Quaternionf()));
        stack.translate(0, -renderState.getOrDefaultGeckolibData(HALF_HEIGHT, 0.0f), 0);
        super.submit(renderState, stack, collector, cameraRenderState);
        stack.popPose();
    }

    // Replaces the removed Entity.noCulling flag (set in the Quadcopter constructor pre-1.21.2).
    @Override
    protected boolean affectedByCulling(Quadcopter quadcopter) {
        return false;
    }

}
