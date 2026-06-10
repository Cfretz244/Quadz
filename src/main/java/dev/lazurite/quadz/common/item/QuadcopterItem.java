package dev.lazurite.quadz.common.item;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.lazurite.form.api.Templated;
import dev.lazurite.form.api.render.FormRegistry;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.common.util.Bindable;
import dev.lazurite.quadz.client.render.entity.QuadcopterEntityRenderer;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import com.geckolib.animatable.GeoItem;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Represents a quadcopter, allows the player to spawn with right-click on the ground.
 * @see QuadcopterEntityRenderer
 */
public class QuadcopterItem extends Item implements GeoItem, Templated.Item {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public QuadcopterItem() {
        // 1.21.4: items must carry their registry id in Properties (setId) at construction.
        super(new Properties().stacksTo(1)
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Quadz.MODID, "quadcopter"))));
    }

    // 1.21.2: InteractionResultHolder is gone; use() returns InteractionResult directly.
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand interactionHand) {
        var itemStack = player.getItemInHand(interactionHand);
        var hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // wave hand
        } else {
            var entity = Quadz.QUADCOPTER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            entity.copyFrom(Templated.get(itemStack));
            Bindable.get(itemStack).ifPresent(entity::copyFrom);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                entity.absSnapTo(hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z);
                entity.getRigidBody().setPhysicsRotation(Convert.toBullet(QuaternionHelper.rotateY(Convert.toMinecraft(new Quaternion()), -player.getYRot())));
            } else {
                var random = new Random();
                var direction = hitResult.getLocation().subtract(player.position()).add(0, player.getEyeHeight(), 0).normalize();
                var pos = player.position().add(direction);

                entity.absSnapTo(pos.x, pos.y, pos.z);
                entity.getRigidBody().setLinearVelocity(Convert.toBullet(direction).multLocal(4).multLocal(new Vector3f(1, 3, 1)));
                entity.getRigidBody().setAngularVelocity(new Vector3f(random.nextFloat() * 2, random.nextFloat() * 2, random.nextFloat() * 2));
            }

            level.addFreshEntity(entity);
            itemStack.shrink(1);
        }

        return InteractionResult.SUCCESS_SERVER; // wave hand
    }

    // GeckoLib 4.8: getGeoItemRenderer returns a GeoItemRenderer (BlockEntityWithoutLevelRenderer
    // was removed by the 1.21.4 item model rework).
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                return FormRegistry.getItemRenderer(QuadcopterItem.this);
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<QuadcopterItem>("base", 20, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // 1.21.2: getDescriptionId(ItemStack) is gone; per-stack names flow through getName(ItemStack).
    @Override
    public @NotNull Component getName(ItemStack itemStack) {
        var template = Templated.get(itemStack).getTemplate();
        return Component.translatable("template.quadz." + template);
    }

}
