package dev.lazurite.quadz.client.render.screen.osd;

import com.jme3.math.Vector3f;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.Config;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.quadz.common.util.Search;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class OnScreenDisplay {

    // When a readout is toggled off it still flashes briefly while its value is being adjusted,
    // fading out over the tail of the window. Timers are in client ticks (20/sec). The camera-angle
    // and FOV readouts flash independently, since they respond to different keys/events.
    private static final int FLASH_DURATION = 40;    // how long a readout stays up after a change
    private static final int FLASH_FADE_TICKS = 12;  // how long it spends fading out at the end
    private static int flashTicks = 0;
    private static int fovFlashTicks = 0;

    private final Quadcopter quadcopter;
    private final Font font;

    /** Show the camera-angle readout briefly (used when its toggle is off and the angle changes). */
    public static void flashCameraAngle() {
        flashTicks = FLASH_DURATION;
    }

    /** Show the FOV readout briefly (used when its toggle is off and the FOV changes). */
    public static void flashFov() {
        fovFlashTicks = FLASH_DURATION;
    }

    /** Decrement the flash timers; call once per client tick. */
    public static void tickFlash() {
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (fovFlashTicks > 0) {
            fovFlashTicks--;
        }
    }

    public static boolean isCameraAngleFlashing() {
        return flashTicks > 0;
    }

    public static boolean isFovFlashing() {
        return fovFlashTicks > 0;
    }

    public OnScreenDisplay(Quadcopter quadcopter) {
        this.quadcopter = quadcopter;
        this.font = Minecraft.getInstance().font;
    }

    public void renderVelocity(GuiGraphicsExtractor guiGraphics, float tickDelta) {
        var client = Minecraft.getInstance();
        var height = client.getWindow().getGuiScaledHeight() - 25;
        var unit = Config.velocityUnit;
        final var vel = Math.round(quadcopter.getRigidBody().getLinearVelocity(new Vector3f()).length() * unit.getFactor() * 10) / 10f;
        final var velocity = Component.literal(vel + " " + unit.getAbbreviation());
        guiGraphics.text(font, velocity, 25, height, 0xFFFFFFFF, true);
    }

    public void renderCameraAngle(GuiGraphicsExtractor guiGraphics, float tickDelta) {
        var client = Minecraft.getInstance();
        var height = client.getWindow().getGuiScaledHeight() - 37;
        final var angle = quadcopter.getEntityData().get(Quadcopter.CAMERA_ANGLE);
        final var text = Component.literal(angle + "°");

        // Full opacity when the readout is showing persistently (master OSD on + its toggle on);
        // otherwise it's only up because of an adjustment flash, so fade it out over the tail of
        // the flash window. The flash is intentionally independent of the master OSD switch.
        final boolean persistent = Config.osdEnabled && Config.cameraAngleDisplayEnabled;
        final int alpha = persistent
                ? 0xFF
                : (int) (0xFF * Math.min(1.0f, flashTicks / (float) FLASH_FADE_TICKS));
        if (alpha <= 0) {
            return;
        }

        guiGraphics.text(font, text, 25, height, (alpha << 24) | 0xFFFFFF, true);
    }

    public void renderFov(GuiGraphicsExtractor guiGraphics, float tickDelta) {
        var client = Minecraft.getInstance();
        // Sits one line above the camera-angle readout (which is at height-37).
        var height = client.getWindow().getGuiScaledHeight() - 49;
        // Labeled ("FOV 120°") so it reads distinctly from the bare uptilt-angle value below it.
        final var text = Component.translatable("quadz.osd.fov", Config.fpvFov);

        // Same persistent-vs-flash logic as the camera-angle readout: full opacity when its toggle
        // is on (and master OSD on), otherwise fade out over the tail of its own flash window.
        final boolean persistent = Config.osdEnabled && Config.fovDisplayEnabled;
        final int alpha = persistent
                ? 0xFF
                : (int) (0xFF * Math.min(1.0f, fovFlashTicks / (float) FLASH_FADE_TICKS));
        if (alpha <= 0) {
            return;
        }

        guiGraphics.text(font, text, 25, height, (alpha << 24) | 0xFFFFFF, true);
    }

    /** Centered "DISARMED" warning, shown whenever the viewed drone's motors are off. */
    public void renderDisarmed(GuiGraphicsExtractor guiGraphics, float tickDelta) {
        if (quadcopter.isArmed()) {
            return;
        }
        var client = Minecraft.getInstance();
        final var text = Component.translatable("quadz.osd.disarmed");
        final var x = (client.getWindow().getGuiScaledWidth() - font.width(text)) / 2;
        final var y = client.getWindow().getGuiScaledHeight() / 3;
        guiGraphics.text(font, text, x, y, 0xFFFF5555, true);
    }

    public void renderSticks(GuiGraphicsExtractor guiGraphics, float tickDelta) {
        Search.forPlayer(quadcopter).ifPresent(player -> {
            var pitch = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "pitch"));
            var yaw = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "yaw"));
            var roll = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "roll"));
            var throttle = (player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "throttle")) + 1.0f);
            var width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            var height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            var scale = Math.max(1, Math.round(25 * Config.stickScale));
            // Gimbal separation and bottom margin scale proportionally with the gimbal size.
            var offset = Math.round(scale * 2.5f);
            var bottomMargin = Math.round(scale * 2.5f);
            renderSticks(guiGraphics, tickDelta, width / 2, height - bottomMargin, scale, offset, pitch, yaw, roll, throttle);
        });
    }

    /**
     * @param offset half the horizontal distance between the two stick gimbals (distance of each
     *               gimbal's centre from {@code x}). Kept independent of {@code scale} so the two
     *               sticks stay a fixed distance apart regardless of gimbal size.
     */
    public static void renderSticks(GuiGraphicsExtractor guiGraphics, float tickDelta, int x, int y, int scale, int offset, float pitch, float yaw, float roll, float throttle) {
        var leftX = x - offset;
        var rightX = x + offset;
        var topY = y + scale;
        var bottomY = y - scale;

        // Draw crosses
        guiGraphics.fill(leftX, bottomY + 1, leftX + 1, topY, 0xFFFFFFFF);
        guiGraphics.fill(rightX, bottomY + 1, rightX + 1, topY, 0xFFFFFFFF);
        guiGraphics.fill(leftX - scale, y, leftX + scale + 1, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(rightX - scale, y, rightX + scale + 1, y + 1, 0xFFFFFFFF);

        // Draw stick positions. Dot size scales with the gimbal size so it keeps the same
        // relative width at any overlay scale (was a fixed 2px, which looked huge when shrunk).
        int dotSize = Math.max(1, Math.round(scale * 0.08f));
        int yawAdjusted = (int) (yaw * scale);
        int throttleAdjusted = (int) (throttle * scale) - scale;
        int rollAdjusted = (int) (roll * scale);
        int pitchAdjusted = (int) (pitch * scale);
        guiGraphics.fill(leftX + yawAdjusted - dotSize, y - throttleAdjusted - dotSize, leftX + yawAdjusted + dotSize, y - throttleAdjusted + dotSize, 0xFFFFFFFF);
        guiGraphics.fill(rightX + rollAdjusted - dotSize, y - pitchAdjusted - dotSize, rightX + rollAdjusted + dotSize, y - pitchAdjusted + dotSize, 0xFFFFFFFF);
    }

}
