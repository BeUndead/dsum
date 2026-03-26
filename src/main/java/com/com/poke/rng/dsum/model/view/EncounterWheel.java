package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.util.SlotPalette;
import com.com.poke.rng.dsum.util.SuggestionStyle;
import com.com.poke.rng.dsum.util.Triplet;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

public final class EncounterWheel extends JPanel {

    private static final int PREFERRED_SIZE = 500;
    /** Baseline wheel size at {@link #geomScale} {@code 1.0}. */
    public static final double DEFAULT_GEOM_SCALE = 1.0;
    /** Scale used in minimal view (smaller window footprint). */
    public static final double COMPACT_GEOM_SCALE = 0.52;
    private static final int DSUM_RANGE = EncounterWheelModel.DSUM_RANGE;

    private static final int WHEEL_OUTER_RADIUS = 220;
    private static final int WHEEL_INNER_RADIUS = 140;
    private static final int TICK_RADIUS_INNER = 225;
    private static final int TICK_RADIUS_OUTER = 235;
    /** Slot number labels: radius as fraction of {@link #WHEEL_OUTER_RADIUS} (larger = farther out on the ring). */
    private static final double SLOT_LABEL_RADIUS_FRAC = 0.78;
    private static final int ARROW_RADIUS = 200;
    private static final int ARROW_HEAD_HALF_WIDTH = 20;
    private static final int ARROW_HEAD_HEIGHT = 40;
    private static final int ARROW_STEM_TOP_OFFSET = 40;
    private static final int ARROW_STEM_BOTTOM_OFFSET = 260;

    private static final double PULSE_PERIOD_SEC = 2;
    private static final double PULSE_SIZE_FRACTION = 0.05;
    private static final float PULSE_BRIGHTNESS_AMOUNT = 1f;

    private final EncounterWheelModel model;
    private double geomScale = DEFAULT_GEOM_SCALE;

    public EncounterWheel(final EncounterWheelModel model) {
        this(model, DEFAULT_GEOM_SCALE);
    }

    public EncounterWheel(final EncounterWheelModel model, final double initialGeomScale) {
        this.model = model;
        setGeomScale(initialGeomScale);
        setOpaque(true);
        setBackground(UiTheme.SURFACE);
        setFocusable(true);
        DsumEncounterPaint.installClickToFocus(this);
    }

    public double getGeomScale() {
        return geomScale;
    }

    /**
     * Uniform scale for all wheel geometry (radii, strokes, fonts). Clamped for sanity; updates preferred size.
     */
    public void setGeomScale(final double geomScale) {
        this.geomScale = Math.max(0.35, Math.min(1.0, geomScale));
        setPreferredSize(new Dimension(gs(PREFERRED_SIZE), gs(PREFERRED_SIZE)));
    }

    /** Pixel diameter matching {@link #setGeomScale(double)} for lining up adjacent panes (e.g. slot strip width). */
    public static int diameterForGeomScale(final double geomScale) {
        final double g = Math.max(0.35, Math.min(1.0, geomScale));
        return (int) Math.round(PREFERRED_SIZE * g);
    }

    private int gs(final int px) {
        return (int) Math.round(px * geomScale);
    }

    private float gsf(final float px) {
        return (float) (px * geomScale);
    }

    private static final int OVERLAP_COUNTDOWN_BAR_W = 196;
    private static final int OVERLAP_COUNTDOWN_BAR_H = 11;
    private static final int OVERLAP_COUNTDOWN_TOP = 10;

    private void drawTargetOverlapCountdownBar(final Graphics2D g2, final int cx) {
        if (model.getCalibratedSlot() == null || model.getTargetSlots().isEmpty() || model.isCalibrating()) {
            return;
        }

        final int barW = gs(OVERLAP_COUNTDOWN_BAR_W);
        final int barH = gs(OVERLAP_COUNTDOWN_BAR_H);
        final int x = cx - barW / 2;
        final int y = gs(OVERLAP_COUNTDOWN_TOP);
        final int arc = Math.max(2, gs(6));
        DsumEncounterPaint.paintApproachCountdownBar(
                g2,
                model,
                x,
                y,
                barW,
                barH,
                arc,
                gsf(1f),
                Math.max(1, gs(3)),
                Math.max(2, gs(4)));
    }

