package com.com.poke.rng.dsum.util;

import com.com.poke.rng.dsum.constants.EncounterSlot;

import java.awt.Color;

public final class SlotPalette {

    private static final double MUTED_WHITE_BLEND = 0.58;
    private static volatile Color mutedBlendTarget = Color.WHITE;

    private SlotPalette() {
    }

    /** Called from {@code UiTheme.install}; do not use from app code. */
    public static void setMutedBlendTarget(final Color c) {
        mutedBlendTarget = c != null ? c : Color.WHITE;
    }

    public static Color fillColor(final EncounterSlot slot) {
        return switch (slot) {
            case _1 -> new Color(255, 130, 150);
            case _2 -> new Color(255, 180, 100);
            case _3 -> new Color(255, 220, 100);
            case _4 -> new Color(120, 220, 150);
            case _5 -> new Color(100, 210, 230);
            case _6 -> new Color(120, 170, 255);
            case _7 -> new Color(210, 130, 220);
            case _8 -> new Color(255, 150, 200);
            case _9 -> new Color(120, 120, 120);
            case _10 -> new Color(140, 150, 175);
        };
    }

    /** Pastel slot tint for default slot buttons (blend target from theme: white in light, charcoal in dark). */
    public static Color mutedFillColor(final EncounterSlot slot) {
        return blendToward(fillColor(slot), MUTED_WHITE_BLEND, mutedBlendTarget);
    }

    /** Strongest highlight for the likeliest suggested slot. */
    public static Color likelyAccent(final EncounterSlot slot) {
        return saturate(fillColor(slot).brighter());
    }

    /** Vibrant but slightly softer than {@link #likelyAccent} for other suggested slots in range. */
    public static Color nearLikelyAccent(final EncounterSlot slot) {
        return saturate(fillColor(slot));
    }

    private static Color blendToward(final Color c, final double towardsTargetFraction, final Color target) {
        final double t = 1.0 - towardsTargetFraction;
        return new Color(
                clamp255((int) (c.getRed() * t + target.getRed() * towardsTargetFraction)),
                clamp255((int) (c.getGreen() * t + target.getGreen() * towardsTargetFraction)),
                clamp255((int) (c.getBlue() * t + target.getBlue() * towardsTargetFraction))
        );
    }

    private static Color saturate(final Color c) {
        final float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        final float s = Math.min(1f, hsb[1] * 1.12f);
        final float b = Math.min(1f, hsb[2] * 1.05f);
        return Color.getHSBColor(hsb[0], s, b);
    }

    private static int clamp255(final int v) {
        return Math.min(255, Math.max(0, v));
    }
}
