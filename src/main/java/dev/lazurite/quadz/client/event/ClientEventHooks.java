package dev.lazurite.quadz.client.event;

import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.QuadzClient;
import dev.lazurite.quadz.common.util.JoystickOutput;
import dev.lazurite.quadz.client.Config;
import dev.lazurite.quadz.client.render.screen.ControllerConnectedToast;
import dev.lazurite.quadz.client.render.screen.osd.OnScreenDisplay;
import dev.lazurite.toolbox.api.network.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClientEventHooks {

    // Controller arm-switch tracking: last computed switch state and whether we were viewing a drone
    // last tick, so we only push SET_ARMED on a switch flip or on view entry (not every tick).
    private static Boolean armSwitchArmed = null;
    private static boolean prevViewing = false;

    public static void onPostLogin(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
        if (player != null) {
            applyRateConfig(player);
        }
    }

    /**
     * Pushes the rate-related config (profile + per-axis rate params) onto the player's synced
     * joystick values. Called at login and after the config screen saves, so rate/profile changes
     * take effect without relogging. The link-vs-per-axis decision is resolved here (client side):
     * when axes are linked, all three axes get the active profile's shared values; when unlinked,
     * each axis gets its own. The server tick then just reads per-axis keys, staying agnostic.
     */
    public static void applyRateConfig(LocalPlayer player) {
        if (player == null) {
            return;
        }

        final var p = Config.rateProfile.ordinal();
        player.quadz$setJoystickValue(rateId("rate_profile"), Config.rateProfile.ordinal());

        if (Config.linkAxes) {
            applyAxisRates(player, "pitch", Config.rates[p], Config.superRates[p], Config.expos[p]);
            applyAxisRates(player, "yaw",   Config.rates[p], Config.superRates[p], Config.expos[p]);
            applyAxisRates(player, "roll",  Config.rates[p], Config.superRates[p], Config.expos[p]);
        } else {
            applyAxisRates(player, "pitch", Config.pitchRates[p], Config.pitchSuperRates[p], Config.pitchExpos[p]);
            applyAxisRates(player, "yaw",   Config.yawRates[p],   Config.yawSuperRates[p],   Config.yawExpos[p]);
            applyAxisRates(player, "roll",  Config.rollRates[p],  Config.rollSuperRates[p],  Config.rollExpos[p]);
        }
    }

    private static void applyAxisRates(LocalPlayer player, String axis, float rate, float superRate, float expo) {
        player.quadz$setJoystickValue(rateId(axis + "_rate"), rate);
        player.quadz$setJoystickValue(rateId(axis + "_super_rate"), superRate);
        player.quadz$setJoystickValue(rateId(axis + "_expo"), expo);
    }

    private static Identifier rateId(String path) {
        return Identifier.fromNamespaceAndPath(Quadz.MODID, path);
    }

    public static void onClientTick(Minecraft minecraft) {
        if (minecraft.isPaused() || minecraft.player == null) {
            return;
        }

        // Advance the camera-angle readout's flash timer (used when its OSD toggle is off).
        OnScreenDisplay.tickFlash();

        if (JoystickOutput.controllerExists()) {
            JoystickOutput.getAxisValue(minecraft.player, Config.pitch, Identifier.fromNamespaceAndPath(Quadz.MODID, "pitch"), Config.pitchInverted, false);
            JoystickOutput.getAxisValue(minecraft.player, Config.yaw, Identifier.fromNamespaceAndPath(Quadz.MODID, "yaw"), Config.yawInverted, false);
            JoystickOutput.getAxisValue(minecraft.player, Config.roll, Identifier.fromNamespaceAndPath(Quadz.MODID, "roll"), Config.rollInverted, false);
            JoystickOutput.getAxisValue(minecraft.player, Config.throttle, Identifier.fromNamespaceAndPath(Quadz.MODID, "throttle"), Config.throttleInverted, Config.throttleInCenter);
        }

        // Camera uptilt: drain the keybind queue (so presses don't pile up) and, when viewing a
        // drone, ask the server to nudge that drone's uptilt by 1 degree per press.
        var delta = 0;
        while (QuadzClient.CAMERA_UP.consumeClick()) delta += 1;
        while (QuadzClient.CAMERA_DOWN.consumeClick()) delta -= 1;

        if (delta != 0 && QuadzClient.getQuadcopterFromCamera().isPresent()) {
            final var sent = delta;
            ClientNetworking.send(Quadz.Networking.ADJUST_CAMERA_ANGLE, buf -> buf.writeInt(sent));
            // Briefly surface the readout even when its toggle is off, so the pilot sees the value change.
            OnScreenDisplay.flashCameraAngle();
        }

        // Explicit arm/disarm (keyboard): each press toggles the viewed drone's armed state (override
        // on top of auto-arm). The server flips ARMED and syncs it back.
        var armToggles = 0;
        while (QuadzClient.ARM_DISARM.consumeClick()) armToggles++;

        if (armToggles % 2 != 0 && QuadzClient.getQuadcopterFromCamera().isPresent()) {
            ClientNetworking.send(Quadz.Networking.ARM_DISARM, buf -> {});
        }

        // Controller arm switch (direct position): authoritative when bound. We push the switch's
        // state on a flip or on view entry (overriding auto-arm), via the SET_ARMED packet.
        final var viewing = QuadzClient.getQuadcopterFromCamera().isPresent();
        if (Config.armAxis >= 0 && JoystickOutput.controllerExists()) {
            final var axes = JoystickOutput.getAllAxisValues();
            if (axes != null && Config.armAxis < axes.capacity()) {
                final var value = axes.get(Config.armAxis);
                // Default: "down" (negative axis) = armed, "up" = disarmed; armInverted flips it.
                final boolean desired = Config.armInverted ? value > 0.0f : value < 0.0f;
                final boolean entered = viewing && !prevViewing;
                final boolean flipped = armSwitchArmed == null || desired != armSwitchArmed;

                if (viewing && (entered || flipped)) {
                    ClientNetworking.send(Quadz.Networking.SET_ARMED, buf -> buf.writeBoolean(desired));
                }
                armSwitchArmed = desired;
            }
        }
        prevViewing = viewing;
    }

    public static void onJoystickConnect(int id, String name) {
        ControllerConnectedToast.add(Component.translatable("quadz.toast.connect"), name);
    }

    public static void onJoystickDisconnect(int id, String name) {
        ControllerConnectedToast.add(Component.translatable("quadz.toast.disconnect"), name);
    }

    public static void onLeftClick() {
        ClientNetworking.send(Quadz.Networking.REQUEST_QUADCOPTER_VIEW, buf -> buf.writeInt(-1));
    }

    public static void onRightClick() {
        ClientNetworking.send(Quadz.Networking.REQUEST_QUADCOPTER_VIEW, buf -> buf.writeInt(1));
    }

    public static void onClientLevelTick(ClientLevel level) {
        final var client = Minecraft.getInstance();

        if (!client.isPaused()) {
            client.player.quadz$syncJoystick();

            if (client.options.keyShift.isDown() && QuadzClient.getQuadcopterFromCamera().isPresent()) {
                client.options.getCameraType().quadz$reset();
            }
        }
    }

}
