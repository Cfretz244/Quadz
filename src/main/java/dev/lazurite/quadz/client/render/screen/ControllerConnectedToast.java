package dev.lazurite.quadz.client.render.screen;

import dev.lazurite.quadz.Quadz;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ControllerConnectedToast implements Toast {

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

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long startTime) {
        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, width(), height());
        guiGraphics.drawString(toastComponent.getMinecraft().font, message, 30, 7, -1, false);
        guiGraphics.drawString(toastComponent.getMinecraft().font, Component.literal(controllerName), 30, 18, -1, false);
        guiGraphics.renderFakeItem(new ItemStack(Quadz.REMOTE_ITEM), 8, 8);

        return startTime >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

    public static void add(Component message, String name) {
        var manager = Minecraft.getInstance().getToasts();
        manager.addToast(new ControllerConnectedToast(message, name));
    }

}
