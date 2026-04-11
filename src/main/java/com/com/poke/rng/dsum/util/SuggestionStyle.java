package com.com.poke.rng.dsum.util;

import java.awt.*;
import java.util.OptionalDouble;

/**
 * Shared colours and layout math for suggested-slot runs (sprite row, wheel, bar).
 * <p>
 * <strong>Wedge overlap darkness</strong> (inner / outer ring): fraction of the painted uncertainty wedge (inner or
 * outer-only band) that falls in this slot — not a probability. Higher = this slot covers more of the wedge.
 * <p>
 * <strong>Distance from likeliest</strong> (along the suggested chain): only used when wedge overlap is negligible;
 * the two darkest distance steps are not used in that case so they stay reserved for strong wedge overlap (otherwise
 * the likeliest slot would look “certain” even when the wedge barely clips it).
 */
public final class SuggestionStyle {

    /** Without meaningful wedge overlap, skip the two darkest distance-only ambers (see class javadoc). */
    private static final int MIN_DISTANCE_INDEX_WITHOUT_WEDGE_OVERLAP = 2;

    private static int clampDistanceForNoWedgeOverlapShading(final int distanceFromLikeliest) {
        return Math.min(5, Math.max(MIN_DISTANCE_INDEX_WITHOUT_WEDGE_OVERLAP, distanceFromLikeliest));
    }

    private SuggestionStyle() {
    }

    /**
     * Distance along the suggested segment (first → … → last) between likeliest and slotIndex; 0 = likeliest.
     */
    public static int segmentDistanceFromLikeliest(
            final int firstIndex,
            final int lastIndex,
            final int likeliestIndex,
            final int slotIndex,
            final int n) {
        int posSlot = -1;
        int posLike = -1;
        int p = 0;
        for (int i = firstIndex; ; i = (i + 1) % n) {
            if (i == slotIndex) {
                posSlot = p;
            }
            if (i == likeliestIndex) {
                posLike = p;
            }
            p++;
            if (i == lastIndex) {
                break;
            }
        }
        return Math.abs(posSlot - posLike);
    }

    /** Richer ambers for likeliest; lighter / softer as distance grows along the suggested run (sprite bars). */
    public static Color amberSuggestionShade(final int distanceFromLikeliest) {
        return switch (Math.min(distanceFromLikeliest, 5)) {
            case 0 -> new Color(220, 105, 8, 250);
            case 1 -> new Color(245, 145, 35, 220);
            case 2 -> new Color(255, 175, 65, 195);
            case 3 -> new Color(255, 200, 105, 175);
            case 4 -> new Color(255, 215, 140, 155);
            default -> new Color(255, 228, 170, 135);
        };
    }

    /**
     * Amber by wedge-overlap fraction: higher {@code portion01} → richer orange. Uses a slight gamma so small
     * overlaps (a sliver of a slot in the wedge) still read visibly different from near-full coverage.
     */
    public static Color amberSuggestionShadeFromWedgeOverlap(final double portion01) {
        final double t = overlapToEmphasis(Math.max(0.0, Math.min(1.0, portion01)));
        final Color strong = new Color(220, 105, 8, 250);
        final Color light = new Color(255, 228, 170, 135);
        final int a = (int) Math.round(light.getAlpha() + t * (strong.getAlpha() - light.getAlpha()));
        final int r = (int) Math.round(light.getRed() + t * (strong.getRed() - light.getRed()));
        final int g = (int) Math.round(light.getGreen() + t * (strong.getGreen() - light.getGreen()));
        final int b = (int) Math.round(light.getBlue() + t * (strong.getBlue() - light.getBlue()));
        return new Color(r, g, b, a);
    }

    /**
     * Sprite-bar / wheel suggested-amber: strong when overlapping the inner uncertainty wedge; faint when only the
     * outer (cycle-length) ring overlaps.
     */
    public static Color amberSuggestedAmberBar(
            final int distanceFromLikeliest,
            final OptionalDouble innerOverlap,
            final OptionalDouble outerOnlyOverlap) {
        if (innerOverlap.isPresent() && innerOverlap.getAsDouble() > 1e-9) {
            return amberSuggestionFillOpaqueFromInnerWedgeOverlap(innerOverlap.getAsDouble());
        }
        if (outerOnlyOverlap.isPresent() && outerOnlyOverlap.getAsDouble() > 1e-9) {
            return amberSuggestionFillOpaqueFromOuterOnlyWedgeOverlap(outerOnlyOverlap.getAsDouble());
        }
        return amberSuggestionShade(clampDistanceForNoWedgeOverlapShading(distanceFromLikeliest));
    }

    /** Stroke for {@link #amberSuggestedAmberBar}. */
    public static Color amberSuggestedAmberBarStroke(
            final int distanceFromLikeliest,
            final OptionalDouble innerOverlap,
            final OptionalDouble outerOnlyOverlap) {
        return amberSuggestedAmberBar(distanceFromLikeliest, innerOverlap, outerOnlyOverlap).darker();
    }

    /** Wheel/bar opaque fill: inner wedge — vivid orange ramp (higher overlap → darker / richer). */
    public static Color amberSuggestionFillOpaqueFromInnerWedgeOverlap(final double portion01) {
        final double t = overlapToEmphasisInnerWedge(Math.max(0.0, Math.min(1.0, portion01)));
        final Color strong = new Color(200, 72, 0);
        final Color light = new Color(255, 205, 130);
        return lerpRgbOpaque(light, strong, t);
    }

