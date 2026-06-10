package dev.lazurite.quadz.common.util;

import com.mojang.serialization.Codec;
import dev.lazurite.quadz.Quadz;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/**
 * 1.20.5+ data components replacing the old {@code stack.bindable} NBT sub-tag.
 * {@code persistent} keeps the bind id across save/load; {@code networkSynchronized} ships it to the
 * client so a transmitter knows which drone it is bound to.
 */
public final class QuadzComponents {
    public static final DataComponentType<Integer> BIND_ID = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Quadz.MODID, "bind_id"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Touch the class so the static field registers during mod init. */
    public static void init() { }

    private QuadzComponents() { }
}
