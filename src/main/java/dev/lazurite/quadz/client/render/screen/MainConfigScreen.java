package dev.lazurite.quadz.client.render.screen;

import dev.lazurite.quadz.client.Config;
import dev.lazurite.quadz.client.event.ClientEventHooks;
import dev.lazurite.quadz.client.render.screen.osd.VelocityUnit;
import dev.lazurite.quadz.common.util.RateProfile;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public interface MainConfigScreen {

    static Screen get(Screen parent) {
        var builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("quadz.config.title"))
            .setSavingRunnable(() -> {
                Config.save();
                // Re-push rate/profile to the synced joystick values so changes apply without relog.
                ClientEventHooks.applyRateConfig(Minecraft.getInstance().player);
            });

        var entryBuilder = builder.entryBuilder();
        var controllerCategory = builder.getOrCreateCategory(Component.translatable("quadz.config.controller.title"));
        var visualsCategory = builder.getOrCreateCategory(Component.translatable("quadz.config.visuals.title"));

        // Rate profile selector + per-profile rate fields. All three profiles' field sets are added,
        // but each field is shown only when its profile is selected (cloth display requirement keyed
        // off the selector). So picking a profile instantly swaps both the field labels (RC Rate vs
        // Center Sensitivity, etc.) and the values — and each profile remembers its own numbers.
        final var profileEntry =
                entryBuilder.startEnumSelector(Component.translatable("quadz.config.controller.rate_profile"), RateProfile.class, Config.rateProfile)
                        .setEnumNameProvider(profile -> ((RateProfile) profile).getTranslation())
                        .setTooltip(Component.translatable("quadz.config.controller.rate_profile.tooltip"))
                        // Intentionally no default value: cloth's per-entry reset arrow would otherwise
                        // revert the *profile selection* (to a default profile), which reads as a bug.
                        // Each profile's value fields keep their own reset arrows for resetting values.
                        .setSaveConsumer(value -> Config.rateProfile = value)
                        .build();
        controllerCategory.addEntry(profileEntry);

        for (RateProfile profile : RateProfile.values()) {
            final int i = profile.ordinal();

            controllerCategory.addEntry(
                    entryBuilder.startFloatField(Component.translatable(profile.rateKey()), Config.rates[i])
                            .setDefaultValue(Config.DEFAULT_RATES[i])
                            .setDisplayRequirement(Requirement.isValue(profileEntry, profile))
                            .setSaveConsumer(value -> Config.rates[i] = value)
                            .build()
            );

            controllerCategory.addEntry(
                    entryBuilder.startFloatField(Component.translatable(profile.superRateKey()), Config.superRates[i])
                            .setDefaultValue(Config.DEFAULT_SUPER_RATES[i])
                            .setDisplayRequirement(Requirement.isValue(profileEntry, profile))
                            .setSaveConsumer(value -> Config.superRates[i] = value)
                            .build()
            );

            controllerCategory.addEntry(
                    entryBuilder.startFloatField(Component.translatable(profile.expoKey()), Config.expos[i])
                            .setDefaultValue(Config.DEFAULT_EXPOS[i])
                            .setDisplayRequirement(Requirement.isValue(profileEntry, profile))
                            .setSaveConsumer(value -> Config.expos[i] = value)
                            .build()
            );
        }

        controllerCategory.addEntry(
                entryBuilder.startFloatField(Component.translatable("quadz.config.controller.deadzone"), Config.deadzone)
                        .setDefaultValue(Config.deadzone)
                        .setSaveConsumer(value -> Config.deadzone = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.osd_toggle"), Config.osdEnabled)
                        .setDefaultValue(Config.osdEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.osd_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.osdEnabled = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.speed_display_toggle"), Config.speedDisplayEnabled)
                        .setDefaultValue(Config.speedDisplayEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.speed_display_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.speedDisplayEnabled = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.stick_display_toggle"), Config.stickDisplayEnabled)
                        .setDefaultValue(Config.stickDisplayEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.stick_display_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.stickDisplayEnabled = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.camera_angle_display_toggle"), Config.cameraAngleDisplayEnabled)
                        .setDefaultValue(Config.cameraAngleDisplayEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.camera_angle_display_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.cameraAngleDisplayEnabled = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startIntSlider(Component.translatable("quadz.config.visuals.stick_scale"), Math.round(Config.stickScale * 100), 15, 200)
                        .setDefaultValue(100)
                        .setTextGetter(value -> Component.literal(value + "%"))
                        .setTooltip(Component.translatable("quadz.config.visuals.stick_scale.tooltip"))
                        .setSaveConsumer(value -> Config.stickScale = value * 0.01f)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startEnumSelector(Component.translatable("quadz.config.visuals.velocity_unit"), VelocityUnit.class, Config.velocityUnit)
                        .setEnumNameProvider(unit -> ((VelocityUnit) unit).getTranslation())
                        .setDefaultValue(Config.velocityUnit)
                        .setSaveConsumer(value -> Config.velocityUnit = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.follow_los_toggle"), Config.followLOS)
                        .setDefaultValue(Config.followLOS)
                        .setSaveConsumer(value -> Config.followLOS = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.static_toggle"), Config.videoInterferenceEnabled)
                    .setDefaultValue(Config.videoInterferenceEnabled)
                    .setSaveConsumer(value -> Config.videoInterferenceEnabled = value)
                    .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.fisheye_toggle"), Config.fisheyeEnabled)
                        .setDefaultValue(Config.fisheyeEnabled)
                        .setSaveConsumer(value -> Config.fisheyeEnabled = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startIntSlider(Component.translatable("quadz.config.visuals.fisheye_amount"), (int) (Config.fisheyeAmount * 100), 0, 100)
                        .setDefaultValue((int) (Config.fisheyeAmount * 100))
                        .setSaveConsumer(value -> Config.fisheyeAmount = value * 0.01f)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.camera_in_center_toggle"), Config.renderCameraInCenter)
                        .setDefaultValue(Config.renderCameraInCenter)
                        .setSaveConsumer(value -> Config.renderCameraInCenter = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.first_person_render_toggle"), Config.renderFirstPerson)
                        .setDefaultValue(Config.renderFirstPerson)
                        .setSaveConsumer(value -> Config.renderFirstPerson = value)
                        .build()
        );

        builder.setGlobalized(true);
        return builder.build();
    }

}
