package com.com.poke.rng.dsum.model.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.com.poke.rng.dsum.util.SlotPalette;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

/** Shared palette and global LAF (FlatLaf). Default appearance is dark. */
public final class UiTheme {

    public enum Appearance {
        LIGHT,
        DARK
    }

    private static Appearance appearance = Appearance.DARK;

    public static Color SURFACE;
    public static Color SURFACE_ALT;
    public static Color TEXT_PRIMARY;
    public static Color TEXT_MUTED;
    public static Color ACCENT;

    /** Muted slot tiles blend toward this instead of white (dark = charcoal). */
    public static Color SLOT_MUTED_BLEND_TARGET;

    public static Color TOGGLE_OFF_BG;
    public static Color TOGGLE_OFF_BORDER;
    public static Color TOGGLE_ON_BG;
    public static Color TOGGLE_ON_BORDER;

    public static Color CHIP_BG;
    public static Color CHIP_BORDER;
    public static Color APPROACH_BAR_BG;
    public static Color APPROACH_BAR_BORDER;

    public static Color TICK_GRAY;
    public static Color SLOT_SLICE_EDGE;
    public static Color SUGGESTION_GOLD_FILL;
    public static Color SUGGESTION_GOLD_STROKE;
    public static Color UNCERTAINTY_FILL;
    public static Color UNCERTAINTY_STROKE;
    /** “If Space now” encounter DSum debug wedge on the wheel (toggle with F2). */
    public static Color DEBUG_SPACE_ENCOUNTER_FILL;
    public static Color DEBUG_SPACE_ENCOUNTER_STROKE;
    /** Full-area tint while waiting for calibration digit (in battle). */
    public static Color CALIBRATING_SURFACE_WASH;
    /** Subtle warm tint on overworld when no slot has been calibrated yet (distinct from calibrating blue). */
    public static Color UNCALIBRATED_SURFACE_WASH;
    /** Slot number text halo / outline. */
    public static Color LABEL_HALO;
    public static Color SELECTOR_DIVIDER;

    private static int or0;
    private static int og0;
    private static int ob0;
    private static int or1;
    private static int og1;
    private static int ob1;

    private UiTheme() {
    }

    public static Appearance getAppearance() {
        return appearance;
    }

    public static boolean isDark() {
        return appearance == Appearance.DARK;
    }

    public static void install() {
        install(Appearance.DARK);
    }

    public static void install(final Appearance app) {
        appearance = app;
        if (app == Appearance.DARK) {
            applyDarkPalette();
        } else {
            applyLightPalette();
        }
        SlotPalette.setMutedBlendTarget(SLOT_MUTED_BLEND_TARGET);

        try {
            UIManager.setLookAndFeel(app == Appearance.DARK ? new FlatDarkLaf() : new FlatLightLaf());
        } catch (final Exception e) {
            fallbackLookAndFeel();
        }

        UIManager.put("defaultFont", new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 13));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ComboBox.padding", new Insets(4, 8, 4, 8));
        UIManager.put("Spinner.padding", new Insets(4, 6, 4, 6));

