package dev.lazurite.quadz.client.render.screen;

import dev.lazurite.quadz.client.Config;
import dev.lazurite.quadz.client.event.ClientEventHooks;
import dev.lazurite.quadz.client.render.screen.osd.VelocityUnit;
import dev.lazurite.quadz.common.util.RateProfile;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public interface MainConfigScreen {

    static Screen get(Screen parent) {
        // Captured to detect an in-session Link Axes on -> off transition when the screen saves.
        final boolean wasLinked = Config.linkAxes;

        var builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("quadz.config.title"))
            .setSavingRunnable(() -> {
                // If the user just turned Link Axes off, seed every axis from the (shared) values
                // they had while linked — including edits made this same session, since the field
                // save-consumers have already run by now. Cloth closes the screen on save, so the
                // per-axis fields show these seeded values the next time it's opened.
                if (wasLinked && !Config.linkAxes) {
                    Config.mirrorPerAxisFromShared();
                }
                Config.save();
                // Re-push rate/profile to the synced joystick values so changes apply without relog.
                ClientEventHooks.applyRateConfig(Minecraft.getInstance().player);
            });

        var entryBuilder = builder.entryBuilder();
        var controllerCategory = builder.getOrCreateCategory(Component.translatable("quadz.config.controller.title"));
        var visualsCategory = builder.getOrCreateCategory(Component.translatable("quadz.config.visuals.title"));

        // Rate profile selector + per-profile rate fields. Every profile's field set is added but
        // gated by a cloth display requirement keyed off the selector (and the Link Axes toggle
        // below), so the visible fields — labels and values — swap instantly when you change either.
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

        // Link Axes: on = one shared rate set drives pitch/yaw/roll; off = independent per-axis rates.
        final var linkEntry =
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.controller.link_axes"), Config.linkAxes)
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("quadz.config.controller.link_axes.tooltip"))
                        .setSaveConsumer(value -> Config.linkAxes = value)
                        .build();
        controllerCategory.addEntry(linkEntry);

        // Field-entry refs, so the rate-curve graph below can read live (in-progress) values.
        // [profile][param 0=rate,1=superRate,2=expo] for the linked (shared) fields; and
        // [profile][axis 0=pitch,1=yaw,2=roll][param] for the unlinked per-axis fields.
        final int nProfiles = RateProfile.values().length;
        final FloatListEntry[][] sharedFields = new FloatListEntry[nProfiles][3];
        final FloatListEntry[][][] axisFields = new FloatListEntry[nProfiles][3][3];

        for (RateProfile profile : RateProfile.values()) {
            final int i = profile.ordinal();
            final var thisProfile = Requirement.isValue(profileEntry, profile);
            final var linked = Requirement.all(thisProfile, Requirement.isTrue(linkEntry));
            final var unlinked = Requirement.all(thisProfile, Requirement.isFalse(linkEntry));

            // Linked: one shared set per profile.
            sharedFields[i][0] = addRateField(controllerCategory, entryBuilder, Component.translatable(profile.rateKey()),      Config.rates,      Config.DEFAULT_RATES,      i, linked);
            sharedFields[i][1] = addRateField(controllerCategory, entryBuilder, Component.translatable(profile.superRateKey()), Config.superRates, Config.DEFAULT_SUPER_RATES, i, linked);
            sharedFields[i][2] = addRateField(controllerCategory, entryBuilder, Component.translatable(profile.expoKey()),      Config.expos,      Config.DEFAULT_EXPOS,      i, linked);

            // Unlinked: independent pitch / yaw / roll sets, each labelled "<Axis> <field name>".
            axisFields[i][0][0] = addRateField(controllerCategory, entryBuilder, axisLabel("pitch", profile.rateKey()),      Config.pitchRates,      Config.DEFAULT_RATES,      i, unlinked);
            axisFields[i][0][1] = addRateField(controllerCategory, entryBuilder, axisLabel("pitch", profile.superRateKey()), Config.pitchSuperRates, Config.DEFAULT_SUPER_RATES, i, unlinked);
            axisFields[i][0][2] = addRateField(controllerCategory, entryBuilder, axisLabel("pitch", profile.expoKey()),      Config.pitchExpos,      Config.DEFAULT_EXPOS,      i, unlinked);
            axisFields[i][1][0] = addRateField(controllerCategory, entryBuilder, axisLabel("yaw",   profile.rateKey()),      Config.yawRates,        Config.DEFAULT_RATES,      i, unlinked);
            axisFields[i][1][1] = addRateField(controllerCategory, entryBuilder, axisLabel("yaw",   profile.superRateKey()), Config.yawSuperRates,   Config.DEFAULT_SUPER_RATES, i, unlinked);
            axisFields[i][1][2] = addRateField(controllerCategory, entryBuilder, axisLabel("yaw",   profile.expoKey()),      Config.yawExpos,        Config.DEFAULT_EXPOS,      i, unlinked);
            axisFields[i][2][0] = addRateField(controllerCategory, entryBuilder, axisLabel("roll",  profile.rateKey()),      Config.rollRates,       Config.DEFAULT_RATES,      i, unlinked);
            axisFields[i][2][1] = addRateField(controllerCategory, entryBuilder, axisLabel("roll",  profile.superRateKey()), Config.rollSuperRates,  Config.DEFAULT_SUPER_RATES, i, unlinked);
            axisFields[i][2][2] = addRateField(controllerCategory, entryBuilder, axisLabel("roll",  profile.expoKey()),      Config.rollExpos,       Config.DEFAULT_EXPOS,      i, unlinked);
        }

        // Live rate-curve graph: reads the selected profile + link state + the field entries above,
        // recomputing each frame so it redraws as the pilot tunes. Linked → one white curve; unlinked
        // → pitch/yaw/roll in three colours.
        final Supplier<List<RateCurveGraphEntry.CurveSpec>> curveSupplier = () -> {
            final RateProfile p = profileEntry.getValue();
            if (p == null) {
                return List.of();
            }
            final int pi = p.ordinal();
            final Boolean linkedVal = linkEntry.getValue();
            final boolean linked = linkedVal == null || linkedVal;
            final List<RateCurveGraphEntry.CurveSpec> list = new ArrayList<>();
            if (linked) {
                list.add(new RateCurveGraphEntry.CurveSpec(p,
                        liveVal(sharedFields[pi][0]), liveVal(sharedFields[pi][1]), liveVal(sharedFields[pi][2]),
                        0xFFFFFFFF, "All"));
            } else {
                final int[] colors = { 0xFFFF6B6B, 0xFF6BD46B, 0xFF6BAAFF };
                final String[] names = { "Pitch", "Yaw", "Roll" };
                for (int a = 0; a < 3; a++) {
                    list.add(new RateCurveGraphEntry.CurveSpec(p,
                            liveVal(axisFields[pi][a][0]), liveVal(axisFields[pi][a][1]), liveVal(axisFields[pi][a][2]),
                            colors[a], names[a]));
                }
            }
            return list;
        };
        controllerCategory.addEntry(new RateCurveGraphEntry(Component.translatable("quadz.config.controller.rate_curve"), curveSupplier));

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

        // HUD element toggles, ordered: stick overlays first, then the text readouts in the order
        // they appear on screen top-to-bottom when all are enabled (FOV, camera angle, speed, coords).
        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.stick_display_toggle"), Config.stickDisplayEnabled)
                        .setDefaultValue(Config.stickDisplayEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.stick_display_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.stickDisplayEnabled = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.fov_display_toggle"), Config.fovDisplayEnabled)
                        .setDefaultValue(Config.fovDisplayEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.fov_display_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.fovDisplayEnabled = value)
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
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.speed_display_toggle"), Config.speedDisplayEnabled)
                        .setDefaultValue(Config.speedDisplayEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.speed_display_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.speedDisplayEnabled = value)
                        .build()
        );

        visualsCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("quadz.config.visuals.coords_display_toggle"), Config.coordsDisplayEnabled)
                        .setDefaultValue(Config.coordsDisplayEnabled)
                        .setTooltip(Component.translatable("quadz.config.visuals.coords_display_toggle.tooltip"))
                        .setSaveConsumer(value -> Config.coordsDisplayEnabled = value)
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
                entryBuilder.startIntSlider(Component.translatable("quadz.config.visuals.fpv_fov"), Config.fpvFov, Config.FPV_FOV_MIN, Config.FPV_FOV_MAX)
                        .setDefaultValue(110)
                        .setTextGetter(value -> Component.literal(value + "°"))
                        .setTooltip(Component.translatable("quadz.config.visuals.fpv_fov.tooltip"))
                        .setSaveConsumer(value -> Config.fpvFov = value)
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

    /**
     * Adds one float rate field bound to {@code store[i]}, shown only when {@code displayRequirement}
     * holds. Returns the built entry so the rate-curve graph can read its live (in-progress) value.
     */
    private static FloatListEntry addRateField(ConfigCategory category, ConfigEntryBuilder entryBuilder, Component label, float[] store, float[] defaults, int i, Requirement displayRequirement) {
        var entry = entryBuilder.startFloatField(label, store[i])
                .setDefaultValue(defaults[i])
                .setDisplayRequirement(displayRequirement)
                .setSaveConsumer(value -> store[i] = value)
                .build();
        category.addEntry(entry);
        return entry;
    }

    /** Live value of a rate field, guarded against a null/mid-edit-invalid parse. */
    private static double liveVal(FloatListEntry entry) {
        try {
            Float f = entry.getValue();
            return f == null ? 0.0 : f.doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    /** "<Axis> <field name>", e.g. "Pitch RC Rate" — axis prefix joined to the profile's field label. */
    private static Component axisLabel(String axisKey, String fieldKey) {
        return Component.translatable("quadz.config.controller.axis." + axisKey)
                .append(" ")
                .append(Component.translatable(fieldKey));
    }

}
