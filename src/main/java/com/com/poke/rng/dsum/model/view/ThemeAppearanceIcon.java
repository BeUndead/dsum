package com.com.poke.rng.dsum.model.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

/** Toolbar icon for light (sun) or dark (moon) appearance. */
public final class ThemeAppearanceIcon implements Icon {

    public enum Kind {
        SUN,
        MOON
    }

    public static final int SIZE = 22;

    private final Kind kind;

    public ThemeAppearanceIcon(final Kind kind) {
        this.kind = kind;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        final Graphics2D g2 = (Graphics2D) g.create(x, y, SIZE, SIZE);
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            final boolean selected = c instanceof AbstractButton b && b.getModel().isSelected();
            final Color fg = c.isEnabled()
                    ? (selected ? UiTheme.ACCENT : UiTheme.TEXT_PRIMARY)
                    : UiTheme.TEXT_MUTED;
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (kind == Kind.SUN) {
                paintSun(g2);
            } else {
                paintMoon(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    private static void paintSun(final Graphics2D g2) {
        final float cx = SIZE / 2f;
        final float cy = SIZE / 2f;
        final float r = 3.2f;
        g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        for (int i = 0; i < 8; i++) {
            final double a = i * Math.PI / 4;
            final float x1 = cx + (float) Math.cos(a) * 4.8f;
            final float y1 = cy - (float) Math.sin(a) * 4.8f;
            final float x2 = cx + (float) Math.cos(a) * 7.2f;
            final float y2 = cy - (float) Math.sin(a) * 7.2f;
            g2.draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));
        }
    }

    private static void paintMoon(final Graphics2D g2) {
        final float cx = SIZE / 2f + 0.5f;
        final float cy = SIZE / 2f;
        final Shape disk = new Ellipse2D.Float(cx - 5.5f, cy - 5.5f, 11f, 11f);
        final Shape bite = new Ellipse2D.Float(cx - 1.5f, cy - 5.5f, 9.5f, 11f);
        final Area moon = new Area(disk);
        moon.subtract(new Area(bite));
        g2.fill(moon);
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
