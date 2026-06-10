package dev.lazurite.quadz.common.hooks;

import dev.lazurite.form.api.loader.TemplateLoader;
import dev.lazurite.form.impl.common.template.model.Template;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class ServerEventHooks {

    public static void onEntityTemplateChanged(Entity entity) {
        if (entity instanceof Quadcopter quadcopter) {
            quadcopter.getRigidBody().setCollisionShape(MinecraftShape.box(quadcopter.getBoundingBox()));

            TemplateLoader.getTemplateById(quadcopter.getTemplate()).ifPresent(template -> {
                quadcopter.getEntityData().set(Quadcopter.CAMERA_ANGLE, template.metadata().get("cameraAngle").getAsInt());
                quadcopter.getRigidBody().setMass(template.metadata().get("mass").getAsFloat());
                quadcopter.getRigidBody().setDragCoefficient(template.metadata().get("dragCoefficient").getAsFloat());
            });
        }
    }

    public static void onTemplateLoaded(Template template) {
        // 1.21: CreativeModeTab.rebuildSearchTree() was removed. The creative search index is rebuilt
        // by the menu itself when (re)opened, so a newly loaded template still becomes searchable.
    }

    public static void onRebuildCreativeTab(FabricCreativeModeTabOutput content) {
        content.accept(new ItemStack(Quadz.GOGGLES_ITEM));
        content.accept(new ItemStack(Quadz.REMOTE_ITEM));

        final var templates = TemplateLoader.getTemplateByModId(Quadz.MODID);
        Quadz.LOGGER.info("Creative tab rebuild: {} quadz templates", templates.size());

        templates.forEach(template -> {
            final var stack = TemplateLoader.getItemStackFor(template, Quadz.QUADCOPTER_ITEM);

            if (stack != null) {
                content.accept(stack);
            } else {
                Quadz.LOGGER.warn("No item stack for template {} (originDistance={})", template.getId(), template.originDistance());
            }
        });
    }

}
