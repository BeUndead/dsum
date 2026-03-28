package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.model.OverworldMovementMode;

import javax.swing.*;
import java.awt.*;

/**
 * Small line-art icon for each {@link OverworldMovementMode} (toolbar-style toggles).
 */
public final class MovementModeIcon implements Icon {

    public static final int SIZE = 24;

    private final OverworldMovementMode mode;

    public MovementModeIcon(final OverworldMovementMode mode) {
        this.mode = mode;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        final Graphics2D g2 = (Graphics2D) g.create(x, y, SIZE, SIZE);
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            final boolean selected =
                    c instanceof AbstractButton b && b.getModel().isSelected();
            final Color fg = c.isEnabled()
                    ? (selected ? UiTheme.ACCENT : UiTheme.TEXT_PRIMARY)
                    : UiTheme.TEXT_MUTED;
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (mode) {
                case CORNER_BONK -> paintCornerBonk(g2);
                case BIKE -> paintBike(g2);
                case WALKING -> paintWalking(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    /** Inner corner with arrows along each wall (bounce directions). */
    private static void paintCornerBonk(final Graphics2D g2) {
        final float s = SIZE;
        // Walls meeting at inner corner (bottom-right of the “room” pocket)
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.65f, s * 0.18f, s * 0.65f, s * 0.58f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.65f, s * 0.58f, s * 0.88f, s * 0.58f));
        // Arrows from inside toward walls
        arrowHead(g2, s * 0.65f, s * 0.32f, s * 0.65f, s * 0.22f);
        arrowHead(g2, s * 0.78f, s * 0.58f, s * 0.86f, s * 0.58f);
    }

    private static void arrowHead(
            final Graphics2D g2, final float x1, final float y1, final float tipX, final float tipY) {
        final double ang = Math.atan2(tipY - y1, tipX - x1);
        final float wing = 4f;
        final double a1 = ang + Math.PI * 0.8;
        final double a2 = ang - Math.PI * 0.8;
        g2.draw(new java.awt.geom.Line2D.Float(tipX, tipY, tipX + (float) (Math.cos(a1) * wing), tipY + (float) (Math.sin(a1) * wing)));
        g2.draw(new java.awt.geom.Line2D.Float(tipX, tipY, tipX + (float) (Math.cos(a2) * wing), tipY + (float) (Math.sin(a2) * wing)));
    }

    private static void paintBike(final Graphics2D g2) {
        final float s = SIZE;
        g2.drawOval(Math.round(s * 0.12f), Math.round(s * 0.48f), Math.round(s * 0.34f), Math.round(s * 0.34f));
        g2.drawOval(Math.round(s * 0.54f), Math.round(s * 0.48f), Math.round(s * 0.34f), Math.round(s * 0.34f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.28f, s * 0.62f, s * 0.48f, s * 0.38f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.48f, s * 0.38f, s * 0.72f, s * 0.38f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.72f, s * 0.38f, s * 0.78f, s * 0.62f));
    }

    private static void paintWalking(final Graphics2D g2) {
        final float s = SIZE;
        // Simple stride: two legs from a small “body”
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.48f, s * 0.2f, s * 0.48f, s * 0.45f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.48f, s * 0.45f, s * 0.28f, s * 0.82f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.48f, s * 0.45f, s * 0.68f, s * 0.82f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.48f, s * 0.28f, s * 0.62f, s * 0.22f));
        g2.draw(new java.awt.geom.Line2D.Float(s * 0.48f, s * 0.28f, s * 0.34f, s * 0.22f));
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
