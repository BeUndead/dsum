package com.com.poke.rng.dsum.model.view;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/** Speaker glyph with optional “sound waves” — typical volume-on / volume-off (muted) toolbar imagery. */
public final class VolumeToolbarIcon implements Icon {

    private static final int SIZE = 20;

    private final boolean muted;

    public VolumeToolbarIcon(final boolean muted) {
        this.muted = muted;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        final Graphics2D g2 = (Graphics2D) g.create(x, y, SIZE, SIZE);
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final Color fg = c != null ? c.getForeground() : UiTheme.TEXT_PRIMARY;
            g2.setColor(fg);

            final Path2D speaker = new Path2D.Float(Path2D.WIND_NON_ZERO);
            speaker.moveTo(1.5f, 7f);
            speaker.lineTo(5.5f, 7f);
            speaker.lineTo(10.5f, 3.5f);
            speaker.lineTo(10.5f, 16.5f);
            speaker.lineTo(5.5f, 13f);
            speaker.lineTo(1.5f, 13f);
            speaker.closePath();
            g2.fill(speaker);

            if (!muted) {
                // Thinner stroke so three parallels don’t visually fuse at 20×20
                g2.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setPaint(fg);
                // Same ellipse center (14, 10); each wave is a larger oval so gaps read clearly
                g2.draw(new Arc2D.Double(10.5, 6, 7, 8, -62, 124, Arc2D.OPEN));
                g2.draw(new Arc2D.Double(9.5, 4.5, 9, 11, -60, 120, Arc2D.OPEN));
                g2.draw(new Arc2D.Double(8.5, 2.5, 11, 15, -58, 116, Arc2D.OPEN));
            }
        } finally {
            g2.dispose();
        }
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
