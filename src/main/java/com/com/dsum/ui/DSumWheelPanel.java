package com.com.dsum.ui;

import com.com.dsum.model.EncounterSlot;
import com.com.dsum.model.SelectionsConfig;
import com.com.dsum.sim.DSumDriver;
import com.com.dsum.sim.DSumSlotComputer;
import com.com.dsum.sim.DSumTracker;
import com.com.dsum.sim.ToolTipProvider;
import com.com.dsum.util.Colours;
import com.com.dsum.util.GameConstants;
import com.com.dsum.util.MathUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

public class DSumWheelPanel extends JPanel {

    private static final int CHIP_PAD = 5;
    private static final int CHIP_ROUNDNESS = 10;

    private static final Font STATE_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font STATE_SUB_FONT = new Font("Arial", Font.BOLD, 12);

    private static final double UNCERTAINTY_WEDGE_RATIO = 1.05;
    private static final int WHEEL_RADIUS = 250;

    private static final double SLIVER_ANGULAR_OVERLAP_DEG = 0.15;

    private final SelectionsConfig config;
    private final DSumDriver driver;
    private final DSumTracker tracker;
    private final DSumSlotComputer slotComputer;
    private final Colours colours;
    private final ToolTipProvider toolTipProvider;

    private volatile Map<EncounterSlot, Integer> suggested = null;
    private volatile int uncertainty = 1;

    private Area[] mergedSlotAreasCache = null;
    private int mergeCacheWidth = -1;
    private int mergeCacheHeight = -1;
    private int mergeCacheSlotsEpoch = -1;

    private double wedgeCacheCenter = Integer.MIN_VALUE;
    private int wedgeCacheU = -1;
    private int wedgeCacheCx = Integer.MIN_VALUE;
    private int wedgeCacheCy = Integer.MIN_VALUE;
    private Arc2D wedgeCacheArc;

    public DSumWheelPanel(final SelectionsConfig config, final DSumDriver driver, final DSumTracker tracker, final DSumSlotComputer slotComputer, final Colours colours, final ToolTipProvider toolTipProvider) {
        this.config = config;
        this.driver = driver;
        this.tracker = tracker;
        this.slotComputer = slotComputer;
        this.colours = colours;
        this.toolTipProvider = toolTipProvider;

        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(600, 600));

        this.setFocusable(true);
        this.requestFocusInWindow();

        driver.registerUpdateListener(() -> SwingUtilities.invokeLater(this::repaint));
        driver.registerSuggestionListener(suggestions -> {
            this.suggested = suggestions;
            SwingUtilities.invokeLater(this::repaint);
        });
        driver.registerUncertaintyChangeListener(uncertainty -> {
            this.uncertainty = uncertainty;
            SwingUtilities.invokeLater(this::repaint);
        });
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (!this.isVisible()) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g;

