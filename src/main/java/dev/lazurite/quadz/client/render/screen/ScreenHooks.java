package dev.lazurite.quadz.client.render.screen;

import dev.lazurite.quadz.Quadz;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class ScreenHooks {

    public static void addQuadzButtonToPauseScreen(PauseScreen screen) {
        screen.addRenderableWidget(getButton(
                screen,
                screen.disconnectButton.getX() + screen.disconnectButton.getWidth() + 5,
                screen.disconnectButton.getY()
        ));
    }

    public static void addQuadzButtonToTitleScreen(TitleScreen screen) {
        screen.addRenderableWidget(getButton(
                screen,
                screen.width / 2 + 128,
                screen.height / 4 + 132)
        );
    }

    // 1.21: ImageButton's constructor switched to the WidgetSprites system. The Quadz icon texture
    // isn't set up as a GUI sprite, so use a plain text Button (this entry point is dev-unreachable
    // without Mod Menu and purely opens the controller-setup screen).
    private static Button getButton(Screen parent, int x, int y) {
        return Button.builder(
                        Component.translatable("quadz.config.title"),
                        button -> Minecraft.getInstance().setScreen(new ControllerSetupScreen(parent)))
                .bounds(x, y, 20, 20)
                .build();
    }

}
