package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.model.EncounterWheelModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Shared painting and color helpers for {@link EncounterWheel} and {@link EncounterWheelBar}.
 */
public final class DsumEncounterPaint {

    private DsumEncounterPaint() {
    }

    public static void installClickToFocus(final JComponent c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                SwingUtilities.invokeLater(c::requestFocusInWindow);
            }
        });
    }

    public static int lerpRgb(final int a, final int b, final double t) {
        return (int) Math.round(a + (b - a) * Math.max(0.0, Math.min(1.0, t)));
    }

    public static Color overlapStrengthGreen(
            final int rLo, final int gLo, final int bLo,
            final int rHi, final int gHi, final int bHi,
            final double portion) {
        final double p = Math.max(0.0, Math.min(1.0, portion));
        return new Color(
                lerpRgb(rLo, rHi, p),
                lerpRgb(gLo, gHi, p),
                lerpRgb(bLo, bHi, p)
        );
    }

    public static Color overlapStrengthGreen(
            final int rLo, final int gLo, final int bLo,
            final int rHi, final int gHi, final int bHi,
            final double portion,
            final int aLo, final int aHi) {
        final double p = Math.max(0.0, Math.min(1.0, portion));
        return new Color(
                lerpRgb(rLo, rHi, p),
                lerpRgb(gLo, gHi, p),
                lerpRgb(bLo, bHi, p),
                lerpRgb(aLo, aHi, p)
        );
    }

    public static Color pulseColor(final Color color, final float pulseAmount, final float brightnessWeight) {
        final float t = pulseAmount * brightnessWeight;
        final int r = (int) (color.getRed() + (255 - color.getRed()) * t);
        final int g = (int) (color.getGreen() + (255 - color.getGreen()) * t);
        final int b = (int) (color.getBlue() + (255 - color.getBlue()) * t);
        return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b))
        );
    }

    /**
     * Amber / overlap fill for the overlap-approach countdown inner bar.
     */
    public static Color approachBarInnerFill(final EncounterWheelModel model, final double progress) {
        final double p = progress;
        final double op = model.getTargetUncertaintyOverlapPortionOfSlot();
        if (model.targetOverlapsUncertainty()) {
            return overlapStrengthGreen(72, 190, 95, 28, 165, 72, op);
        }
        return UiTheme.approachCountdownNonOverlapFill(p);
    }

    /**
     * Track + border + optional inner fill (when {@code progress} &gt; ~0).
     */
    public static void paintApproachCountdownBar(
            final Graphics2D g2,
            final EncounterWheelModel model,
            final int x,
            final int y,
            final int barW,
            final int barH,
            final int arcOuter,
            final float borderStrokeW,
            final int inset,
            final int innerArc) {
        final double p = model.getTargetOverlapApproachProgress();
        g2.setColor(UiTheme.APPROACH_BAR_BG);
        g2.fillRoundRect(x, y, barW, barH, arcOuter, arcOuter);
        g2.setColor(UiTheme.APPROACH_BAR_BORDER);
        g2.setStroke(new BasicStroke(borderStrokeW));
        g2.drawRoundRect(x, y, barW, barH, arcOuter, arcOuter);
        if (p > 0.004) {
            final int innerW = barW - inset * 2;
            final int fillW = Math.max(1, (int) Math.round(innerW * p));
            final int hInner = barH - inset * 2;
            g2.setColor(approachBarInnerFill(model, p));
            g2.fillRoundRect(x + inset, y + inset, fillW, hInner, innerArc, innerArc);
        }
    }

    /** Font sizes and padding for the bottom-right DSum chip (wheel scales with {@code geomScale}). */
    public record DsumReadoutMetrics(
            int pad,
            int cornerRadius,
            int lineGap,
            float capFontPt,
            float valueFontPt,
            float tildeFontPt,
            int valueTildeGapPx,
            int edgeMargin,
            float chipBorderStroke
    ) {
        public static DsumReadoutMetrics forWheel(final double geomScale) {
            final double g = Math.max(0.35, Math.min(1.0, geomScale));
            final int pad = (int) Math.round(11 * g);
            final int radius = Math.max(4, (int) Math.round(10 * g));
            final int gap = Math.max(1, (int) Math.round(2 * g));
            final int valueGap = Math.max(1, (int) Math.round(3 * g));
            final int margin = (int) Math.round(16 * g);
            return new DsumReadoutMetrics(
                    pad,
                    radius,
                    gap,
                    (float) (10.5 * g),
                    (float) (24 * g),
                    (float) (14 * g),
                    valueGap,
                    margin,
                    (float) g
            );
        }

        public static DsumReadoutMetrics forBar(final boolean tight) {
            if (tight) {
                return new DsumReadoutMetrics(5, 6, 1, 8.5f, 15f, 10f, 2, 6, 1f);
            }
            return new DsumReadoutMetrics(8, 8, 2, 10f, 20f, 12f, 2, 12, 1f);
        }
    }

    /** Bottom-right approximate DSum readout (raw {@link EncounterWheelModel#getDsum()}, not display-lag value). */
    public static void paintDsumReadoutChip(
            final Graphics2D g2,
            final int dsum,
            final int panelWidth,
            final int panelHeight,
            final DsumReadoutMetrics m) {
        paintDsumReadoutChip(g2, dsum, panelWidth, panelHeight, m, null);
    }

    /**
     * @param chipFootnote optional second line under the value (e.g. uncalibrated hint); {@code null} for default chip
     */
    public static void paintDsumReadoutChip(
            final Graphics2D g2,
            final int dsum,
            final int panelWidth,
            final int panelHeight,
            final DsumReadoutMetrics m,
            final String chipFootnote) {
        final String cap = "DSum";
        final String val = Integer.toString(dsum);
        final Font base = g2.getFont();
        final Font capFont = base.deriveFont(Font.PLAIN, m.capFontPt);
        final Font valFont = base.deriveFont(Font.BOLD, m.valueFontPt);
        final Font tildeFont = base.deriveFont(Font.PLAIN, m.tildeFontPt);
        final Font footFont =
                chipFootnote == null || chipFootnote.isEmpty()
                        ? null
                        : base.deriveFont(Font.PLAIN, Math.max(8f, m.capFontPt * 0.72f));
        g2.setFont(capFont);
        final FontMetrics capFm = g2.getFontMetrics();
        g2.setFont(valFont);
        final FontMetrics valFm = g2.getFontMetrics();
        g2.setFont(tildeFont);
        final FontMetrics tildeFm = g2.getFontMetrics();
        FontMetrics footFm = null;
        int footW = 0;
        if (footFont != null) {
            g2.setFont(footFont);
            footFm = g2.getFontMetrics();
            footW = footFm.stringWidth(chipFootnote);
        }
        final int capW = capFm.stringWidth(cap);
        final int tildeW = tildeFm.stringWidth("~");
        final int numW = valFm.stringWidth(val);
        final int valueRowW = tildeW + m.valueTildeGapPx + numW;
        final int extraFootH =
                footFm == null ? 0 : footFm.getHeight() + Math.max(1, m.lineGap - 1);
        final int chipW = Math.max(Math.max(capW, valueRowW), footW) + m.pad * 2;
        final int chipH = capFm.getHeight() + m.lineGap + valFm.getHeight() + m.pad * 2 - 2 + extraFootH;
        final int chipX = panelWidth - chipW - m.edgeMargin;
        final int chipY = panelHeight - chipH - m.edgeMargin;
        final Stroke oldStroke = g2.getStroke();
        g2.setColor(UiTheme.CHIP_BG);
        g2.fillRoundRect(chipX, chipY, chipW, chipH, m.cornerRadius, m.cornerRadius);
        g2.setColor(UiTheme.CHIP_BORDER);
        g2.setStroke(new BasicStroke(m.chipBorderStroke));
        g2.drawRoundRect(chipX, chipY, chipW, chipH, m.cornerRadius, m.cornerRadius);
        g2.setStroke(oldStroke);
        g2.setFont(capFont);
        g2.setColor(UiTheme.TEXT_MUTED);
        g2.drawString(cap, chipX + (chipW - capW) / 2f, chipY + m.pad + capFm.getAscent());
        final float valueBaseline = chipY + m.pad + capFm.getHeight() + m.lineGap + valFm.getAscent();
        float vx = chipX + (chipW - valueRowW) / 2f;
        g2.setFont(tildeFont);
        final float tildeBaseline = valueBaseline - (valFm.getAscent() - tildeFm.getAscent());
        g2.drawString("~", vx, tildeBaseline);
        vx += tildeW + m.valueTildeGapPx;
        g2.setFont(valFont);
        g2.setColor(UiTheme.TEXT_PRIMARY);
        g2.drawString(val, vx, valueBaseline);
        if (footFont != null && footFm != null) {
            g2.setFont(footFont);
            g2.setColor(UiTheme.TEXT_MUTED);
            final float footBaseline = valueBaseline + valFm.getDescent() + Math.max(1, m.lineGap - 1) + footFm.getAscent();
            g2.drawString(chipFootnote, chipX + (chipW - footW) / 2f, footBaseline);
        }
    }

    /**
     * Bottom-left hint chip (same chrome as DSum). {@code text} may contain {@code \\n} for wraps; skipped when
     * {@code null} or empty.
     */
    public static void paintInstructionChip(
            final Graphics2D g2,
            final String text,
            final int panelWidth,
            final int panelHeight,
            final DsumReadoutMetrics m) {
        if (text == null || text.isEmpty()) {
            return;
        }
        final String[] lines = text.split("\n", -1);
        final Font base = g2.getFont();
        final Font lineFont = base.deriveFont(Font.PLAIN, Math.max(9f, m.capFontPt * 0.92f));
        g2.setFont(lineFont);
        final FontMetrics fm = g2.getFontMetrics();
        int maxLineW = 0;
        for (final String line : lines) {
            maxLineW = Math.max(maxLineW, fm.stringWidth(line));
        }
        final int innerLineGap = Math.max(1, m.lineGap);
        final int chipW = maxLineW + m.pad * 2;
        final int chipH = fm.getHeight() * lines.length + innerLineGap * (lines.length - 1) + m.pad * 2;
        final int chipX = m.edgeMargin;
        final int chipY = panelHeight - chipH - m.edgeMargin;
        final Stroke oldStroke = g2.getStroke();
        g2.setColor(UiTheme.CHIP_BG);
        g2.fillRoundRect(chipX, chipY, chipW, chipH, m.cornerRadius, m.cornerRadius);
        g2.setColor(UiTheme.CHIP_BORDER);
        g2.setStroke(new BasicStroke(m.chipBorderStroke));
        g2.drawRoundRect(chipX, chipY, chipW, chipH, m.cornerRadius, m.cornerRadius);
        g2.setStroke(oldStroke);
        g2.setFont(lineFont);
        g2.setColor(UiTheme.TEXT_PRIMARY);
        int lineY = chipY + m.pad + fm.getAscent();
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], chipX + m.pad, lineY);
            lineY += fm.getHeight();
            if (i < lines.length - 1) {
                lineY += innerLineGap;
            }
        }
    }

    /**
     * Arrowhead pointing downward: tip at {@code tipY} (lowest on screen), base at {@code tipY - headHeight}.
     * Cubic sides for a smooth, contemporary shape; light gradient and rounded outline.
     */
    public static void paintCurvedArrowHeadDown(
            final Graphics2D g2,
            final float cx,
            final float tipY,
            final float halfWidth,
            final float headHeight,
            final Color body) {
        final float baseY = tipY - headHeight;
        final Path2D.Float gp = new Path2D.Float(Path2D.WIND_NON_ZERO);
        gp.moveTo(cx, tipY);
        gp.curveTo(
                cx - halfWidth * 0.38f, tipY - headHeight * 0.42f,
                cx - halfWidth * 0.92f, tipY - headHeight * 0.82f,
                cx - halfWidth, baseY);
        gp.lineTo(cx + halfWidth, baseY);
        gp.curveTo(
                cx + halfWidth * 0.92f, tipY - headHeight * 0.82f,
                cx + halfWidth * 0.38f, tipY - headHeight * 0.42f,
                cx, tipY);
        gp.closePath();

        final Color hi = new Color(
                Math.min(255, body.getRed() + 32),
                Math.min(255, body.getGreen() + 18),
                Math.min(255, body.getBlue() + 18));
        final Color lo = new Color(
                Math.max(0, body.getRed() - 40),
                Math.max(0, body.getGreen() - 22),
                Math.max(0, body.getBlue() - 22));
        final Paint oldP = g2.getPaint();
        g2.setPaint(new GradientPaint(cx, baseY, hi, cx, tipY, lo));
        g2.fill(gp);
        g2.setPaint(oldP);

        final Stroke oldS = g2.getStroke();
        final float ow = Math.max(1f, Math.min(2.5f, halfWidth * 0.09f));
        g2.setStroke(new BasicStroke(ow, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(
                Math.max(0, body.getRed() - 55),
                Math.max(0, body.getGreen() - 35),
                Math.max(0, body.getBlue() - 35)));
        g2.draw(gp);
        g2.setStroke(oldS);
    }
}
