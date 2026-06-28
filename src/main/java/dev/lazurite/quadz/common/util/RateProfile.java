package dev.lazurite.quadz.common.util;

import net.minecraft.network.chat.Component;

/**
 * Rate systems borrowed from Betaflight firmware. Each interprets the same three stored
 * parameters (kept in {@link dev.lazurite.quadz.client.Config} as rate / superRate / expo)
 * differently, with profile-specific names. {@link #calculate} returns an angular-rate step
 * (already multiplied by {@code delta}) in the same units the original Betaflight path used,
 * so all three profiles are magnitude-comparable; fine feel tuning is expected per pilot.
 */
public enum RateProfile {
    BETAFLIGHT,
    ACTUAL,
    KISS;

    /**
     * @param rcCommand stick input, normally -1..1
     * @param p1 the "rate" field   (BF: RC Rate, Actual: Center Sensitivity, KISS: RC Rate)
     * @param p2 the "superRate" field (BF: Super Rate, Actual: Max Rate, KISS: Rate)
     * @param p3 the "expo" field    (BF: RC Expo, Actual: Expo, KISS: RC Curve)
     */
    public double calculate(double rcCommand, double p1, double p2, double p3, double delta) {
        return switch (this) {
            // Existing Betaflight path, unchanged (preserves current feel). Note the original
            // signature order is (rcCommand, rcRate, expo, superRate, delta).
            case BETAFLIGHT -> BetaflightHelper.calculateRates(rcCommand, p1, p3, p2, delta);
            case ACTUAL -> actual(rcCommand, p1, p2, p3, delta);
            case KISS -> kiss(rcCommand, p1, p2, p3, delta);
        };
    }

    /** Betaflight "Actual" rates. p1 = center sensitivity, p2 = max rate, p3 = expo (0..1). */
    private static double actual(double rcCommand, double centerSensitivity, double maxRate, double expo, double delta) {
        final var absCmd = Math.abs(rcCommand);
        final var expof = absCmd * (Math.pow(rcCommand, 5) * expo + rcCommand * (1.0 - expo));
        final var center = centerSensitivity * 200.0;
        final var stickMovement = Math.max(0.0, maxRate * 200.0 - center);
        final var angleRate = rcCommand * center + stickMovement * expof;
        return angleRate * delta;
    }

    /** KISS rates. p1 = RC rate, p2 = rate, p3 = RC curve (expo, 0..1). */
    private static double kiss(double rcCommand, double rcRate, double rate, double rcCurve, double delta) {
        final var absCmd = Math.abs(rcCommand);
        final var useRates = 1.0 / clamp(1.0 - absCmd * rate, 0.01, 1.0);
        final var kissRc = (Math.pow(rcCommand, 3) * rcCurve + rcCommand * (1.0 - rcCurve)) * (rcRate * 0.2);
        final var angleRate = (1000.0 * useRates) * kissRc;
        return angleRate * delta;
    }

    private static double clamp(double n, double min, double max) {
        return Math.max(Math.min(max, n), min);
    }

    public Component getTranslation() {
        return Component.translatable("quadz.config.controller.rate_profile." + name().toLowerCase());
    }

    /** Translation key for the "rate" field's label under this profile. */
    public String rateKey() {
        return "quadz.config.controller." + name().toLowerCase() + ".rate";
    }

    /** Translation key for the "superRate" field's label under this profile. */
    public String superRateKey() {
        return "quadz.config.controller." + name().toLowerCase() + ".super_rate";
    }

    /** Translation key for the "expo" field's label under this profile. */
    public String expoKey() {
        return "quadz.config.controller." + name().toLowerCase() + ".expo";
    }
}
