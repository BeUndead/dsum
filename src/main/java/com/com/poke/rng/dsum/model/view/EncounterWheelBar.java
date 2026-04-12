package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.util.SlotPalette;
import com.com.poke.rng.dsum.util.SuggestionStyle;
import com.com.poke.rng.dsum.util.Triplet;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Linear, horizontally wrapping view of the DSum cycle: the same slot colours as the wheel scroll sideways and
 * re-enter from the opposite edge. The needle is fixed at the horizontal centre; strip position follows
 * {@link EncounterWheelModel#getDisplayAngleDeg()} (continuous, not integer {@link EncounterWheelModel#getDsum()},
 * so the strip scrolls smoothly — especially in battle where the cycle is slow and DSum steps are sparse).
 */
public final class EncounterWheelBar extends JPanel {

    private static final int DSUM_RANGE = EncounterWheelModel.DSUM_RANGE;
    private static final double PULSE_PERIOD_SEC = 2;
    private static final double PULSE_SIZE_FRACTION = 0.2;
    private static final float PULSE_BRIGHTNESS_WEIGHT = 1f;

    /** Default height when the bar is not shown at compact (e.g. after switching back to full). */
    public static final int DEFAULT_PREFERRED_HEIGHT = 118;

    private static final int OVERLAP_BAR_W = 196;

    /** Same cycle wrap as {@link EncounterWheelModel} DSum mapping; sub-step smooth scrolling for the strip. */
    private static double needleCycleFraction(final double angleDeg) {
        double a = angleDeg % 360.0;
        if (a < 0.0) {
            a += 360.0;
        }
        return a / 360.0;
    }

    private final EncounterWheelModel model;
    private int stripWidth = 500;
    private int barHeight = DEFAULT_PREFERRED_HEIGHT;

    public EncounterWheelBar(final EncounterWheelModel model) {
        this.model = model;
        setOpaque(true);
        setBackground(UiTheme.SURFACE);
        setPreferredSize(new Dimension(stripWidth, barHeight));
        setFocusable(true);
        DsumEncounterPaint.installClickToFocus(this);
    }

    /**
     * Width and height of the bar. Minimal layout uses {@link SlotsDisplayPanel#COMPACT_PREFERRED_HEIGHT}
     * so the bar is not taller than the sprite row.
     */
    public void setStripMetrics(final int width, final int height) {
        final int w = Math.max(200, width);
        final int h = Math.max(72, height);
        if (w == stripWidth && h == barHeight) {
            return;
        }
        stripWidth = w;
        barHeight = h;
        setPreferredSize(new Dimension(stripWidth, barHeight));
        revalidate();
    }

