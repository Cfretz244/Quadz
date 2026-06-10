package dev.lazurite.quadz.common.item;

import dev.lazurite.quadz.Quadz;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Represents the goggles a player wears in order to see their quadcopter's POV.
 */
public class GogglesItem extends ArmorItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GogglesItem() {
        // 1.21.2/1.21.4: armor materials moved to world.item.equipment, ArmorItem.Type became
        // ArmorType, and items must carry their registry id in Properties (setId).
        super(
                ArmorMaterials.LEATHER,
                ArmorType.HELMET,
                new Properties().stacksTo(1)
                        .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Quadz.MODID, "goggles")))
        );
    }

    // GeckoLib 4.8: getGeoArmorRenderer gained the EquipmentClientInfo.LayerType param and a
    // HumanoidRenderState-typed base model; GeckoLib's armor layer calls prepForRender itself now.
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<GogglesItem> renderer;

            @Override
            public <E extends LivingEntity, S extends HumanoidRenderState> @NotNull HumanoidModel<?> getGeoArmorRenderer(E livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, EquipmentClientInfo.LayerType layerType, HumanoidModel<S> original) {
                if (this.renderer == null) {
                    this.renderer = new GeoArmorRenderer<>(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(Quadz.MODID, "armor/goggles")));
                }

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}
