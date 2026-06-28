package dev.lazurite.quadz.client;

import dev.lazurite.corduroy.api.ViewStack;
import dev.lazurite.form.api.Templated;
import dev.lazurite.form.api.render.FormRegistry;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.render.QuadcopterView;
import dev.lazurite.quadz.client.render.entity.QuadcopterEntityRenderer;
import dev.lazurite.quadz.common.util.Bindable;
import dev.lazurite.quadz.common.util.Search;
import dev.lazurite.quadz.common.util.event.ClickEvents;
import dev.lazurite.quadz.common.util.event.JoystickEvents;
import dev.lazurite.quadz.client.event.ClientEventHooks;
import dev.lazurite.quadz.client.event.ClientNetworkEventHooks;
import dev.lazurite.quadz.client.resource.SplashResourceLoader;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.toolbox.api.event.ClientEvents;
import dev.lazurite.toolbox.api.network.PacketRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class QuadzClient implements ClientModInitializer {

    // Adjust the viewed drone's camera uptilt while in FPV (rebindable; default arrow keys).
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Quadz.MODID, "quadz"));
    public static final KeyMapping CAMERA_UP = new KeyMapping(
            "quadz.key.camera_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, KEY_CATEGORY);
    public static final KeyMapping CAMERA_DOWN = new KeyMapping(
            "quadz.key.camera_down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, KEY_CATEGORY);

    /**
     * Finds the player's quadcopter based on its camera view.
     */
    public static Optional<Quadcopter> getQuadcopterFromCamera() {
        return ViewStack.getInstance()
                .peek()
                .filter(view -> view instanceof QuadcopterView)
                .map(view -> ((QuadcopterView) view).getQuadcopter());
    }

    /**
     * Finds the player's quadcopter based on its held remote.
     * <p>
     * Only works if it's within a 256 block radius.
     */
    public static Optional<Quadcopter> getQuadcopterFromRemote() {
        var player = Minecraft.getInstance().player;

        if (player != null) {
            return Bindable.get(player.getMainHandItem()).flatMap(remote -> Search.forQuadWithBindId(player.level(), player.position(), remote.getBindId(), 256));
        }

        return Optional.empty();
    }

    @Override
    public void onInitializeClient() {
        Config.load();

        // Keybinds
        KeyMappingHelper.registerKeyMapping(CAMERA_UP);
        KeyMappingHelper.registerKeyMapping(CAMERA_DOWN);

        // Renderer
        EntityRendererRegistry.register(Quadz.QUADCOPTER, QuadcopterEntityRenderer::new);
        FormRegistry.register((Templated.Item) Quadz.QUADCOPTER_ITEM);

        // FPV on-screen display — 1.21.6 deferred GUI pipeline: registered as a HUD element
        // (replaces the old Gui.render TAIL mixin); post effects run from the GameRenderer hook.
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Quadz.MODID, "osd"), (guiGraphics, deltaTracker) ->
                getQuadcopterFromCamera().ifPresent(quadcopter ->
                        quadcopter.getView().onGuiRender(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))));

        // Splash screen text injection
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SplashResourceLoader());

        // Events
        ClientEvents.Lifecycle.POST_LOGIN.register(ClientEventHooks::onPostLogin);
        ClientEvents.Tick.START_CLIENT_TICK.register(ClientEventHooks::onClientTick);
        ClientEvents.Tick.START_LEVEL_TICK.register(ClientEventHooks::onClientLevelTick);
        JoystickEvents.JOYSTICK_CONNECT.register(ClientEventHooks::onJoystickConnect);
        JoystickEvents.JOYSTICK_DISCONNECT.register(ClientEventHooks::onJoystickDisconnect);
        ClickEvents.LEFT_CLICK_EVENT.register(ClientEventHooks::onLeftClick);
        ClickEvents.RIGHT_CLICK_EVENT.register(ClientEventHooks::onRightClick);

        // Network events
        PacketRegistry.registerClientbound(Quadz.Networking.JOYSTICK_INPUT, ClientNetworkEventHooks::onJoystickInput);
    }

}