    public void setStripWidth(final int width) {
        setStripMetrics(width, barHeight);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setPaint(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        final int w = getWidth();
        final int h = getHeight();
        final int cx = w / 2;

        final boolean uncalibrated = model.getCalibratedSlot() == null && !model.isCalibrating();
        if (uncalibrated) {
            g2.setColor(UiTheme.UNCALIBRATED_SURFACE_WASH);
            g2.fillRect(0, 0, w, h);
        }

        if (model.isCalibrating()) {
            g2.setColor(UiTheme.CALIBRATING_SURFACE_WASH);
            g2.fillRect(0, 0, w, h);
        }

        if (model.targetOverlapsUncertainty()) {
            g2.setColor(UiTheme.overlapUncertaintyWash(model.getTargetUncertaintyOverlapPortionOfSlot()));
            g2.fillRect(0, 0, w, h);
        }

        final boolean tight = h <= SlotsDisplayPanel.COMPACT_PREFERRED_HEIGHT + 6;
        int y = tight ? 2 : 6;
        final String instructionText = model.getDsumInstructionChipText(tight);
        final boolean showInstruction = instructionText != null && !instructionText.isEmpty();
        final boolean showApproach = model.getCalibratedSlot() != null && !model.getTargetSlots().isEmpty() && !model.isCalibrating();
        final int overlapBarH = showApproach
                ? (tight ? Math.max(5, Math.min(8, h / 14)) : 11)
                : 0;
        if (showApproach) {
            y = drawTargetOverlapCountdownBar(g2, cx, y, overlapBarH, tight) + (tight ? 2 : 6);
        }
        final int tickH = tight ? 2 : Math.min(5, h / 18);
        final int tickTop = y;
        final int bandY = tickTop + tickH;
        final int instructionLineCount =
                showInstruction ? (int) instructionText.chars().filter(ch -> ch == '\n').count() + 1 : 0;
        final int chipReserve =
                (tight ? 30 : 48)
                        + (showInstruction
                                ? (tight ? 12 : 18) + instructionLineCount * (tight ? 11 : 14)
                                : 0);
        final int bandH = Math.max(tight ? 16 : 22, h - bandY - chipReserve);
        final double periodPx = Math.max(w * 2.2, 560.0);
        final double translate = cx - needleCycleFraction(model.getDisplayAngleDeg()) * periodPx;

        drawTicks(g2, translate, periodPx, w, tickTop, bandY, tight);
        drawSlotStrip(g2, translate, periodPx, w, bandY, bandH);
        drawSuggestedOverlay(g2, translate, periodPx, w, bandY, bandH);
        drawTargetSlotsOverlay(g2, translate, periodPx, w, bandY, bandH);
        drawUncertaintyBand(g2, translate, periodPx, w, bandY, bandH);
        if (!tight) {
            drawSlotLabels(g2, translate, periodPx, w, bandY + bandH + 2);
        }
        drawNeedle(g2, cx, bandY, tight);
        final DsumEncounterPaint.DsumReadoutMetrics chipMetrics = DsumEncounterPaint.DsumReadoutMetrics.forBar(tight);
        DsumEncounterPaint.paintInstructionChip(g2, instructionText, w, h, chipMetrics);
        DsumEncounterPaint.paintDsumReadoutChip(
                g2,
                model.getDsum(),
                w,
                h,
                chipMetrics,
                model.getDsumChipStateFootnote(tight));
    }

    /** @return bottom Y of the countdown bar */
    private int drawTargetOverlapCountdownBar(
            final Graphics2D g2,
            final int cx,
            final int y,
            final int barH,
            final boolean tight) {
        final int barW = Math.min(OVERLAP_BAR_W, getWidth() - 24);
        final int x = cx - barW / 2;
        final int arc = tight ? 4 : 6;
        final int inset = tight ? 2 : 3;
        final int innerArc = tight ? 3 : 4;
        DsumEncounterPaint.paintApproachCountdownBar(g2, model, x, y, barW, barH, arc, 1f, inset, innerArc);
        return y + barH;
    }

    private void drawTicks(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int tickTop,
            final int bandTop,
            final boolean tight) {
        if (tickTop >= bandTop) {
            return;
        }
        g.setColor(UiTheme.TICK_GRAY);
        final int step = tight ? 8 : 4;
        for (int i = 0; i < DSUM_RANGE; i += step) {
            final double xd = translate + (i / (double) DSUM_RANGE) * periodPx;
            for (int k = -3; k <= 3; k++) {
                final int x = (int) Math.round(xd + k * periodPx);
                if (x < -2 || x > viewW + 2) {
                    continue;
                }
                g.setStroke(i % (step * 4) == 0 ? new BasicStroke(tight ? 1.2f : 2f) : new BasicStroke(1f));
                g.drawLine(x, tickTop, x, bandTop);
            }
        }
    }

    private void drawSlotStrip(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int bandY,
            final int bandH) {
        final double pulse = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1e9 * 2 * Math.PI / PULSE_PERIOD_SEC);

        for (final EncounterSlot slot : EncounterSlot.values()) {
            final double x0 = translate + (slot.min() / (double) DSUM_RANGE) * periodPx;
            final double x1 = translate + ((slot.max() + 1) / (double) DSUM_RANGE) * periodPx;
            int baseH = bandH;
            Color color = SlotPalette.fillColor(slot);
            if (model.getTargetSlots().contains(slot)) {
                final double sizeScale = 1.0 + PULSE_SIZE_FRACTION * (2 * pulse - 1);
                baseH = Math.max(12, (int) Math.round(bandH * sizeScale));
                color = DsumEncounterPaint.pulseColor(color.brighter(), (float) pulse, PULSE_BRIGHTNESS_WEIGHT);
            }

            final int y0 = bandY + (bandH - baseH) / 2;
            for (int k = -3; k <= 3; k++) {
                final int ix0 = (int) Math.floor(x0 + k * periodPx);
                final int ix1 = (int) Math.ceil(x1 + k * periodPx);
                final int clipL = Math.max(0, ix0);
                final int clipR = Math.min(viewW, ix1);
                if (clipR <= clipL) {
                    continue;
                }
                final GradientPaint gradient = new GradientPaint(
                        0, y0, color.brighter(),
                        0, y0 + baseH, color.darker());
                g.setPaint(gradient);
                g.fillRect(clipL, y0, clipR - clipL, baseH);
                g.setColor(UiTheme.SLOT_SLICE_EDGE);
                g.setStroke(new BasicStroke(1f));
                g.drawRect(clipL, y0, clipR - clipL, baseH - 1);
            }
        }
    }