        final Font uiFont = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        final Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            final Object key = keys.nextElement();
            if (key instanceof String && ((String) key).endsWith(".font")) {
                UIManager.put(key, new FontUIResource(uiFont));
            }
        }
    }

    /** Green wash when target overlaps uncertainty wedge (full panel tint). */
    public static Color overlapUncertaintyWash(final double portion) {
        final double p = Math.max(0.0, Math.min(1.0, portion));
        return new Color(
                lerpRgb(or0, or1, p),
                lerpRgb(og0, og1, p),
                lerpRgb(ob0, ob1, p)
        );
    }

    /** Inner fill for the overlap approach bar when not in green “overlap” hum state. */
    public static Color approachCountdownNonOverlapFill(final double progress) {
        final double p = Math.max(0.0, Math.min(1.0, progress));
        if (appearance == Appearance.DARK) {
            return new Color(
                    (int) (100 + 55 * (1 - p)),
                    (int) (72 + 75 * p),
                    (int) (42 + 28 * (1 - p))
            );
        }
        return new Color(
                (int) (230 + 25 * (1 - p)),
                (int) (140 + 95 * p),
                (int) (55 + 35 * (1 - p))
        );
    }

    private static void applyDarkPalette() {
        SURFACE = new Color(0x1e, 0x21, 0x27);
        SURFACE_ALT = new Color(0x25, 0x28, 0x30);
        TEXT_PRIMARY = new Color(0xea, 0xed, 0xf3);
        TEXT_MUTED = new Color(0x9a, 0xa3, 0xaf);
        ACCENT = new Color(0x7c, 0xb4, 0xff);

        SLOT_MUTED_BLEND_TARGET = new Color(0x35, 0x38, 0x42);

        TOGGLE_OFF_BG = new Color(0x2d, 0x31, 0x39);
        TOGGLE_OFF_BORDER = new Color(0xff, 0xff, 0xff, 40);
        TOGGLE_ON_BG = new Color(0x2a, 0x3d, 0x5c);
        TOGGLE_ON_BORDER = new Color(0x6d, 0xa8, 0xff);

        CHIP_BG = new Color(0x2d, 0x31, 0x39, 0xf5);
        CHIP_BORDER = new Color(0x55, 0x5f, 0x70);
        APPROACH_BAR_BG = new Color(0x35, 0x3a, 0x45);
        APPROACH_BAR_BORDER = new Color(0x4d, 0x56, 0x68);

        TICK_GRAY = new Color(0x7d, 0x86, 0x97);
        SLOT_SLICE_EDGE = new Color(0, 0, 0, 90);
        SUGGESTION_GOLD_FILL = new Color(0xff, 0xd7, 0x30);
        SUGGESTION_GOLD_STROKE = new Color(0xc9, 0x8a, 0x0, 0xc8);
        // Wedge over slot colours — was easy to lose on dark surfaces; stronger fill + light rim read better than feint white/alpha.
        UNCERTAINTY_FILL = new Color(0xe8, 0xee, 0xfb, 0x58);
        UNCERTAINTY_STROKE = new Color(0xff, 0xff, 0xff, 0xc0);
        DEBUG_SPACE_ENCOUNTER_FILL = new Color(0x4a, 0x9e, 0xff, 0x40);
        DEBUG_SPACE_ENCOUNTER_STROKE = new Color(0x6d, 0xc0, 0xff, 0xb0);
        CALIBRATING_SURFACE_WASH = new Color(0x6d, 0xa8, 0xff, 0x22);
        UNCALIBRATED_SURFACE_WASH = new Color(0xf5, 0xa4, 0x23, 0x18);
        LABEL_HALO = new Color(0x0, 0x0, 0x0, 0xd0);
        SELECTOR_DIVIDER = new Color(0xff, 0xff, 0xff, 0x10);

        or0 = 0x28;
        og0 = 0x3a;
        ob0 = 0x30;
        or1 = 0x20;
        og1 = 0xc6;
        ob1 = 0x4e;
    }

    private static void applyLightPalette() {
        SURFACE = new Color(248, 249, 251);
        SURFACE_ALT = new Color(242, 244, 247);
        TEXT_PRIMARY = new Color(33, 37, 41);
        TEXT_MUTED = new Color(108, 117, 125);
        ACCENT = new Color(37, 99, 235);

        SLOT_MUTED_BLEND_TARGET = Color.WHITE;

        TOGGLE_OFF_BG = Color.WHITE;
        TOGGLE_OFF_BORDER = new Color(0, 0, 0, 32);
        TOGGLE_ON_BG = new Color(224, 235, 255);
        TOGGLE_ON_BORDER = new Color(37, 99, 235);

        CHIP_BG = new Color(255, 255, 255, 246);
        CHIP_BORDER = new Color(220, 224, 230);
        APPROACH_BAR_BG = new Color(233, 236, 239);
        APPROACH_BAR_BORDER = new Color(199, 204, 211);

        TICK_GRAY = new Color(154, 160, 168);
        SLOT_SLICE_EDGE = new Color(55, 58, 65, 100);
        SUGGESTION_GOLD_FILL = new Color(255, 215, 48);
        SUGGESTION_GOLD_STROKE = new Color(180, 120, 0, 200);
        UNCERTAINTY_FILL = new Color(248, 250, 255, 228);
        UNCERTAINTY_STROKE = new Color(55, 65, 85, 110);
        DEBUG_SPACE_ENCOUNTER_FILL = new Color(37, 99, 235, 52);
        DEBUG_SPACE_ENCOUNTER_STROKE = new Color(29, 78, 216, 200);
        CALIBRATING_SURFACE_WASH = new Color(0x3b, 0x82, 0xf6, 0x2a);
        UNCALIBRATED_SURFACE_WASH = new Color(0xc2, 0x7a, 0x00, 0x14);
        LABEL_HALO = new Color(255, 255, 255, 210);
        SELECTOR_DIVIDER = new Color(0, 0, 0, 8);

        or0 = 212;
        og0 = 255;
        ob0 = 212;
        or1 = 32;
        og1 = 198;
        ob1 = 78;
    }

    private static int lerpRgb(final int a, final int b, final double t) {
        return (int) Math.round(a + (b - a) * Math.max(0.0, Math.min(1.0, t)));
    }

    private static void fallbackLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (final Exception ignored) {
                // keep default
            }
        }
    }
}