    public EncounterWheelModel getModel() {
        return model;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setPaint(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        final int cx = getWidth() / 2;
        final int cy = getHeight() / 2;

        if (model.targetOverlapsUncertainty()) {
            g2.setColor(UiTheme.overlapUncertaintyWash(model.getTargetUncertaintyOverlapPortionOfSlot()));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        drawTargetOverlapCountdownBar(g2, cx);

        g2.setColor(UiTheme.TICK_GRAY);
        drawTicks(g2, cx, cy);

        AffineTransform old = g2.getTransform();
        g2.rotate(Math.toRadians(model.getDisplayAngleDeg()), cx, cy);
        drawSlots(g2, cx, cy);
        drawSuggestedRangeHighlight(g2, cx, cy);
        g2.setTransform(old);
        drawSlotLabels(g2, cx, cy);

        drawUncertaintyWedge(g2, cx, cy);
        drawArrow(g2, cx, cy);
        drawText(g2);
    }

    private void drawSlots(final Graphics2D g, final int cx, final int cy) {
        double pulse = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1e9 * 2 * Math.PI / PULSE_PERIOD_SEC);

        for (EncounterSlot slot : EncounterSlot.values()) {
            final int width = slot.max() - slot.min() + 1;
            final double startDeg = (slot.min() / (double) DSUM_RANGE) * 360 + 90;
            final double extentDeg = (width / (double) DSUM_RANGE) * 360;

            int outerR = gs(WHEEL_OUTER_RADIUS);
            int innerR = gs(WHEEL_INNER_RADIUS);
            Color color = SlotPalette.fillColor(slot);

            if (model.getTargetSlots().contains(slot)) {
                final double sizeScale = 1.0 + PULSE_SIZE_FRACTION * (2 * pulse - 1);
                outerR = (int) Math.round(gs(WHEEL_OUTER_RADIUS) * sizeScale);
                innerR = (int) Math.round(gs(WHEEL_INNER_RADIUS) * sizeScale);
                color = DsumEncounterPaint.pulseColor(color.brighter(), (float) pulse, PULSE_BRIGHTNESS_AMOUNT);
            }

            final Shape slice = createRingSlice(cx, cy, outerR, innerR, startDeg, extentDeg, false);
            final GradientPaint gradient =
                    new GradientPaint(cx, cy - outerR, color.brighter(), cx, cy + outerR, color.darker());
            g.setPaint(gradient);
            g.fill(slice);
            g.setColor(UiTheme.SLOT_SLICE_EDGE);
            g.setStroke(new BasicStroke(gsf(1f)));
            g.draw(slice);
        }
    }

    private void drawSuggestedRangeHighlight(final Graphics2D g, final int cx, final int cy) {
        final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> t = model.getSuggestedSlots();
        if (t == null) {
            return;
        }
        final int firstIndex = t.first().ordinal();
        final int likeliestIndex = t.second().ordinal();
        final int end = t.third().ordinal();
        final int n = EncounterSlot.values().length;
        final boolean overlap = model.targetOverlapsUncertainty();
        final double oPortion = model.getTargetUncertaintyOverlapPortionOfSlot();

        final Composite oldC = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));
        int idx = firstIndex;
        while (true) {
            final EncounterSlot slot = EncounterSlot.values()[idx];
            final int width = slot.max() - slot.min() + 1;
            final double startDeg = (slot.min() / (double) DSUM_RANGE) * 360 + 90;
            final double extentDeg = (width / (double) DSUM_RANGE) * 360;
            final Shape slice =
                    createRingSlice(cx, cy, gs(WHEEL_OUTER_RADIUS + 6), gs(WHEEL_INNER_RADIUS - 6),
                            startDeg, extentDeg, false);
            if (overlap) {
                g.setColor(DsumEncounterPaint.overlapStrengthGreen(115, 235, 125, 42, 185, 62, oPortion));
            } else {
                final int d = SuggestionStyle.segmentDistanceFromLikeliest(firstIndex, end, likeliestIndex, idx, n);
                g.setColor(SuggestionStyle.amberSuggestionFillOpaque(d));
            }
            g.fill(slice);
            if (idx == end) {
                break;
            }
            idx = (idx + 1) % n;
        }
        g.setComposite(oldC);

