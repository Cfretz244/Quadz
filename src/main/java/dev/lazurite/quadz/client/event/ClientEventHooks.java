package dev.lazurite.quadz.client.event;

import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.QuadzClient;
import dev.lazurite.quadz.common.util.JoystickOutput;
import dev.lazurite.quadz.client.Config;
import dev.lazurite.quadz.client.render.screen.ControllerConnectedToast;
import dev.lazurite.toolbox.api.network.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClientEventHooks {

    public static void onPostLogin(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
        if (player != null) {
            player.quadz$setJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "rate"), Config.rate);
            player.quadz$setJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "super_rate"), Config.superRate);
            player.quadz$setJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "expo"), Config.expo);
        }
    }

    public static void onClientTick(Minecraft minecraft) {
        if (!minecraft.isPaused() && minecraft.player != null && JoystickOutput.controllerExists()) {
            JoystickOutput.getAxisValue(minecraft.player, Config.pitch, Identifier.fromNamespaceAndPath(Quadz.MODID, "pitch"), Config.pitchInverted, false);
            JoystickOutput.getAxisValue(minecraft.player, Config.yaw, Identifier.fromNamespaceAndPath(Quadz.MODID, "yaw"), Config.yawInverted, false);
            JoystickOutput.getAxisValue(minecraft.player, Config.roll, Identifier.fromNamespaceAndPath(Quadz.MODID, "roll"), Config.rollInverted, false);
            JoystickOutput.getAxisValue(minecraft.player, Config.throttle, Identifier.fromNamespaceAndPath(Quadz.MODID, "throttle"), Config.throttleInverted, Config.throttleInCenter);
        }
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
