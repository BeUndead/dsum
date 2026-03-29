package com.com.poke.rng.dsum.model.view;

import javax.swing.*;
import javax.accessibility.AccessibleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Centered “shutter” grip: rounded pill with ridges and a small chevron (up = expanded / will collapse upward).
 */
public final class RouteSettingsBlindsHandle extends JComponent {

    private static final int DEF_W = 88;
    private static final int DEF_H = 24;

    private boolean expanded = true;
    private boolean hover = false;

    public RouteSettingsBlindsHandle(final Runnable onActivate) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false);
        updateToolTipAndA11y();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    onActivate.run();
                }
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                hover = false;
                repaint();
            }
        });
        SwingUtilities.invokeLater(
                () -> {
                    final AccessibleContext late = getAccessibleContext();
                    if (late != null) {
                        late.setAccessibleName(
                                expanded ? "Collapse route and settings" : "Expand route and settings");
                    }
                });
    }

    public void setExpanded(final boolean expanded) {
        this.expanded = expanded;
        updateToolTipAndA11y();
        repaint();
    }

    private void updateToolTipAndA11y() {
        setToolTipText(expanded
                ? "Collapse route and toolbar controls"
                : "Expand route and toolbar controls");
        final AccessibleContext ac = getAccessibleContext();
        if (ac != null) {
            ac.setAccessibleName(
                    expanded ? "Collapse route and settings" : "Expand route and settings");
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(DEF_W, DEF_H);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(DEF_W, DEF_H);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(DEF_W, DEF_H);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            final int w = getWidth();
            final int h = getHeight();
            final Color base = UiTheme.SURFACE_ALT;
            if (hover) {
                g2.setColor(base);
            } else {
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 72));
            }
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h * 0.5f, h * 0.5f));

            final Color fg = hover ? UiTheme.TEXT_PRIMARY : UiTheme.TEXT_MUTED;
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            final float cx = w / 2f;
            final float ridgeW = 36f;
            final float xL = cx - ridgeW / 2f;
            final float xR = cx + ridgeW / 2f;

            if (expanded) {
                final float tipY = 5f;
                final float baseY = 9f;
                g2.draw(new Line2D.Float(cx - 5f, baseY, cx, tipY));
                g2.draw(new Line2D.Float(cx, tipY, cx + 5f, baseY));
                final float y0 = 13f;
                for (int i = 0; i < 3; i++) {
                    final float y = y0 + i * 3.2f;
                    g2.draw(new Line2D.Float(xL, y, xR, y));
                }
            } else {
                final float y0 = 5f;
                for (int i = 0; i < 3; i++) {
                    final float y = y0 + i * 3.2f;
                    g2.draw(new Line2D.Float(xL, y, xR, y));
                }
                final float baseY = 17f;
                final float tipY = 21f;
                g2.draw(new Line2D.Float(cx - 5f, baseY, cx, tipY));
                g2.draw(new Line2D.Float(cx, tipY, cx + 5f, baseY));
            }
        } finally {
            g2.dispose();
        }
    }
}
