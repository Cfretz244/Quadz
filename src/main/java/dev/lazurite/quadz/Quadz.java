package dev.lazurite.quadz;

import dev.lazurite.form.api.event.TemplateEvents;
import dev.lazurite.form.api.loader.TemplateLoader;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.quadz.common.hooks.ServerEventHooks;
import dev.lazurite.quadz.common.hooks.ServerNetworkEventHooks;
import dev.lazurite.quadz.common.item.GogglesItem;
import dev.lazurite.quadz.common.item.QuadcopterItem;
import dev.lazurite.quadz.common.item.RemoteItem;
import dev.lazurite.quadz.common.util.QuadzComponents;
import dev.lazurite.toolbox.api.network.PacketRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Quadz implements ModInitializer {

    public static final String MODID = "quadz";
    public static final Logger LOGGER = LogManager.getLogger("Quadz");

    // Items
    public static final Item GOGGLES_ITEM = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "goggles"), new GogglesItem());
    public static final Item QUADCOPTER_ITEM = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "quadcopter"), new QuadcopterItem());
    public static final Item REMOTE_ITEM = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "remote"), new RemoteItem());
    public static final ResourceKey<CreativeModeTab> CREATIVE_MODE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MODID, "quadz"));
    public static final CreativeModeTab CREATIVE_MODE_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_MODE_TAB_KEY,
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(GOGGLES_ITEM))
                    .title(Component.literal("Quadz"))
                    .build());

    // Entities
    public static final EntityType<Quadcopter> QUADCOPTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "quadcopter"),
            FabricEntityTypeBuilder.createLiving()
                    .entityFactory(Quadcopter::new)
                    .spawnGroup(MobCategory.MISC)
                    .dimensions(EntityDimensions.scalable(0.5f, 0.2f))
                    .defaultAttributes(LivingEntity::createLivingAttributes)
                    // 1.21.4: entity types must be built with their registry key.
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "quadcopter"))));

    @Override
    public void onInitialize() {
        LOGGER.info("Goggles down, thumbs up!");

        QuadzComponents.init();

        // Events
        TemplateEvents.ENTITY_TEMPLATE_CHANGED.register(ServerEventHooks::onEntityTemplateChanged);
        TemplateEvents.TEMPLATE_LOADED.register(ServerEventHooks::onTemplateLoaded);
        ItemGroupEvents.modifyEntriesEvent(CREATIVE_MODE_TAB_KEY).register(ServerEventHooks::onRebuildCreativeTab);

        // Network events
        PacketRegistry.registerServerbound(Networking.JOYSTICK_INPUT, ServerNetworkEventHooks::onJoystickInput);
        PacketRegistry.registerServerbound(Networking.REQUEST_QUADCOPTER_VIEW, ServerNetworkEventHooks::onQuadcopterViewRequested);
        PacketRegistry.registerServerbound(Networking.REQUEST_PLAYER_VIEW, ServerNetworkEventHooks::onPlayerViewRequestReceived);

        // Load templates
        TemplateLoader.initialize(MODID);
    }

    public static class Networking {

        public static final ResourceLocation JOYSTICK_INPUT = ResourceLocation.fromNamespaceAndPath(MODID, "joystick_input");
        public static final ResourceLocation REQUEST_QUADCOPTER_VIEW = ResourceLocation.fromNamespaceAndPath(MODID, "request_quadcopter_view");
        public static final ResourceLocation REQUEST_PLAYER_VIEW = ResourceLocation.fromNamespaceAndPath(MODID, "request_player_view");

    }

}
