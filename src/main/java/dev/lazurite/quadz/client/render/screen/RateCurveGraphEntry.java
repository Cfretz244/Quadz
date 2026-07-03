package dev.lazurite.quadz.client.render.screen;

import dev.lazurite.quadz.common.util.RateProfile;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Display-only config entry that plots the active rate curve(s) — output angular rate vs. stick input
 * over the positive quadrant (stick 0..100%, rate 0..max, with halfway/full tick labels on each axis)
 * — so the user can see the shape of their tuning update live. When axes are
 * linked it draws one white curve; unlinked, it draws pitch/yaw/roll in three colors. The curve
 * data is pulled fresh each frame from a supplier (which reads the live config field entries), so it
 * redraws as values are typed. Not a real setting: {@link #getValue()} is a stable dummy and it never
 * saves anything.
 */
public class RateCurveGraphEntry extends AbstractConfigListEntry<Object> {

    /** One plotted curve: which profile's math, its three params, a colour, and a legend label. */
    public record CurveSpec(RateProfile profile, double p1, double p2, double p3, int color, String label) {
        double rateAt(double stick) {
            // delta = 1.0 → the returned "step" is the raw angular rate in the profile's units.
            return profile.calculate(stick, p1, p2, p3, 1.0);
        }
    }

    private static final Object VALUE = new Object();
    private static final int GRAPH_HEIGHT = 56;
    private static final int TITLE_GAP = 12;

    private final Supplier<List<CurveSpec>> curvesSupplier;
    private final Font font;

    public RateCurveGraphEntry(Component title, Supplier<List<CurveSpec>> curvesSupplier) {
        super(title, false);
        this.curvesSupplier = curvesSupplier;
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int index, int y, int x, int entryWidth, int entryHeight,
                                   int mouseX, int mouseY, boolean isHovered, float delta) {
        // Title + colour legend on the first line.
        g.text(font, getFieldName(), x, y + 1, 0xFFFFFFFF, true);
        List<CurveSpec> curves;
        try {
            curves = curvesSupplier.get();
        } catch (Exception e) {
            curves = Collections.emptyList();
        }

        int legendX = x + font.width(getFieldName()) + 10;
        for (CurveSpec c : curves) {
            var label = Component.literal(c.label());
            g.text(font, label, legendX, y + 1, c.color() | 0xFF000000, true);
            legendX += font.width(label) + 8;
        }

        // Single positive quadrant: stick 0..1 along the bottom, rate 0..max up the left side. The
        // rate curves are odd (symmetric through the origin), so the positive quadrant carries all the
        // shape info and reads cleaner than a full centred plot.
        final int yLabelGutter = 15;   // left room for the rate value labels
        int gx = x + yLabelGutter;
        int gyTop = y + TITLE_GAP;
        int gw = Math.max(20, Math.min(entryWidth - yLabelGutter - 4, 240));
        int gh = GRAPH_HEIGHT;
        int gyBot = gyTop + gh;

        g.fill(gx, gyTop, gx + gw + 1, gyBot, 0x40000000);       // backdrop
        g.fill(gx, gyTop, gx + 1, gyBot + 1, 0x80FFFFFF);        // left (rate) axis
        g.fill(gx, gyBot, gx + gw + 1, gyBot + 1, 0x80FFFFFF);   // bottom (stick) axis

        // Normalise the y-scale to the largest full-deflection rate on screen so the curve shapes are
        // visible regardless of profile magnitude (Betaflight rates are ~1, Actual is hundreds).
        double max = 1e-6;
        for (CurveSpec c : curves) {
            max = Math.max(max, Math.abs(c.rateAt(1.0)));
        }

        // Tick marks (+ labels) at halfway and full on each axis.
        int xMid = gx + gw / 2;
        int xEnd = gx + gw;
        int yMid = gyBot - gh / 2;
        g.fill(xMid, gyBot, xMid + 1, gyBot + 3, 0x80FFFFFF);    // stick 50%
        g.fill(xEnd, gyBot, xEnd + 1, gyBot + 3, 0x80FFFFFF);    // stick 100%
        g.fill(gx - 3, yMid, gx, yMid + 1, 0x80FFFFFF);          // rate halfway
        g.fill(gx - 3, gyTop, gx, gyTop + 1, 0x80FFFFFF);        // rate max
        drawSmallLabel(g, "50%", xMid - 5, gyBot + 3);
        drawSmallLabel(g, "100%", xEnd - 9, gyBot + 3);
        drawSmallLabel(g, fmtRate(max / 2.0), x, yMid - 2);
        drawSmallLabel(g, fmtRate(max), x, gyTop - 2);

        for (CurveSpec c : curves) {
            for (int col = 0; col <= gw; col++) {
                double stick = col / (double) gw;               // 0..1
                double r = Math.abs(c.rateAt(stick));
                if (!Double.isFinite(r)) {
                    continue;
                }
                int py = (int) Math.round(gyBot - (r / max) * gh);
                py = Math.max(gyTop, Math.min(gyBot, py));
                g.fill(gx + col, py, gx + col + 1, py + 1, c.color() | 0xFF000000);
            }
        }
    }

    /** Draws a half-size, semi-transparent axis label with its top-left at (px, py). */
    private void drawSmallLabel(GuiGraphicsExtractor g, String text, int px, int py) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(px, py);
        pose.scale(0.5f, 0.5f);
        g.text(font, Component.literal(text), 0, 0, 0xC0FFFFFF, false);
        pose.popMatrix();
    }

    /** Rate-axis value: one decimal for small (Betaflight ~1–3), whole numbers for large (deg/s). */
    private static String fmtRate(double v) {
        return v >= 10.0 ? String.valueOf(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @Override
    public int getItemHeight() {
        return TITLE_GAP + GRAPH_HEIGHT + 10; // +10 leaves room for the x-axis tick labels below the box
    }

    @Override
    public Object getValue() {
        return VALUE;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void save() {
        // display-only
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }
}
