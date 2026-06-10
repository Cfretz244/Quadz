package dev.lazurite.quadz.client.render.screen;

import dev.lazurite.quadz.Quadz;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ControllerConnectedToast implements Toast {

    // 1.21: toasts no longer expose a TEXTURE constant; blit the vanilla toast background sprite.
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");

    private final Component message;
    private final String controllerName;

    public ControllerConnectedToast(Component message, String controllerName) {
        this.message = message;

        if (controllerName.length() > 25) {
            this.controllerName = controllerName.substring(0, 25) + "...";
        } else {
            this.controllerName = controllerName;
        }
    }

    // 1.21.2: Toast was reworked — visibility moves to getWantedVisibility()/update(), render gets
    // the Font directly, and blitSprite needs an explicit RenderType function.
    private Visibility wantedVisibility = Visibility.SHOW;

    @Override
    public Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager toastManager, long startTime) {
        this.wantedVisibility = startTime >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

    // 26.1: toasts extract render state instead of rendering.
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, Font font, long startTime) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width(), height());
        guiGraphics.text(font, message, 30, 7, -1, false);
        guiGraphics.text(font, Component.literal(controllerName), 30, 18, -1, false);
        guiGraphics.fakeItem(new ItemStack(Quadz.REMOTE_ITEM), 8, 8);
    }

    public static void add(Component message, String name) {
        var manager = Minecraft.getInstance().getToastManager();
        manager.addToast(new ControllerConnectedToast(message, name));
    }

}
