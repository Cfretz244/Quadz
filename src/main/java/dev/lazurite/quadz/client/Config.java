package dev.lazurite.quadz.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.render.screen.osd.VelocityUnit;
import dev.lazurite.quadz.common.util.RateProfile;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    public static int controllerId = 0;

    public static int pitch = 1;
    public static int yaw = 3;
    public static int roll = 0;
    public static int throttle = 2;

    public static boolean pitchInverted = false;
    public static boolean yawInverted = false;
    public static boolean rollInverted = false;
    public static boolean throttleInverted = false;
    public static boolean throttleInCenter = false;

    public static RateProfile rateProfile = RateProfile.BETAFLIGHT;

    // Per-profile rate parameters, indexed by RateProfile.ordinal() so each profile remembers its
    // own values (switching profiles no longer drags unsuitable numbers across). Order MUST match
    // the RateProfile enum: BETAFLIGHT, ACTUAL, KISS.
    public static final float[] DEFAULT_RATES       = { 1.0f,  70.0f, 1.0f };
    public static final float[] DEFAULT_SUPER_RATES = { 0.7f, 670.0f, 0.7f };
    public static final float[] DEFAULT_EXPOS       = { 0.0f,   0.0f, 0.0f };
    public static final float[] rates      = DEFAULT_RATES.clone();
    public static final float[] superRates = DEFAULT_SUPER_RATES.clone();
    public static final float[] expos      = DEFAULT_EXPOS.clone();

    /** The active profile's rate parameter (used when axes are linked). */
    public static float rate()      { return rates[rateProfile.ordinal()]; }
    public static float superRate() { return superRates[rateProfile.ordinal()]; }
    public static float expo()      { return expos[rateProfile.ordinal()]; }

    // Per-axis rates. When linkAxes is true (default) pitch/yaw/roll all use the shared values
    // above; when false, each axis uses its own values below. Per-axis arrays are indexed by
    // RateProfile.ordinal() too (so per-axis tuning is still per-profile), and default to the same
    // per-profile defaults so unlinking starts neutral.
    public static boolean linkAxes = true;
    public static final float[] pitchRates      = DEFAULT_RATES.clone();
    public static final float[] pitchSuperRates = DEFAULT_SUPER_RATES.clone();
    public static final float[] pitchExpos      = DEFAULT_EXPOS.clone();
    public static final float[] yawRates        = DEFAULT_RATES.clone();
    public static final float[] yawSuperRates   = DEFAULT_SUPER_RATES.clone();
    public static final float[] yawExpos        = DEFAULT_EXPOS.clone();
    public static final float[] rollRates       = DEFAULT_RATES.clone();
    public static final float[] rollSuperRates  = DEFAULT_SUPER_RATES.clone();
    public static final float[] rollExpos       = DEFAULT_EXPOS.clone();

    public static float deadzone = 0.05f;
    public static boolean followLOS = true;
    public static boolean renderFirstPerson = true;
    public static boolean renderCameraInCenter = false;
    public static boolean osdEnabled = true;
    public static boolean speedDisplayEnabled = true;
    public static boolean stickDisplayEnabled = true;
    public static boolean cameraAngleDisplayEnabled = true;
    public static float stickScale = 1.0f;
    public static VelocityUnit velocityUnit = VelocityUnit.METERS_PER_SECOND;
    public static boolean videoInterferenceEnabled = true;
    public static boolean fisheyeEnabled = true;
    public static float fisheyeAmount = 0.8f;

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("quadz.json");
    }

    public static void save() {
        final var path = getConfigPath();
        final var config = new JsonObject();

        config.add("pitch", new JsonPrimitive(pitch));
        config.add("yaw", new JsonPrimitive(yaw));
        config.add("roll", new JsonPrimitive(roll));
        config.add("throttle", new JsonPrimitive(throttle));
        config.add("pitchInverted", new JsonPrimitive(pitchInverted));
        config.add("yawInverted", new JsonPrimitive(yawInverted));
        config.add("rollInverted", new JsonPrimitive(rollInverted));
        config.add("throttleInverted", new JsonPrimitive(throttleInverted));
        config.add("throttleInCenter", new JsonPrimitive(throttleInCenter));
        config.add("rateProfile", new JsonPrimitive(rateProfile.name()));
        config.add("rates", floatArrayToJson(rates));
        config.add("superRates", floatArrayToJson(superRates));
        config.add("expos", floatArrayToJson(expos));
        config.add("linkAxes", new JsonPrimitive(linkAxes));
        config.add("pitchRates", floatArrayToJson(pitchRates));
        config.add("pitchSuperRates", floatArrayToJson(pitchSuperRates));
        config.add("pitchExpos", floatArrayToJson(pitchExpos));
        config.add("yawRates", floatArrayToJson(yawRates));
        config.add("yawSuperRates", floatArrayToJson(yawSuperRates));
        config.add("yawExpos", floatArrayToJson(yawExpos));
        config.add("rollRates", floatArrayToJson(rollRates));
        config.add("rollSuperRates", floatArrayToJson(rollSuperRates));
        config.add("rollExpos", floatArrayToJson(rollExpos));
        config.add("controllerId", new JsonPrimitive(controllerId));
        config.add("deadzone", new JsonPrimitive(deadzone));
        config.add("followLOS", new JsonPrimitive(followLOS));
        config.add("renderFirstPerson", new JsonPrimitive(renderFirstPerson));
        config.add("renderCameraInCenter", new JsonPrimitive(renderCameraInCenter));
        config.add("osdEnabled", new JsonPrimitive(osdEnabled));
        config.add("speedDisplayEnabled", new JsonPrimitive(speedDisplayEnabled));
        config.add("stickDisplayEnabled", new JsonPrimitive(stickDisplayEnabled));
        config.add("cameraAngleDisplayEnabled", new JsonPrimitive(cameraAngleDisplayEnabled));
        config.add("stickScale", new JsonPrimitive(stickScale));
        config.add("velocityUnit", new JsonPrimitive(velocityUnit.toString()));
        config.add("videoInterferenceEnabled", new JsonPrimitive(videoInterferenceEnabled));
        config.add("fisheyeEnabled", new JsonPrimitive(fisheyeEnabled));
        config.add("fisheyeAmount", new JsonPrimitive(fisheyeAmount));

        try {
            Files.writeString(path, config.toString());
        } catch(IOException e) {
            Quadz.LOGGER.error(e);
        }
    }

    public static void load() {
        final var path = getConfigPath();

        if (!Files.exists(path)) {
            save();
            return;
        }

        try {
            final var config = JsonParser.parseReader(new InputStreamReader(Files.newInputStream(path))).getAsJsonObject();
            pitch = config.get("pitch").getAsInt();
            yaw = config.get("yaw").getAsInt();
            roll = config.get("roll").getAsInt();
            throttle = config.get("throttle").getAsInt();
            pitchInverted = config.get("pitchInverted").getAsBoolean();
            yawInverted = config.get("yawInverted").getAsBoolean();
            rollInverted = config.get("rollInverted").getAsBoolean();
            throttleInverted = config.get("throttleInverted").getAsBoolean();
            throttleInCenter = config.get("throttleInCenter").getAsBoolean();
            if (config.has("rateProfile")) rateProfile = RateProfile.valueOf(config.get("rateProfile").getAsString());
            if (config.has("linkAxes")) linkAxes = config.get("linkAxes").getAsBoolean();
            // readFloatArray no-ops on absent keys, so older configs just keep the defaults.
            readFloatArray(config, "pitchRates", pitchRates);
            readFloatArray(config, "pitchSuperRates", pitchSuperRates);
            readFloatArray(config, "pitchExpos", pitchExpos);
            readFloatArray(config, "yawRates", yawRates);
            readFloatArray(config, "yawSuperRates", yawSuperRates);
            readFloatArray(config, "yawExpos", yawExpos);
            readFloatArray(config, "rollRates", rollRates);
            readFloatArray(config, "rollSuperRates", rollSuperRates);
            readFloatArray(config, "rollExpos", rollExpos);
            if (config.has("rates")) {
                readFloatArray(config, "rates", rates);
                readFloatArray(config, "superRates", superRates);
                readFloatArray(config, "expos", expos);
            } else if (config.has("rate")) {
                // Migrate a legacy single-value config: those values belonged to the then-active profile.
                final var i = rateProfile.ordinal();
                rates[i] = config.get("rate").getAsFloat();
                superRates[i] = config.get("superRate").getAsFloat();
                expos[i] = config.get("expo").getAsFloat();
            }
            controllerId = config.get("controllerId").getAsInt();
            deadzone = config.get("deadzone").getAsFloat();
            followLOS = config.get("followLOS").getAsBoolean();
            renderFirstPerson = config.get("renderFirstPerson").getAsBoolean();
            renderCameraInCenter = config.get("renderCameraInCenter").getAsBoolean();
            osdEnabled = config.get("osdEnabled").getAsBoolean();
            // Guard newer keys so configs written by older versions still load.
            if (config.has("speedDisplayEnabled")) speedDisplayEnabled = config.get("speedDisplayEnabled").getAsBoolean();
            if (config.has("stickDisplayEnabled")) stickDisplayEnabled = config.get("stickDisplayEnabled").getAsBoolean();
            if (config.has("cameraAngleDisplayEnabled")) cameraAngleDisplayEnabled = config.get("cameraAngleDisplayEnabled").getAsBoolean();
            if (config.has("stickScale")) stickScale = config.get("stickScale").getAsFloat();
            velocityUnit = VelocityUnit.valueOf(config.get("velocityUnit").getAsString());
            videoInterferenceEnabled = config.get("videoInterferenceEnabled").getAsBoolean();
            fisheyeEnabled = config.get("fisheyeEnabled").getAsBoolean();
            fisheyeAmount = config.get("fisheyeAmount").getAsFloat();
        } catch(IOException e) {
            Quadz.LOGGER.error(e);
        }
    }

    private static JsonArray floatArrayToJson(float[] values) {
        final var arr = new JsonArray();
        for (float v : values) {
            arr.add(v);
        }
        return arr;
    }

    private static void readFloatArray(JsonObject config, String key, float[] dest) {
        if (!config.has(key)) {
            return;
        }
        final var arr = config.getAsJsonArray(key);
        for (int i = 0; i < dest.length && i < arr.size(); i++) {
            dest[i] = arr.get(i).getAsFloat();
        }
    }

}
