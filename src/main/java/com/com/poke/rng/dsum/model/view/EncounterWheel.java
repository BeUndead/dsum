package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.model.EncounterWheelModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;

public final class EncounterWheel extends JPanel {

    private static final int PREFERRED_SIZE = 500;
    private static final int DSUM_RANGE = EncounterWheelModel.DSUM_RANGE;

    private static final int WHEEL_OUTER_RADIUS = 220;
    private static final int WHEEL_INNER_RADIUS = 140;
    private static final int TICK_RADIUS_INNER = 225;
    private static final int TICK_RADIUS_OUTER = 235;
    private static final int ARROW_RADIUS = 200;
    private static final int ARROW_HEAD_HALF_WIDTH = 20;
    private static final int ARROW_HEAD_HEIGHT = 40;
    private static final int ARROW_STEM_TOP_OFFSET = 40;
    private static final int ARROW_STEM_BOTTOM_OFFSET = 260;

    private static final double PULSE_PERIOD_SEC = 2;
    private static final double PULSE_SIZE_FRACTION = 0.05;
    private static final float PULSE_BRIGHTNESS_AMOUNT = 1f;

    private final EncounterWheelModel model;

    public EncounterWheel(final EncounterWheelModel model) {
        this.model = model;
        setPreferredSize(new Dimension(PREFERRED_SIZE, PREFERRED_SIZE));
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                SwingUtilities.invokeLater(() -> requestFocus());
            }
        });
    }

    private static Color pulseColor(final Color color, final float amount) {
        float t = amount * PULSE_BRIGHTNESS_AMOUNT;
        int r = (int) (color.getRed() + (255 - color.getRed()) * t);
        int g = (int) (color.getGreen() + (255 - color.getGreen()) * t);
        int b = (int) (color.getBlue() + (255 - color.getBlue()) * t);

        return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b))
        );
    }

    private static Color colorFor(final EncounterSlot slot) {
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

    public EncounterWheelModel getModel() {
        return model;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        final int cx = getWidth() / 2;
        final int cy = getHeight() / 2;

        if (model.targetOverlapsUncertainty()) {
            g2.setColor(new Color(200, 255, 200));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.setColor(Color.BLACK);
        drawTicks(g2, cx, cy);

        AffineTransform old = g2.getTransform();
        g2.rotate(Math.toRadians(model.getAngleDeg()), cx, cy);
        drawSlots(g2, cx, cy);
        g2.setTransform(old);

        drawUncertaintyWedge(g2, cx, cy);
        drawArrow(g2, cx, cy);
    }

    private void drawSlots(final Graphics2D g, final int cx, final int cy) {
        double pulse = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1e9 * 2 * Math.PI / PULSE_PERIOD_SEC);

        for (EncounterSlot slot : EncounterSlot.values()) {
            final int width = slot.max() - slot.min() + 1;
            final double startDeg = (slot.min() / (double) DSUM_RANGE) * 360 + 90;
            final double extentDeg = (width / (double) DSUM_RANGE) * 360;

            int outerR = WHEEL_OUTER_RADIUS;
            int innerR = WHEEL_INNER_RADIUS;
            Color color = colorFor(slot);

            if (model.getTargetSlots().contains(slot)) {
                final double sizeScale = 1.0 + PULSE_SIZE_FRACTION * (2 * pulse - 1);
                outerR = (int) (WHEEL_OUTER_RADIUS * sizeScale);
                innerR = (int) (WHEEL_INNER_RADIUS * sizeScale);
                color = pulseColor(color.brighter(), (float) pulse);
            }

            final Shape slice = createRingSlice(cx, cy, outerR, innerR, startDeg, extentDeg, false);
            final GradientPaint gradient =
                    new GradientPaint(cx, cy - outerR, color.brighter(), cx, cy + outerR, color.darker());
            g.setPaint(gradient);
            g.fill(slice);
            g.setColor(Color.BLACK);
            g.draw(slice);
        }
    }

    private void drawUncertaintyWedge(final Graphics2D g, final int cx, final int cy) {
        if (model.getCalibratedSlot() == null) {
            return;
        }

        final double extentDeg = model.getUncertaintyWedgeExtentDeg();
        final double startDeg = 90 - extentDeg / 2;
        final Shape wedge =
                createRingSlice(cx, cy, WHEEL_OUTER_RADIUS + 5, WHEEL_INNER_RADIUS - 5, startDeg, extentDeg, false);

        g.setColor(new Color(255, 255, 255, 200));
        g.fill(wedge);
        g.setColor(new Color(0, 0, 0, 60));
        g.draw(wedge);
    }

    private void drawArrow(final Graphics2D g, final int cx, final int cy) {
        g.setStroke(new BasicStroke(16));
        g.setColor(Color.RED);

        final int tipY = cy - ARROW_RADIUS;
        g.drawLine(cx, tipY - ARROW_STEM_BOTTOM_OFFSET, cx, tipY - ARROW_STEM_TOP_OFFSET);

        final Polygon head = new Polygon();
        head.addPoint(cx, tipY);
        head.addPoint(cx - ARROW_HEAD_HALF_WIDTH, tipY - ARROW_HEAD_HEIGHT);
        head.addPoint(cx + ARROW_HEAD_HALF_WIDTH, tipY - ARROW_HEAD_HEIGHT);
        g.fill(head);
    }

    private void drawTicks(final Graphics2D g, final int cx, final int cy) {
        for (int i = 0; i < DSUM_RANGE; i += 4) {
            final double angle = Math.toRadians(i / (double) DSUM_RANGE * 360 - 90);
            final int x1 = cx + (int) (Math.cos(angle) * TICK_RADIUS_INNER);
            final int y1 = cy + (int) (Math.sin(angle) * TICK_RADIUS_INNER);
            final int x2 = cx + (int) (Math.cos(angle) * TICK_RADIUS_OUTER);
            final int y2 = cy + (int) (Math.sin(angle) * TICK_RADIUS_OUTER);

            g.setStroke(i % 16 == 0 ? new BasicStroke(3) : new BasicStroke(2));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private Shape createRingSlice(final int cx,
                                  final int cy,
                                  final int outer,
                                  final int inner,
                                  final double start,
                                  final double extent,
                                  final boolean subtractInner) {
        final Arc2D outerArc = new Arc2D.Double(cx - outer, cy - outer, outer * 2, outer * 2, start, extent, Arc2D.PIE);
        final Area area = new Area(outerArc);

        if (subtractInner) {
            final Arc2D innerArc = new Arc2D.Double(
                    cx - inner + 0.5,
                    cy - inner + 0.5,
                    (inner - 0.5) * 2,
                    (inner - 0.5) * 2,
                    start - 50,
                    extent + 50,
                    Arc2D.PIE
            );
            area.subtract(new Area(innerArc));
        }

        return area;
    }
}
