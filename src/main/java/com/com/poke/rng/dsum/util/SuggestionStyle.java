package com.com.poke.rng.dsum.util;

import java.awt.*;

/** Shared colours and layout math for suggested-slot runs (sprite row, wheel, bar). */
public final class SuggestionStyle {

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

    /** Opaque RGB for wheel/bar overlays using {@link AlphaComposite}. */
    public static Color amberSuggestionFillOpaque(final int distanceFromLikeliest) {
        final Color c = amberSuggestionShade(distanceFromLikeliest);
        return new Color(c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color amberSuggestionStroke(final int distanceFromLikeliest) {
        return amberSuggestionFillOpaque(distanceFromLikeliest).darker();
    }
}