        fillBackgroundPerThreshold(g2);
        drawSlots(g2);
        drawStateChip(g2);
        drawInstructionsChip(g2);
    }


    private static Area dsumAnnulus(final int cx, final int cy, final double rInner, final double rOuter, final int dsum) {
        final double nominalStart = (dsum / (double) GameConstants.DSUM_RANGE) * 360 + 90;
        final double nominalExtent = (1 / (double) GameConstants.DSUM_RANGE) * 360;
        final double start = nominalStart - SLIVER_ANGULAR_OVERLAP_DEG;
        final double extent = nominalExtent + 2 * SLIVER_ANGULAR_OVERLAP_DEG;
        final Arc2D outer = new Arc2D.Double(cx - rOuter, cy - rOuter, rOuter * 2, rOuter * 2, start, extent, Arc2D.PIE);
        if (rInner <= 1e-6) {
            return new Area(outer);
        }
        final Area ring = new Area(outer);
        ring.subtract(new Area(new Arc2D.Double(cx - rInner, cy - rInner, rInner * 2, rInner * 2, start, extent, Arc2D.PIE)));
        return ring;
    }

    private void fillBackgroundPerThreshold(final Graphics2D g) {
        final Color bg;
        if (driver.isUncalibrated()) {
            bg = colours.uncalibrated();
        } else {
            final double probability = this.driver.getTargetCumulativeProbability();
            bg = this.colours.background(probability);
        }

        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void drawSlots(final Graphics2D g2) {
        final int cx = getWidth() / 2;
        final int cy = getHeight() / 2;

        final double currentDSum = this.tracker.getDSum();
        final double theta = (currentDSum / (double) GameConstants.DSUM_RANGE) * 360;
        final AffineTransform oldTransform = g2.getTransform();

        g2.rotate(Math.toRadians(theta), cx, cy);
        final Area[] merged = mergedSlotAreas(cx, cy);
        final Map<EncounterSlot, Integer> suggestions = this.suggested;
        for (int ord = 9; ord >= 0; ord--) {
            final EncounterSlot slot = EncounterSlot.values()[ord];
            final Area shape = merged[ord];
            final Color colour = this.colours.fillColor(slot);
            g2.setColor(colour);
            g2.fill(shape);
            final Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(colour.darker().darker());
            g2.draw(shape);
            g2.setStroke(oldStroke);

            if (config.getTargets().contains(slot)) {
                final Color target = this.colours.targetColor();
                g2.setColor(target);
                g2.fill(shape);
            }
            if (suggestions != null && suggestions.containsKey(slot)) {
                // Deliberately over the slot colour; suggestion colour includes transparency.
                g2.setColor(this.colours.suggestionColor());
                g2.fill(shape);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(shape);
                g2.setStroke(oldStroke);
            }
        }

        drawUncertaintyWedge(g2, cx, cy);
        g2.setTransform(oldTransform);
    }

    private Area[] mergedSlotAreas(final int cx, final int cy) {
        // Each 'slot' is not necessarily a nice, neat wedge.  Depending on the
        // encounter rate of the route, the slot can have dramatically different
        // geometry.
        // Here, we go hRandomAdd by hRandomAdd 'slices' for each slot, and combine
        // the areas for each slot range/wedges, to create a single Area.
        final int w = getWidth();
        final int h = getHeight();
        if (this.mergedSlotAreasCache != null
                && this.mergeCacheWidth == w
                && this.mergeCacheHeight == h
                && this.mergeCacheSlotsEpoch == this.slotComputer.getSlotsEpoch()) {
            return this.mergedSlotAreasCache;
        }
        final double[][] probabilitiesTable = new double[GameConstants.DSUM_RANGE][EncounterSlot.values().length];
        final int epochSnapshot = this.slotComputer.snapshotSlotsInto(probabilitiesTable);
        final Area[] merged = new Area[10];
        for (int i = 0; i < merged.length; i++) {
            merged[i] = new Area();
        }
        final double rMax = WHEEL_RADIUS;
        for (int dsum = 0; dsum < GameConstants.DSUM_RANGE; dsum++) {
            final double[] probabilities = probabilitiesTable[dsum];
            double height = 1.0d;
            for (int slotOrdinal = 9; slotOrdinal >= 0; slotOrdinal--) {
                final double p = probabilities[slotOrdinal];
                if (p != 0.0d) {
                    final double rOuter = rMax * Math.sqrt(height);
                    final double rInner = rMax * Math.sqrt(height - p);
                    merged[slotOrdinal].add(dsumAnnulus(cx, cy, rInner, rOuter, dsum));
                    height -= p;
                }
            }
        }
        this.mergedSlotAreasCache = merged;
        this.mergeCacheWidth = w;
        this.mergeCacheHeight = h;
        this.mergeCacheSlotsEpoch = epochSnapshot;
        return merged;
    }

    private void drawUncertaintyWedge(final Graphics2D g2, final int cx, final int cy) {
        final int u = Math.max(1, this.uncertainty);
        final double center = MathUtilities.mod(this.tracker.getDSum());
        final int span = 2 * u + 1;
        final double r = UNCERTAINTY_WEDGE_RATIO * WHEEL_RADIUS;

        if (wedgeCacheArc == null
                || center != wedgeCacheCenter
                || u != wedgeCacheU
                || cx != wedgeCacheCx
                || cy != wedgeCacheCy) {
            final double startSlice = MathUtilities.mod(center - u);
            final double angularStart = (startSlice / (double) GameConstants.DSUM_RANGE) * 360 + 90;
            final double angularExtent = (span / (double) GameConstants.DSUM_RANGE) * 360;
            wedgeCacheArc = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, angularStart - 1, angularExtent + 2, Arc2D.PIE);
            wedgeCacheCenter = center;
            wedgeCacheU = u;
            wedgeCacheCx = cx;
            wedgeCacheCy = cy;
        }

        final Color colour = this.colours.uncertaintyColor();
        g2.setColor(colour);
        g2.fill(wedgeCacheArc);
        g2.setStroke(new BasicStroke(3.0f));
        g2.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 180));
        g2.draw(wedgeCacheArc);
    }

    private void drawStateChip(final Graphics2D g2) {
        final String stateText = toolTipProvider.getStateText();
        final String stateSubText = toolTipProvider.getStateSubText();

        final FontMetrics fmMain = g2.getFontMetrics(STATE_FONT);
        final FontMetrics fmSub = g2.getFontMetrics(STATE_SUB_FONT);

        // Bunch of nonsense to figure out widths / heights / positions
        // based on the text given (so that it's centred).
        final int mainWidth = fmMain.stringWidth(stateText);
        final int subWidth = fmSub.stringWidth(stateSubText);
        final int line1Height = fmMain.getHeight();
        final int line2Height = fmSub.getHeight();

        final int tw = Math.max(mainWidth, subWidth) + 2 * CHIP_PAD;
        final int th = line1Height + line2Height + 2 * CHIP_PAD;

        final int x = getWidth() - tw - 30;
        final int y = th + 15;

        g2.setColor(colours.stateChip());
        g2.fillRoundRect(x, y, tw, th, CHIP_ROUNDNESS, CHIP_ROUNDNESS);
        g2.setColor(colours.stateChip().darker().darker());
        g2.drawRoundRect(x, y, tw, th, CHIP_ROUNDNESS, CHIP_ROUNDNESS);

        final int innerTop = y + CHIP_PAD;
        final int innerHeight = th - 2 * CHIP_PAD;
        final int extraVertical = innerHeight - line1Height - line2Height;
        final int blockTop = innerTop + Math.max(0, extraVertical) / 2;

        final int baselineMain = blockTop + fmMain.getAscent();
        final int baselineSub = baselineMain + line1Height - 5;

        final int xMain = x + (tw - mainWidth) / 2;
        final int xSub = x + (tw - subWidth) / 2;

        g2.setColor(colours.stateText());
        g2.setFont(STATE_FONT);
        g2.drawString(stateText, xMain, baselineMain);
        g2.setFont(STATE_SUB_FONT);
        g2.drawString(stateSubText, xSub, baselineSub);
    }

    private void drawInstructionsChip(final Graphics2D g2) {
        final String text = toolTipProvider.getInstructionText();
        if (text == null) {
            return;
        }
        final StringTokenizer tokens = new StringTokenizer(text, "\n");

        // Even more nonsense to figure out widths / heights / positions
        // based on the text given (so that it's centred).
        // This one's MORE messy, because the number of lines isn't fixed.
        final FontMetrics fm = g2.getFontMetrics(STATE_SUB_FONT);

        final ArrayList<String> lines = new ArrayList<>();
        int maxLineWidth = 0;
        while (tokens.hasMoreTokens()) {
            final String token = tokens.nextToken();
            lines.add(token);
            maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(token));
        }
        if (lines.isEmpty()) {
            return;
        }

        final int lineHeight = fm.getHeight();
        final int textBlockHeight = lineHeight * lines.size();

        final int tw = maxLineWidth + 2 * CHIP_PAD;
        final int th = textBlockHeight + 2 * CHIP_PAD;

        final int x = 30;
        final int y = getHeight() - textBlockHeight - 30;

        g2.setColor(colours.instructionChip());
        g2.fillRoundRect(x, y, tw, th, CHIP_ROUNDNESS, CHIP_ROUNDNESS);
        g2.setColor(colours.instructionChip().darker().darker());
        g2.drawRoundRect(x, y, tw, th, CHIP_ROUNDNESS, CHIP_ROUNDNESS);

        final int innerTop = y + CHIP_PAD;
        final int innerHeight = th - 2 * CHIP_PAD;
        final int extraVertical = innerHeight - textBlockHeight;
        final int blockTop = innerTop + Math.max(0, extraVertical) / 2;

        g2.setColor(colours.instructionText());
        g2.setFont(STATE_SUB_FONT);

        int baseline = blockTop + fm.getAscent();
        for (final String line : lines) {
            final int lineX = x + (tw - fm.stringWidth(line)) / 2;
            g2.drawString(line, lineX, baseline);
            baseline += lineHeight;
        }
    }
}
