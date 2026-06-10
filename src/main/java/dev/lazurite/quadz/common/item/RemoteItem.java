package dev.lazurite.quadz.common.item;

import dev.lazurite.quadz.Quadz;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Represents a held "Transmitter" or remote for controlling a quadcopter.
 * However, this class is not likely to contain much of the logic.
 */
public class RemoteItem extends Item {

    public RemoteItem() {
        // 1.21.4: items must carry their registry id in Properties (setId) at construction.
        super(new Properties().stacksTo(1)
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Quadz.MODID, "remote"))));
    }

}