        g.setStroke(new BasicStroke(gsf(2f)));
        idx = firstIndex;
        while (true) {
            final EncounterSlot slot = EncounterSlot.values()[idx];
            final int width = slot.max() - slot.min() + 1;
            final double startDeg = (slot.min() / (double) DSUM_RANGE) * 360 + 90;
            final double extentDeg = (width / (double) DSUM_RANGE) * 360;
            final Shape slice =
                    createRingSlice(cx, cy, gs(WHEEL_OUTER_RADIUS + 6), gs(WHEEL_INNER_RADIUS - 6),
                            startDeg, extentDeg, false);
            if (overlap) {
                g.setColor(DsumEncounterPaint.overlapStrengthGreen(28, 135, 55, 8, 95, 28, oPortion, 185, 240));
            } else {
                final int d = SuggestionStyle.segmentDistanceFromLikeliest(firstIndex, end, likeliestIndex, idx, n);
                g.setColor(SuggestionStyle.amberSuggestionStroke(d));
            }
            g.draw(slice);
            if (idx == end) {
                break;
            }
            idx = (idx + 1) % n;
        }
    }

    private void drawSlotLabels(final Graphics2D g, final int cx, final int cy) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, gsf(13f)));
        final FontMetrics fm = g.getFontMetrics();
        final int labelR = (int) (gs(WHEEL_OUTER_RADIUS) * SLOT_LABEL_RADIUS_FRAC);
        final AffineTransform wheelRotate = AffineTransform.getRotateInstance(Math.toRadians(model.getDisplayAngleDeg()));
        for (final EncounterSlot slot : EncounterSlot.values()) {
            final int width = slot.max() - slot.min() + 1;
            final double startDeg = (slot.min() / (double) DSUM_RANGE) * 360 + 90;
            final double extentDeg = (width / (double) DSUM_RANGE) * 360;
            final double midArcDeg = startDeg + extentDeg / 2;
            final double rad = Math.toRadians(midArcDeg);
            final double vx = Math.cos(rad) * labelR;
            final double vy = -Math.sin(rad) * labelR;
            final Point2D offset = new Point2D.Double();
            wheelRotate.transform(new Point2D.Double(vx, vy), offset);
            final float tx = (float) (cx + offset.getX());
            final float ty = (float) (cy + offset.getY());
            final String label = Integer.toString(slot.ordinal() + 1);
            final int w = fm.stringWidth(label);
            final float baseline = ty + (fm.getAscent() - fm.getDescent()) / 2f;

            g.setColor(UiTheme.LABEL_HALO);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    if (ox != 0 || oy != 0) {
                        g.drawString(label, tx - w / 2f + ox, baseline + oy);
                    }
                }
            }
            g.setColor(UiTheme.TEXT_PRIMARY);
            g.drawString(label, tx - w / 2f, baseline);
        }
    }

    private void drawUncertaintyWedge(final Graphics2D g, final int cx, final int cy) {
        if (model.getCalibratedSlot() == null && !model.isCalibrating()) {
            return;
        }

        final double extentDeg = model.getUncertaintyWedgeExtentDeg();
        final double startDeg = 90 - extentDeg / 2;
        final Shape wedge =
                createRingSlice(cx, cy, gs(WHEEL_OUTER_RADIUS + 5), gs(WHEEL_INNER_RADIUS - 5), startDeg, extentDeg, false);

        g.setColor(UiTheme.UNCERTAINTY_FILL);
        g.fill(wedge);
        g.setColor(UiTheme.UNCERTAINTY_STROKE);
        g.setStroke(new BasicStroke(gsf(1f)));
        g.draw(wedge);
    }

    private void drawArrow(final Graphics2D g, final int cx, final int cy) {
        final Color needle = new Color(207, 68, 68);
        g.setColor(needle);
        g.setStroke(new BasicStroke(gsf(12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        final float tipY = cy - gs(ARROW_RADIUS);
        final float stemTop = tipY - gs(ARROW_STEM_TOP_OFFSET);
        final float stemBot = tipY - gs(ARROW_STEM_BOTTOM_OFFSET);
        g.drawLine(cx, (int) stemBot, cx, (int) stemTop);

        DsumEncounterPaint.paintCurvedArrowHeadDown(
                g,
                cx,
                tipY,
                gsf(ARROW_HEAD_HALF_WIDTH),
                gsf(ARROW_HEAD_HEIGHT),
                needle);
    }

    private void drawTicks(final Graphics2D g, final int cx, final int cy) {
        for (int i = 0; i < DSUM_RANGE; i += 4) {
            final double angle = Math.toRadians(i / (double) DSUM_RANGE * 360 - 90);
            final int x1 = cx + (int) (Math.cos(angle) * gs(TICK_RADIUS_INNER));
            final int y1 = cy + (int) (Math.sin(angle) * gs(TICK_RADIUS_INNER));
            final int x2 = cx + (int) (Math.cos(angle) * gs(TICK_RADIUS_OUTER));
            final int y2 = cy + (int) (Math.sin(angle) * gs(TICK_RADIUS_OUTER));

            g.setStroke(i % 16 == 0 ? new BasicStroke(gsf(3f)) : new BasicStroke(gsf(2f)));
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

    private void drawText(final Graphics2D g) {
        DsumEncounterPaint.paintDsumReadoutChip(
                g,
                model.getDsum(),
                getWidth(),
                getHeight(),
                DsumEncounterPaint.DsumReadoutMetrics.forWheel(geomScale));
    }
}
