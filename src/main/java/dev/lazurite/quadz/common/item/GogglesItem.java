package dev.lazurite.quadz.common.item;

import dev.lazurite.quadz.Quadz;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Represents the goggles a player wears in order to see their quadcopter's POV.
 */
public class GogglesItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GogglesItem() {
        // 1.21.5: ArmorItem is gone; armor is a plain Item with humanoidArmor properties.
        super(
                new Properties().stacksTo(1)
                        .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.HELMET)
                        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Quadz.MODID, "goggles")))
        );
    }

    // GeckoLib 5: the armor provider is render-state-based and returns the GeoArmorRenderer
    // directly. Raw type because the renderer's render-state generic has no compile-time
    // vanilla implementation (GeckoLib injects GeoRenderState via mixin at runtime).
    // GeckoLib 5.5: the armor provider takes just the stack and slot.
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
                if (this.renderer == null) {
                    this.renderer = new GeoArmorRenderer(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(Quadz.MODID, "armor/goggles")));
                }

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GogglesItem>("base", 20, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}
