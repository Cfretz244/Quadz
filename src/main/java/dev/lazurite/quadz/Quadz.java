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
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
    public static final Item GOGGLES_ITEM = Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "goggles"), new GogglesItem());
    public static final Item QUADCOPTER_ITEM = Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "quadcopter"), new QuadcopterItem());
    public static final Item REMOTE_ITEM = Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "remote"), new RemoteItem());
    public static final ResourceKey<CreativeModeTab> CREATIVE_MODE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "quadz"));
    public static final CreativeModeTab CREATIVE_MODE_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_MODE_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(GOGGLES_ITEM))
                    .title(Component.literal("Quadz"))
                    .build());

    // Entities
    public static final EntityType<Quadcopter> QUADCOPTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "quadcopter"),
            // 26.1: FabricEntityTypeBuilder is gone; FabricEntityType.Builder wraps the vanilla builder.
            FabricEntityType.Builder.createLiving(Quadcopter::new, MobCategory.MISC,
                            living -> living.defaultAttributes(LivingEntity::createLivingAttributes))
                    .sized(0.5f, 0.2f)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "quadcopter"))));

    @Override
    public void onInitialize() {
        LOGGER.info("Goggles down, thumbs up!");

        QuadzComponents.init();

        // Events
        TemplateEvents.ENTITY_TEMPLATE_CHANGED.register(ServerEventHooks::onEntityTemplateChanged);
        TemplateEvents.TEMPLATE_LOADED.register(ServerEventHooks::onTemplateLoaded);
        CreativeModeTabEvents.modifyOutputEvent(CREATIVE_MODE_TAB_KEY).register(ServerEventHooks::onRebuildCreativeTab);

        // Network events
        PacketRegistry.registerServerbound(Networking.JOYSTICK_INPUT, ServerNetworkEventHooks::onJoystickInput);
        PacketRegistry.registerServerbound(Networking.REQUEST_QUADCOPTER_VIEW, ServerNetworkEventHooks::onQuadcopterViewRequested);
        PacketRegistry.registerServerbound(Networking.REQUEST_PLAYER_VIEW, ServerNetworkEventHooks::onPlayerViewRequestReceived);

        // Load templates
        TemplateLoader.initialize(MODID);
    }

    public static class Networking {

        public static final Identifier JOYSTICK_INPUT = Identifier.fromNamespaceAndPath(MODID, "joystick_input");
        public static final Identifier REQUEST_QUADCOPTER_VIEW = Identifier.fromNamespaceAndPath(MODID, "request_quadcopter_view");
        public static final Identifier REQUEST_PLAYER_VIEW = Identifier.fromNamespaceAndPath(MODID, "request_player_view");

    }

}
