package dev.lazurite.quadz.common.util;

import net.minecraft.world.item.ItemStack;

/**
 * Stores the bind id in the transmitter through the {@link Bindable} interface.
 * 1.20.5+: backed by the {@link QuadzComponents#BIND_ID} data component rather than a live, mutable
 * NBT sub-tag (components are immutable, so we hold the stack and set/get the component value).
 * @see Bindable
 */
public class BindableItemWrapper implements Bindable {

    private final ItemStack stack;

    public BindableItemWrapper(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void setBindId(int bindId) {
        stack.set(QuadzComponents.BIND_ID, bindId);
    }

    @Override
    public int getBindId() {
        return stack.getOrDefault(QuadzComponents.BIND_ID, -1);
    }

    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BindableItemWrapper) {
            return getBindId() == ((BindableItemWrapper) obj).getBindId();
        }

        return false;
    }

}
