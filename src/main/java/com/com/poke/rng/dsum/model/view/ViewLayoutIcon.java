package com.com.poke.rng.dsum.model.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Toolbar icon for full (square wheel) vs compact (thin strip) layout. */
public final class ViewLayoutIcon implements Icon {

    public enum Kind {
        /** Larger, nearly square region — full wheel view. */
        FULL,
        /** Shorter rectangle — compact strip view. */
        COMPACT
    }

    public static final int SIZE = 22;

    private final Kind kind;

    public ViewLayoutIcon(final Kind kind) {
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
            if (kind == Kind.FULL) {
                paintFull(g2);
            } else {
                paintCompact(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    private static void paintFull(final Graphics2D g2) {
        final float pad = 3f;
        final float w = SIZE - 2f * pad;
        final float h = w;
        g2.draw(new RoundRectangle2D.Float(pad, pad, w, h, 3.5f, 3.5f));
    }

    private static void paintCompact(final Graphics2D g2) {
        final float padX = 2.5f;
        final float w = SIZE - 2f * padX;
        final float h = 5f;
        final float y = (SIZE - h) / 2f;
        g2.draw(new RoundRectangle2D.Float(padX, y, w, h, 2f, 2f));
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