    private void drawSuggestedOverlay(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int bandY,
            final int bandH) {
        final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> t = model.getSuggestedSlots();
        if (t == null) {
            return;
        }
        // Match EncounterWheel suggested arc: omit amber overlay when the grey uncertainty band is shown (field mode).
        if (model.getCalibratedSlot() != null && !model.isCalibrating()) {
            return;
        }
        final int firstIndex = t.first().ordinal();
        final int likeliestIndex = t.second().ordinal();
        final int end = t.third().ordinal();
        final int n = EncounterSlot.values().length;
        final boolean anyWedgeInRun = model.suggestedRunHasAnyWedgeOverlap(t);
        final Composite oldC = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));

        int idx = firstIndex;
        while (true) {
            final EncounterSlot slot = EncounterSlot.values()[idx];
            final int d = SuggestionStyle.segmentDistanceFromLikeliest(firstIndex, end, likeliestIndex, idx, n);
            final OptionalDouble inner = model.encounterInnerWedgeOverlapPortionOfSlot(slot);
            final OptionalDouble outerOnly = model.encounterOuterOnlyWedgeOverlapPortionOfSlot(slot);
            final List<int[]> outerSegs = model.suggestedOuterOnlyWedgeDsumSegmentsForSlot(slot);
            final List<int[]> innerSegs = model.suggestedInnerWedgeDsumSegmentsForSlot(slot);
            final boolean hasInnerPaint = inner.isPresent() && inner.getAsDouble() > 1e-9;
            final boolean hasOuterPaint = outerOnly.isPresent() && outerOnly.getAsDouble() > 1e-9;
            if (!hasInnerPaint && !hasOuterPaint) {
                if (!anyWedgeInRun) {
                    g.setColor(SuggestionStyle.amberSuggestionFillOpaqueFromSuggestedLayers(d, inner, outerOnly));
                    fillSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, slot.min(), slot.max());
                }
            } else {
                final Color outerFill =
                        SuggestionStyle.amberSuggestionFillOpaqueFromSuggestedLayers(
                                d, OptionalDouble.empty(), outerOnly);
                if (hasOuterPaint) {
                    if (outerSegs.isEmpty()) {
                        g.setColor(outerFill);
                        fillSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, slot.min(), slot.max());
                    } else {
                        for (final int[] seg : outerSegs) {
                            g.setColor(outerFill);
                            fillSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, seg[0], seg[1]);
                        }
                    }
                }
                final Color innerFill =
                        SuggestionStyle.amberSuggestionFillOpaqueFromSuggestedLayers(d, inner, OptionalDouble.empty());
                if (hasInnerPaint) {
                    if (innerSegs.isEmpty()) {
                        g.setColor(innerFill);
                        fillSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, slot.min(), slot.max());
                    } else {
                        for (final int[] seg : innerSegs) {
                            g.setColor(innerFill);
                            fillSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, seg[0], seg[1]);
                        }
                    }
                }
            }
            if (idx == end) {
                break;
            }
            idx = (idx + 1) % n;
        }
        g.setComposite(oldC);
        g.setStroke(new BasicStroke(1.5f));
        idx = firstIndex;
        while (true) {
            final EncounterSlot slot = EncounterSlot.values()[idx];
            final int segD = SuggestionStyle.segmentDistanceFromLikeliest(firstIndex, end, likeliestIndex, idx, n);
            final OptionalDouble innerS = model.encounterInnerWedgeOverlapPortionOfSlot(slot);
            final OptionalDouble outerS = model.encounterOuterOnlyWedgeOverlapPortionOfSlot(slot);
            final List<int[]> outerSegs = model.suggestedOuterOnlyWedgeDsumSegmentsForSlot(slot);
            final List<int[]> innerSegs = model.suggestedInnerWedgeDsumSegmentsForSlot(slot);
            final boolean hasInnerPaint = innerS.isPresent() && innerS.getAsDouble() > 1e-9;
            final boolean hasOuterPaint = outerS.isPresent() && outerS.getAsDouble() > 1e-9;
            if (!hasInnerPaint && !hasOuterPaint) {
                if (!anyWedgeInRun) {
                    g.setColor(SuggestionStyle.amberSuggestionStrokeFromSuggestedLayers(segD, innerS, outerS));
                    strokeSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, slot.min(), slot.max());
                }
            } else {
                final Color outerStroke =
                        SuggestionStyle.amberSuggestionStrokeFromSuggestedLayers(
                                segD, OptionalDouble.empty(), outerS);
                if (hasOuterPaint) {
                    if (outerSegs.isEmpty()) {
                        g.setColor(outerStroke);
                        strokeSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, slot.min(), slot.max());
                    } else {
                        for (final int[] seg : outerSegs) {
                            g.setColor(outerStroke);
                            strokeSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, seg[0], seg[1]);
                        }
                    }
                }
                final Color innerStroke =
                        SuggestionStyle.amberSuggestionStrokeFromSuggestedLayers(
                                segD, innerS, OptionalDouble.empty());
                if (hasInnerPaint) {
                    if (innerSegs.isEmpty()) {
                        g.setColor(innerStroke);
                        strokeSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, slot.min(), slot.max());
                    } else {
                        for (final int[] seg : innerSegs) {
                            g.setColor(innerStroke);
                            strokeSuggestedDsumSpan(g, translate, periodPx, viewW, bandY, bandH, seg[0], seg[1]);
                        }
                    }
                }
            }
            if (idx == end) {
                break;
            }
            idx = (idx + 1) % n;
        }
    }

    private static void fillSuggestedDsumSpan(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int bandY,
            final int bandH,
            final int lo,
            final int hi) {
        final double x0 = translate + (lo / (double) DSUM_RANGE) * periodPx;
        final double x1 = translate + ((hi + 1) / (double) DSUM_RANGE) * periodPx;
        for (int k = -3; k <= 3; k++) {
            final int ix0 = (int) Math.floor(x0 + k * periodPx);
            final int ix1 = (int) Math.ceil(x1 + k * periodPx);
            final int clipL = Math.max(0, ix0);
            final int clipR = Math.min(viewW, ix1);
            if (clipR > clipL) {
                g.fillRect(clipL, bandY - 2, clipR - clipL, bandH + 4);
            }
        }
    }

    private static void strokeSuggestedDsumSpan(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int bandY,
            final int bandH,
            final int lo,
            final int hi) {
        final double x0 = translate + (lo / (double) DSUM_RANGE) * periodPx;
        final double x1 = translate + ((hi + 1) / (double) DSUM_RANGE) * periodPx;
        for (int k = -3; k <= 3; k++) {
            final int ix0 = (int) Math.floor(x0 + k * periodPx);
            final int ix1 = (int) Math.ceil(x1 + k * periodPx);
            final int clipL = Math.max(0, ix0);
            final int clipR = Math.min(viewW, ix1);
            if (clipR > clipL) {
                g.drawRect(clipL, bandY - 2, clipR - clipL, bandH + 3);
            }
        }
    }

    /** Green band on target slot(s) above the amber suggestion overlay. */
    private void drawTargetSlotsOverlay(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int bandY,
            final int bandH) {
        if (model.getTargetSlots().isEmpty()) {
            return;
        }
        final boolean overlap = model.targetOverlapsUncertainty();
        final double oBlend = overlap ? model.getTargetUncertaintyOverlapPortionOfSlot() : 1.0;
        final Composite oldC = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlap ? 0.38f : 0.42f));

        for (final EncounterSlot slot : model.getTargetSlots()) {
            final double x0 = translate + (slot.min() / (double) DSUM_RANGE) * periodPx;
            final double x1 = translate + ((slot.max() + 1) / (double) DSUM_RANGE) * periodPx;
            g.setColor(DsumEncounterPaint.overlapStrengthGreen(115, 235, 125, 42, 185, 62, oBlend));
            for (int k = -3; k <= 3; k++) {
                final int ix0 = (int) Math.floor(x0 + k * periodPx);
                final int ix1 = (int) Math.ceil(x1 + k * periodPx);
                final int clipL = Math.max(0, ix0);
                final int clipR = Math.min(viewW, ix1);
                if (clipR > clipL) {
                    g.fillRect(clipL, bandY - 2, clipR - clipL, bandH + 4);
                }
            }
        }
        g.setComposite(oldC);
        g.setStroke(new BasicStroke(1.5f));
        for (final EncounterSlot slot : model.getTargetSlots()) {
            final double x0 = translate + (slot.min() / (double) DSUM_RANGE) * periodPx;
            final double x1 = translate + ((slot.max() + 1) / (double) DSUM_RANGE) * periodPx;
            g.setColor(DsumEncounterPaint.overlapStrengthGreen(28, 135, 55, 8, 95, 28, oBlend, 185, 240));
            for (int k = -3; k <= 3; k++) {
                final int ix0 = (int) Math.floor(x0 + k * periodPx);
                final int ix1 = (int) Math.ceil(x1 + k * periodPx);
                final int clipL = Math.max(0, ix0);
                final int clipR = Math.min(viewW, ix1);
                if (clipR > clipL) {
                    g.drawRect(clipL, bandY - 2, clipR - clipL, bandH + 3);
                }
            }
        }
    }

    private void drawUncertaintyBand(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int bandY,
            final int bandH) {
        if (model.getCalibratedSlot() == null || model.isCalibrating()) {
            return;
        }
        final double dCenter = needleCycleFraction(model.getDisplayAngleDeg()) * DSUM_RANGE;
        if (model.isDrawingOuterRbCycleUncertaintyBand()) {
            final double negDeg = model.getUncertaintyWedgeExtentNegDeg();
            final double posDeg = model.getUncertaintyWedgeExtentPosDeg();
            final double wdNeg = (negDeg / 360.0) * DSUM_RANGE;
            final double wdPos = (posDeg / 360.0) * DSUM_RANGE;
            drawUncertaintyBandStrip(
                    g, translate, periodPx, viewW, bandY, bandH, dCenter - wdNeg, dCenter + wdPos,
                    UiTheme.UNCERTAINTY_OUTER_FILL, UiTheme.UNCERTAINTY_OUTER_STROKE, 1.25f);
        }
        final double inNeg = model.getInnerUncertaintyWedgeExtentNegDeg();
        final double inPos = model.getInnerUncertaintyWedgeExtentPosDeg();
        final double iWdNeg = (inNeg / 360.0) * DSUM_RANGE;
        final double iWdPos = (inPos / 360.0) * DSUM_RANGE;
        drawUncertaintyBandStrip(
                g, translate, periodPx, viewW, bandY, bandH, dCenter - iWdNeg, dCenter + iWdPos,
                UiTheme.UNCERTAINTY_FILL, UiTheme.UNCERTAINTY_STROKE, 1.75f);
    }

    private static void drawUncertaintyBandStrip(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int bandY,
            final int bandH,
            final double d0,
            final double d1,
            final Color fill,
            final Color stroke,
            final float strokeWidth) {
        final double x0 = translate + (d0 / DSUM_RANGE) * periodPx;
        final double x1 = translate + (d1 / DSUM_RANGE) * periodPx;
        for (int k = -3; k <= 3; k++) {
            final int ix0 = (int) Math.floor(x0 + k * periodPx);
            final int ix1 = (int) Math.ceil(x1 + k * periodPx);
            final int clipL = Math.max(0, ix0);
            final int clipR = Math.min(viewW, ix1);
            if (clipR <= clipL) {
                continue;
            }
            g.setColor(fill);
            g.fillRect(clipL, bandY - 1, clipR - clipL, bandH + 2);
            g.setColor(stroke);
            g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.drawRect(clipL, bandY - 1, clipR - clipL, bandH + 1);
        }
    }

    private void drawSlotLabels(
            final Graphics2D g,
            final double translate,
            final double periodPx,
            final int viewW,
            final int labelY) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
        final FontMetrics fm = g.getFontMetrics();
        for (final EncounterSlot slot : EncounterSlot.values()) {
            final double mid = slot.min() + (slot.max() - slot.min()) / 2.0;
            final double xm = translate + (mid / DSUM_RANGE) * periodPx;
            final String label = Integer.toString(slot.ordinal() + 1);
            final int lw = fm.stringWidth(label);
            for (int k = -3; k <= 3; k++) {
                final int tx = (int) Math.round(xm + k * periodPx - lw / 2.0);
                if (tx + lw < 0 || tx > viewW) {
                    continue;
                }
                final float baseline = labelY + fm.getAscent();
                g.setColor(UiTheme.LABEL_HALO);
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oy = -1; oy <= 1; oy++) {
                        if (ox != 0 || oy != 0) {
                            g.drawString(label, tx + ox, baseline + oy);
                        }
                    }
                }
                g.setColor(UiTheme.TEXT_PRIMARY);
                g.drawString(label, tx, baseline);
            }
        }
    }

    private void drawNeedle(final Graphics2D g, final int cx, final int bandY, final boolean tight) {
        final Color needle = new Color(207, 68, 68);
        final int stemW = tight ? 5 : 8;
        g.setStroke(new BasicStroke(stemW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(needle);
        final int tipY = bandY - (tight ? 5 : 8);
        final int stemLen = tight ? 12 : 22;
        final int stemBottom = tipY + stemLen;
        g.drawLine(cx, tipY, cx, stemBottom);
        final float hw = tight ? 6f : 10f;
        final float hh = tight ? 8f : 14f;
        DsumEncounterPaint.paintCurvedArrowHeadDown(g, cx, stemBottom + hh, hw, hh, needle);
    }

    public EncounterWheelModel getModel() {
        return model;
    }
}