    /** Wheel/bar opaque fill: outer ring only — pale wash (higher overlap → slightly stronger hint). */
    public static Color amberSuggestionFillOpaqueFromOuterOnlyWedgeOverlap(final double portion01) {
        final double t = overlapToEmphasisOuterRing(Math.max(0.0, Math.min(1.0, portion01)));
        final Color hint = new Color(255, 205, 155);
        final Color faint = new Color(255, 248, 238);
        return lerpRgbOpaque(faint, hint, t);
    }

    private static double overlapToEmphasisInnerWedge(final double p) {
        return Math.pow(p, 0.42);
    }

    private static double overlapToEmphasisOuterRing(final double p) {
        return Math.pow(p, 0.72);
    }

    private static Color lerpRgbOpaque(final Color a, final Color b, final double t) {
        final double u = Math.max(0.0, Math.min(1.0, t));
        final int r = (int) Math.round(a.getRed() + u * (b.getRed() - a.getRed()));
        final int g = (int) Math.round(a.getGreen() + u * (b.getGreen() - a.getGreen()));
        final int bl = (int) Math.round(a.getBlue() + u * (b.getBlue() - a.getBlue()));
        return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, bl)));
    }

    /** Opaque RGB for wheel/bar overlays using {@link AlphaComposite}. */
    public static Color amberSuggestionFillOpaque(final int distanceFromLikeliest) {
        final Color c = amberSuggestionShade(distanceFromLikeliest);
        return new Color(c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color amberSuggestionFillOpaqueFromWedgeOverlap(final double portion01) {
        final double t = overlapToEmphasis(Math.max(0.0, Math.min(1.0, portion01)));
        final Color strong = new Color(220, 105, 8);
        final Color light = new Color(255, 228, 170);
        final int r = (int) Math.round(light.getRed() + t * (strong.getRed() - light.getRed()));
        final int g = (int) Math.round(light.getGreen() + t * (strong.getGreen() - light.getGreen()));
        final int b = (int) Math.round(light.getBlue() + t * (strong.getBlue() - light.getBlue()));
        return new Color(r, g, b);
    }

    /** Wheel suggested highlights (composite alpha): inner vs outer-only vs distance fallback. */
    public static Color amberSuggestionFillOpaqueFromSuggestedLayers(
            final int distanceFromLikeliest,
            final OptionalDouble innerOverlap,
            final OptionalDouble outerOnlyOverlap) {
        if (innerOverlap.isPresent() && innerOverlap.getAsDouble() > 1e-9) {
            return amberSuggestionFillOpaqueFromInnerWedgeOverlap(innerOverlap.getAsDouble());
        }
        if (outerOnlyOverlap.isPresent() && outerOnlyOverlap.getAsDouble() > 1e-9) {
            return amberSuggestionFillOpaqueFromOuterOnlyWedgeOverlap(outerOnlyOverlap.getAsDouble());
        }
        return amberSuggestionFillOpaque(clampDistanceForNoWedgeOverlapShading(distanceFromLikeliest));
    }

    public static Color amberSuggestionStrokeFromSuggestedLayers(
            final int distanceFromLikeliest,
            final OptionalDouble innerOverlap,
            final OptionalDouble outerOnlyOverlap) {
        return amberSuggestionFillOpaqueFromSuggestedLayers(distanceFromLikeliest, innerOverlap, outerOnlyOverlap)
                .darker();
    }

    /** Pulls low fractions away from “almost white” so partial overlaps stay distinguishable. */
    private static double overlapToEmphasis(final double portion01) {
        return Math.pow(portion01, 0.62);
    }

    /**
     * Thin <strong>suggestion</strong> strip (under the slot-colour bar) when the slot is both a target and in the
     * suggested run: green ramp — deeper near likeliest / inner wedge overlap; a touch of green only for outer-ring overlap.
     */
    public static Color greenTargetSuggestionStrip(
            final int distanceFromLikeliest,
            final OptionalDouble innerOverlap,
            final OptionalDouble outerOnlyOverlap) {
        final double distT = Math.min(1.0, distanceFromLikeliest / 5.0);
        final Color deep = new Color(12, 102, 56);
        final Color soft = new Color(186, 236, 202);
        Color c = lerpRgb(deep, soft, distT);
        if (innerOverlap.isPresent() && innerOverlap.getAsDouble() > 1e-9) {
            final double e = overlapToEmphasisInnerWedge(Math.max(0.0, Math.min(1.0, innerOverlap.getAsDouble())));
            c = lerpRgb(c, deep, e * 0.42);
        } else if (outerOnlyOverlap.isPresent() && outerOnlyOverlap.getAsDouble() > 1e-9) {
            final double e = overlapToEmphasisOuterRing(Math.max(0.0, Math.min(1.0, outerOnlyOverlap.getAsDouble())));
            c = lerpRgb(c, deep, e * 0.12);
        }
        return c;
    }

    private static Color lerpRgb(final Color a, final Color b, final double t) {
        final double u = Math.max(0.0, Math.min(1.0, t));
        final int r = (int) Math.round(a.getRed() + u * (b.getRed() - a.getRed()));
        final int g = (int) Math.round(a.getGreen() + u * (b.getGreen() - a.getGreen()));
        final int bl = (int) Math.round(a.getBlue() + u * (b.getBlue() - a.getBlue()));
        return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, bl)));
    }

    public static Color amberSuggestionStroke(final int distanceFromLikeliest) {
        return amberSuggestionFillOpaque(distanceFromLikeliest).darker();
    }

    public static Color amberSuggestionStrokeFromWedgeOverlap(final double portion01) {
        return amberSuggestionFillOpaqueFromWedgeOverlap(portion01).darker();
    }
}
