package dev.lazurite.quadz.common.hooks;

import dev.lazurite.form.api.loader.TemplateLoader;
import dev.lazurite.form.impl.common.template.model.Template;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.common.entity.Quadcopter;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class ServerEventHooks {

    public static void onEntityTemplateChanged(Entity entity) {
        if (entity instanceof Quadcopter quadcopter) {
            TemplateLoader.getTemplateById(quadcopter.getTemplate()).ifPresentOrElse(template -> {
                final var meta = template.metadata();
                final float width = meta.get("width").getAsFloat();
                final float height = meta.get("height").getAsFloat();
                // Optional per-template collision depth (front-back / Z). Defaults to width, which
                // reproduces the old square width×width footprint for any template that omits it.
                // Giving it a distinct value lets a drone use a rectangular prism hitbox that matches
                // its frame instead of an oversized square. Only the box's extent matters to the Bullet
                // shape (it's centered on the body and rotates with it), so a zero-origin AABB of the
                // right size is all we need.
                final float depth = meta.has("depth") ? meta.get("depth").getAsFloat() : width;
                quadcopter.getRigidBody().setCollisionShape(MinecraftShape.box(new AABB(0, 0, 0, width, height, depth)));

                // Aerodynamic drag reference box, DECOUPLED from the (possibly tiny) collision box.
                // Rayon's SIMPLE central air drag derives its area from the collision box, so shrinking
                // the hitbox to fit through gaps also cut drag and raised top speed. These optional
                // dragWidth/Height/Depth fields let each template keep its historical aero size (and
                // thus its original top speed + flight feel) independent of the collision box. Each
                // defaults to the collision dimension, reproducing the old box-derived behavior when a
                // template omits them. Area is Σ(half-extent²), matching Rayon's box formula. It's a
                // networked body property, so the pilot's client sim and the server always agree.
                final float dragWidth = meta.has("dragWidth") ? meta.get("dragWidth").getAsFloat() : width;
                final float dragHeight = meta.has("dragHeight") ? meta.get("dragHeight").getAsFloat() : height;
                final float dragDepth = meta.has("dragDepth") ? meta.get("dragDepth").getAsFloat() : depth;
                final float dragArea = (dragWidth * dragWidth + dragHeight * dragHeight + dragDepth * dragDepth) / 4.0f;
                quadcopter.getRigidBody().setDragArea(dragArea);

                // Restore a saved uptilt (from a picked-up drone item) if one is pending, else use the
                // template default. This is the placement path, so it preserves the pilot's adjustment.
                var pending = quadcopter.consumePendingCameraAngle();
                quadcopter.getEntityData().set(Quadcopter.CAMERA_ANGLE,
                        pending != null ? pending : meta.get("cameraAngle").getAsInt());
                quadcopter.getRigidBody().setMass(meta.get("mass").getAsFloat());
                quadcopter.getRigidBody().setDragCoefficient(meta.get("dragCoefficient").getAsFloat());
            }, () -> {
                // No template (e.g. a template-less /give): fall back to the square vanilla box, and
                // reset the drag area (-1) so drag derives from that box as before.
                quadcopter.getRigidBody().setCollisionShape(MinecraftShape.box(quadcopter.getBoundingBox()));
                quadcopter.getRigidBody().setDragArea(-1.0f);
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
