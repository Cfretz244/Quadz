package dev.lazurite.quadz.client.render;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.lazurite.corduroy.api.View;
import dev.lazurite.form.api.loader.TemplateLoader;
import dev.lazurite.form.impl.common.template.model.Template;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.Config;
import dev.lazurite.quadz.client.render.screen.osd.OnScreenDisplay;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.rayon.impl.bullet.thread.util.Clock;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import dev.lazurite.toolbox.api.math.VectorHelper;
import dev.lazurite.quadz.client.render.shader.PostEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class QuadcopterView extends View implements View.Ticking {

    private static final int SIGNAL_DISTANCE = 1024;

    private final Quadcopter quadcopter;
    private final OnScreenDisplay osd;
    private final Clock clock;
    private Template template;

    public QuadcopterView(Quadcopter quadcopter) {
        this.quadcopter = quadcopter;
        this.osd = new OnScreenDisplay(quadcopter);
        this.clock = new Clock();
    }

    @Override
    public void tick() {
        PostEffects.fisheyeAmount = Config.fisheyeAmount;

        var distance = getQuadcopter().distanceTo(Minecraft.getInstance().player);
        var lineOfSight = getQuadcopter().hasLineOfSight(Minecraft.getInstance().player);
        PostEffects.staticAmount = lineOfSight ? Mth.clamp(distance / (float) SIGNAL_DISTANCE, 0.0f, 1.0f) * 2.0f : 2.0f;
    }

    @Override
    public void onRender() {
        var options = Minecraft.getInstance().options;
        var camera = this.getCamera();

        if (options.getCameraType().isFirstPerson()) {
            if (!Config.renderCameraInCenter) {
                var template = this.getTemplate();
                float cameraX = template.metadata().get("cameraX").getAsFloat();
                float cameraY = template.metadata().get("cameraY").getAsFloat();
                camera.move(-cameraX, -cameraY, 0);
            }
        } else {
            camera.move(camera.getMaxZoom(4), 0, 0);
        }
    }

    public void onGuiRender(GuiGraphicsExtractor guiGraphics, float tickDelta) {
        if (Config.osdEnabled) {
            if (Config.speedDisplayEnabled) {
                this.osd.renderVelocity(guiGraphics, tickDelta);
            }

            if (Config.stickDisplayEnabled) {
                this.osd.renderSticks(guiGraphics, tickDelta);
            }
        }

        // Camera-angle readout: shows persistently when enabled (and the master OSD is on), and
        // ALSO flashes briefly while the angle is being adjusted — independent of the master OSD,
        // so the value is always visible the moment you change it.
        if ((Config.osdEnabled && Config.cameraAngleDisplayEnabled) || OnScreenDisplay.isCameraAngleFlashing()) {
            this.osd.renderCameraAngle(guiGraphics, tickDelta);
        }

        // FOV readout: same rules as the camera-angle one — persistent when enabled (+ master OSD),
        // and flashes briefly on adjustment regardless, so the value shows the moment you change it.
        if ((Config.osdEnabled && Config.fovDisplayEnabled) || OnScreenDisplay.isFovFlashing()) {
            this.osd.renderFov(guiGraphics, tickDelta);
        }

        // Disarmed warning follows the master OSD toggle (per tester request).
        if (Config.osdEnabled) {
            this.osd.renderDisarmed(guiGraphics, tickDelta);
        }
    }

    /**
     * Called from the GameRenderer post-effect hook each frame (1.21.5+ pipeline) — the shader
     * passes no longer run inside the GUI render.
     */
    public void onPostEffectRender() {
        var firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();

        PostEffects.fisheyeEnabled = firstPerson && Config.fisheyeEnabled;
        PostEffects.staticEnabled = firstPerson && Config.videoInterferenceEnabled;
        PostEffects.staticTime = clock.get();
        PostEffects.render();
    }

    @Override
    public Vec3 getPosition(float tickDelta) {
        return VectorHelper.toVec3(Convert.toMinecraft(this.quadcopter.getPhysicsLocation(new Vector3f(), tickDelta)));
    }

    @Override
    public Quaternionf getRotation(float tickDelta) {
        return QuaternionHelper.rotateY(QuaternionHelper.rotateX(
                Convert.toMinecraft(this.quadcopter.getPhysicsRotation(new Quaternion(), tickDelta)),
                -this.quadcopter.getEntityData().get(Quadcopter.CAMERA_ANGLE)
        ), 180);
    }

    @Override
    public boolean shouldRenderTarget() {
        return (!Config.renderCameraInCenter && Config.renderFirstPerson) || !Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public Quadcopter getQuadcopter() {
        return this.quadcopter;
    }

    private Template getTemplate() {
        // cache the template object
        if (this.template == null) {
            this.template = TemplateLoader.getTemplateById(this.quadcopter.getTemplate()).orElse(null);
        }

        return this.template;
    }

}
