package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.*;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.util.SlotPalette;
import com.com.poke.rng.dsum.util.SuggestionStyle;
import com.com.poke.rng.dsum.util.SpriteImageUtil;
import com.com.poke.rng.dsum.util.Triplet;
import ext.CenteredBoxIcon;
import ext.CompoundIcon;
import ext.TextIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SlotsDisplayPanel extends JPanel {

    private static final int BAR_IDLE_PX = 15;
    private static final int BAR_SUGGESTED_PX = 15;
    private static final int BAR_LIKELIEST_PX = 15;
    private static final int SUGGESTION_AMBER_H = 6;

    /** Preferred row height in minimal view; kept in sync with {@link EncounterWheelBar} strip height. */
    public static final int COMPACT_PREFERRED_HEIGHT = 102;

    private volatile Game game;
    private volatile Route route;

    private volatile int barIdlePx = BAR_IDLE_PX;
    private volatile int barSuggestedPx = BAR_SUGGESTED_PX;
    private volatile int barLikeliestPx = BAR_LIKELIEST_PX;
    private volatile int suggestionAmberH = SUGGESTION_AMBER_H;
    private volatile boolean compact;
    private volatile Triplet<EncounterSlot, EncounterSlot, EncounterSlot> lastSuggested;

    private final EncounterWheelModel encounterModel;

    private final List<JToggleButton> buttons = new ArrayList<>(10);
    private final List<TargetAwareSlotColorBar> slotColorBars = new ArrayList<>(10);
    private final List<SlotSuggestionStrip> suggestionAmberBars = new ArrayList<>(10);
    private final List<SlotTile> tiles = new ArrayList<>();

    private final Consumer<List<EncounterSlot>> onTargetsChanged;
    private volatile boolean suppressTargetCallbacks;

    public SlotsDisplayPanel(
            final Game game,
            final Route route,
            final EncounterSlot initialSlot,
            final EncounterWheelModel encounterModel,
            final Consumer<List<EncounterSlot>> onTargetsChanged) {
        this.onTargetsChanged = onTargetsChanged;
        this.encounterModel = encounterModel;
        this.game = game;
        this.route = route;

        setBackground(UiTheme.SURFACE);
        setPreferredSize(new Dimension(500, 150));

        setLayout(new GridBagLayout());
        for (int i = 0; i < 10; i++) {
            final EncounterSlot encounterSlot = EncounterSlot.values()[i];
            final JPanel column = new JPanel(new BorderLayout());
            column.setOpaque(false);

            final JToggleButton toggle = new SpriteSlotToggle(this);
            toggle.setPreferredSize(new Dimension(getWidth() / 10, getHeight())); 
            toggle.setFocusPainted(false);
            if (i == initialSlot.ordinal()) {
                toggle.setSelected(true);
            }
            toggle.addActionListener(e -> {
                if (suppressTargetCallbacks) {
                    return;
                }
                final List<EncounterSlot> newTargets = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    final JToggleButton b = buttons.get(j);
                    if (b.isSelected()) {
                        newTargets.add(EncounterSlot.values()[j]);
                    }
                }
                onTargetsChanged.accept(newTargets);
                refreshToggleStyles();
                if (lastSuggested != null) {
                    setSuggestedSlots(lastSuggested);
                }
            });
            applyToggleChrome(toggle);

            final TargetAwareSlotColorBar colorBar = new TargetAwareSlotColorBar(encounterModel, encounterSlot);
            applyIdleBar(colorBar);

            final SlotSuggestionStrip amberBar = new SlotSuggestionStrip();
            applyIdleAmberBar(amberBar);

            final JPanel bottomStack = new JPanel(new BorderLayout());
            bottomStack.setOpaque(false);
            bottomStack.add(colorBar, BorderLayout.NORTH);
            bottomStack.add(amberBar, BorderLayout.SOUTH);

            column.add(toggle, BorderLayout.CENTER);
            column.add(bottomStack, BorderLayout.SOUTH);

            buttons.add(toggle);
            slotColorBars.add(colorBar);
            suggestionAmberBars.add(amberBar);
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = i;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.ipadx = 3;
            constraints.fill = GridBagConstraints.BOTH;
            add(column, constraints);
        }

        refreshToggleStyles();
        this.update();
    }

    /**
     * Select target slots from code (e.g. presets) in one shot — one {@link #onTargetsChanged} call, no duplicate
     * callbacks from toggle {@code setSelected}.
     */
    public void applyPresetTargets(final List<EncounterSlot> targets) {
        final List<EncounterSlot> next = List.copyOf(targets);
        suppressTargetCallbacks = true;
        try {
            for (int j = 0; j < 10; j++) {
                buttons.get(j).setSelected(next.contains(EncounterSlot.values()[j]));
            }
            refreshToggleStyles();
        } finally {
            suppressTargetCallbacks = false;
        }
        onTargetsChanged.accept(next);
        if (lastSuggested != null) {
            setSuggestedSlots(lastSuggested);
        }
    }

    public void applyUiThemeColors() {
        setBackground(UiTheme.SURFACE);
        refreshToggleStyles();
        for (int j = 0; j < slotColorBars.size(); j++) {
            applyIdleBar(slotColorBars.get(j));
            applyIdleAmberBar(suggestionAmberBars.get(j));
        }
        if (lastSuggested != null) {
            setSuggestedSlots(lastSuggested);
        }
        repaint();
    }

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(final boolean compact) {
        if (this.compact == compact) {
            return;
        }
        this.compact = compact;
        if (compact) {
            final int w = EncounterWheel.diameterForGeomScale(EncounterWheel.COMPACT_GEOM_SCALE);
            barIdlePx = 9;
            barSuggestedPx = 9;
            barLikeliestPx = 9;
            suggestionAmberH = 4;
            setPreferredSize(new Dimension(w, COMPACT_PREFERRED_HEIGHT));
        } else {
            barIdlePx = BAR_IDLE_PX;
            barSuggestedPx = BAR_SUGGESTED_PX;
            barLikeliestPx = BAR_LIKELIEST_PX;
            suggestionAmberH = SUGGESTION_AMBER_H;
            setPreferredSize(new Dimension(500, 150));
        }
        for (final EncounterSlot slot : EncounterSlot.values()) {
            applyIdleBar(slotColorBars.get(slot.ordinal()));
            applyIdleAmberBar(suggestionAmberBars.get(slot.ordinal()));
        }
        if (lastSuggested != null) {
            setSuggestedSlots(lastSuggested);
        }
        update();
        revalidate();
        repaint();
    }

    private void refreshToggleStyles() {
        for (final JToggleButton button : buttons) {
            applyToggleChrome(button);
        }
    }

    private static void applyToggleChrome(final JToggleButton b) {
        b.setForeground(UiTheme.TEXT_PRIMARY);
        if (b.isSelected()) {
            b.setBackground(UiTheme.TOGGLE_ON_BG);
            b.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
        } else {
            b.setBackground(UiTheme.TOGGLE_OFF_BG);
            b.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
        }
    }

    /**
     * Sprite slot target toggles: rounded fill + outline (replacing {@link BorderFactory#createLineBorder}, which is
     * always square and hid FlatLaf’s {@code ToggleButton.arc} look).
     */
    private static final class SpriteSlotToggle extends JToggleButton {

        private final SlotsDisplayPanel panel;

        private SpriteSlotToggle(final SlotsDisplayPanel panel) {
            this.panel = panel;
            setOpaque(false);
            setContentAreaFilled(false);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final Insets ins = getInsets();
            final int x = ins.left;
            final int y = ins.top;
            final int bw = getWidth() - ins.left - ins.right;
            final int bh = getHeight() - ins.top - ins.bottom;
            final float maxArc = panel.isCompact() ? 8f : 12f;
            final float arc = Math.min(maxArc, Math.min(bw, bh) / 2f - 0.5f);

            final RoundRectangle2D body = new RoundRectangle2D.Float(x, y, bw, bh, arc, arc);

            g2.setColor(getBackground());
            g2.fill(body);

            final boolean on = isSelected();
            g2.setColor(on ? UiTheme.TOGGLE_ON_BORDER : UiTheme.TOGGLE_OFF_BORDER);
            final float sw = on ? 2f : 1f;
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            final float inset = sw / 2f;
            g2.draw(new RoundRectangle2D.Float(
                    x + inset, y + inset, bw - sw, bh - sw, arc, arc));

            g2.dispose();

            final Shape oldClip = g.getClip();
            g.setClip(body);
            super.paintComponent(g);
            g.setClip(oldClip);
        }
    }


    public void setGame(final Game game) {
        this.game = game;

        update();
    }

    public void setRoute(final Route route) {
        this.route = route;

        update();
    }

    private void update() {
        this.tiles.clear();

        final Map<EncounterSlot, Encounter> encounterMap = route.getEncounters().get(game);
        for (final EncounterSlot slot : EncounterSlot.values()) {
            tiles.add(new SlotTile(encounterMap.get(slot)));
        }
        for (int i = 0; i < tiles.size(); i++) {
            updateTileIcon(i, tiles.get(i));
        }

        SwingUtilities.invokeLater(this::repaint);
    }

    public void setSuggestedSlots(final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> slots) {
        lastSuggested = slots;
        if (buttons.isEmpty()) {
            return;
        }
        for (int j = 0; j < slotColorBars.size(); j++) {
            applyIdleBar(slotColorBars.get(j));
            applyIdleAmberBar(suggestionAmberBars.get(j));
        }

        if (slots == null) {
            SwingUtilities.invokeLater(() -> {
                revalidate();
                repaint();
            });
            return;
        }

        final int firstIndex = slots.first().ordinal();
        final int likeliestIndex = slots.second().ordinal();
        final int lastIndex = slots.third().ordinal();
        final int n = buttons.size();

        for (int i = firstIndex; ; i = (i + 1) % n) {
            final EncounterSlot slot = EncounterSlot.values()[i];
            final TargetAwareSlotColorBar bar = slotColorBars.get(i);
            if (encounterModel.getTargetSlots().contains(slot)) {
                bar.setTone(SlotBarTone.IDLE);
                bar.setBarHeightPx(i == likeliestIndex ? barLikeliestPx : barSuggestedPx);
            } else if (i == likeliestIndex) {
                bar.setTone(SlotBarTone.SUGGESTED_LIKELIEST);
                bar.setBarHeightPx(barLikeliestPx);
            } else {
                bar.setTone(SlotBarTone.SUGGESTED_NEAR);
                bar.setBarHeightPx(barSuggestedPx);
            }
            final int d = SuggestionStyle.segmentDistanceFromLikeliest(firstIndex, lastIndex, likeliestIndex, i, n);
            final SlotSuggestionStrip amber = suggestionAmberBars.get(i);
            amber.configureSuggestion(
                    encounterModel, slot, buttons.get(i).isSelected(), d, suggestionAmberH);

            if (i == lastIndex) {
                break;
            }
        }

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    private void applyIdleBar(final TargetAwareSlotColorBar bar) {
        bar.setTone(SlotBarTone.IDLE);
        bar.setBarHeightPx(barIdlePx);
    }

    private enum SlotBarTone {
        IDLE,
        SUGGESTED_LIKELIEST,
        SUGGESTED_NEAR
    }

    /**
     * Muted / suggested slot tints, or the same target green as {@link EncounterWheel} when this column is a target.
     */
    private static final class TargetAwareSlotColorBar extends JPanel {

        private final EncounterWheelModel model;
        private final EncounterSlot slot;
        private SlotBarTone tone = SlotBarTone.IDLE;

        TargetAwareSlotColorBar(final EncounterWheelModel model, final EncounterSlot slot) {
            this.model = model;
            this.slot = slot;
            setOpaque(true);
        }

        void setTone(final SlotBarTone t) {
            this.tone = t;
            repaint();
        }

        void setBarHeightPx(final int px) {
            setPreferredSize(new Dimension(0, px));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final int w = getWidth();
                final int h = getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }
                if (model.getTargetSlots().contains(slot)) {
                    final boolean overlap = model.targetOverlapsUncertainty();
                    final double oBlend = overlap ? model.getTargetUncertaintyOverlapPortionOfSlot() : 1.0;
                    g2.setColor(DsumEncounterPaint.overlapStrengthGreen(115, 235, 125, 42, 185, 62, oBlend));
                    g2.fillRect(0, 0, w, h);
                    return;
                }
                final Color c =
                        switch (tone) {
                            case IDLE -> SlotPalette.mutedFillColor(slot);
                            case SUGGESTED_LIKELIEST -> SlotPalette.likelyAccent(slot);
                            case SUGGESTED_NEAR -> SlotPalette.nearLikelyAccent(slot);
                        };
                g2.setColor(c);
                g2.fillRect(0, 0, w, h);
            } finally {
                g2.dispose();
            }
        }
    }

    private void applyIdleAmberBar(final SlotSuggestionStrip bar) {
        bar.setIdle();
        bar.setPreferredSize(new Dimension(0, suggestionAmberH));
    }

    /**
     * Thin suggestion strip under the slot colour bar: paints only DSum sub-ranges where the slot overlaps the
     * uncertainty wedge (aligned with the wheel / linear bar).
     */
    private static final class SlotSuggestionStrip extends JPanel {

        private boolean idle = true;
        private EncounterWheelModel model;
        private EncounterSlot slot;
        private boolean targetGreen;
        private int distance;

        SlotSuggestionStrip() {
            setOpaque(false);
        }

        void setIdle() {
            idle = true;
            slot = null;
            model = null;
            setOpaque(false);
            repaint();
        }

        void configureSuggestion(
                final EncounterWheelModel model,
                final EncounterSlot slot,
                final boolean targetGreen,
                final int distanceFromLikeliest,
                final int barH) {
            idle = false;
            this.model = model;
            this.slot = slot;
            this.targetGreen = targetGreen;
            this.distance = distanceFromLikeliest;
            setOpaque(true);
            setPreferredSize(new Dimension(0, barH));
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (idle || slot == null || model == null) {
                return;
            }
            final OptionalDouble innerOv = model.encounterInnerWedgeOverlapPortionOfSlot(slot);
            final OptionalDouble outerOv = model.encounterOuterOnlyWedgeOverlapPortionOfSlot(slot);
            final List<int[]> innerSegs = model.suggestedInnerWedgeDsumSegmentsForSlot(slot);
            final List<int[]> outerSegs = model.suggestedOuterOnlyWedgeDsumSegmentsForSlot(slot);
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final int w = getWidth();
            final int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            final int sLo = slot.min();
            final int span = slot.max() - slot.min() + 1;
            final boolean hasInner = innerOv.isPresent() && innerOv.getAsDouble() > 1e-9;
            final boolean hasOuter = outerOv.isPresent() && outerOv.getAsDouble() > 1e-9;

            if (!hasInner && !hasOuter) {
                final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> sug = model.getSuggestedSlots();
                if (model.suggestedRunHasAnyWedgeOverlap(sug)) {
                    return;
                }
                final Color c = targetGreen
                        ? SuggestionStyle.greenTargetSuggestionStrip(distance, innerOv, outerOv)
                        : SuggestionStyle.amberSuggestedAmberBar(distance, innerOv, outerOv);
                g2.setColor(c);
                g2.fillRect(0, 0, w, h);
                return;
            }

            if (targetGreen) {
                if (hasOuter) {
                    final Color c = SuggestionStyle.greenTargetSuggestionStrip(
                            distance, OptionalDouble.empty(), outerOv);
                    fillLayer(g2, w, h, sLo, span, outerSegs, c, hasOuter);
                }
                if (hasInner) {
                    final Color c = SuggestionStyle.greenTargetSuggestionStrip(distance, innerOv, OptionalDouble.empty());
                    fillLayer(g2, w, h, sLo, span, innerSegs, c, hasInner);
                }
            } else {
                if (hasOuter) {
                    final Color c = SuggestionStyle.amberSuggestedAmberBar(
                            distance, OptionalDouble.empty(), outerOv);
                    fillLayer(g2, w, h, sLo, span, outerSegs, c, hasOuter);
                }
                if (hasInner) {
                    final Color c = SuggestionStyle.amberSuggestedAmberBar(distance, innerOv, OptionalDouble.empty());
                    fillLayer(g2, w, h, sLo, span, innerSegs, c, hasInner);
                }
            }
        }

        private static void fillLayer(
                final Graphics2D g2,
                final int w,
                final int h,
                final int sLo,
                final int span,
                final List<int[]> segs,
                final Color color,
                final boolean hasLayer) {
            g2.setColor(color);
            if (!hasLayer) {
                return;
            }
            if (segs.isEmpty()) {
                g2.fillRect(0, 0, w, h);
                return;
            }
            for (final int[] seg : segs) {
                final int lo = seg[0];
                final int hi = seg[1];
                int x0 = (int) Math.floor((lo - sLo) / (double) span * w);
                int x1 = (int) Math.ceil((hi - sLo + 1) / (double) span * w);
                x0 = Math.max(0, Math.min(w, x0));
                x1 = Math.max(0, Math.min(w, x1));
                if (x1 > x0) {
                    g2.fillRect(x0, 0, x1 - x0, h);
                }
            }
        }
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
    }


    private void updateTileIcon(final int index, final SlotTile tile) {
        final JToggleButton button = buttons.get(index);

        final float slotFontSp = compact ? 13f : 18f;
        final float levelFontSp = compact ? 10.5f : 13f;
        final TextIcon slotText = new TextIcon(button, "" + (index + 1), TextIcon.Layout.HORIZONTAL);
        slotText.setFont(slotText.getFont().deriveFont(Font.BOLD).deriveFont(slotFontSp));
        final TextIcon levelText = new TextIcon(button, "Lv. " + tile.encounter.level(), TextIcon.Layout.HORIZONTAL);
        levelText.setFont(levelText.getFont().deriveFont(levelFontSp));

        final Map<Species, ImageIcon> spriteMap = SpriteStorer.sprites.get(game);
        final Dimension spriteCell = SpriteStorer.spriteCellDimension(game);
        final ImageIcon rawSprite = spriteMap.get(tile.encounter.species());
        final Icon spriteRow = new CenteredBoxIcon(rawSprite, spriteCell.width, spriteCell.height);
        final CompoundIcon icon = new CompoundIcon(CompoundIcon.Axis.Y_AXIS,
                slotText, spriteRow, levelText);

        button.setIcon(icon);
    }

    private record SlotTile(Encounter encounter) {
    }

    private static final class SpriteStorer {
        private static final Map<Game, Map<Species, ImageIcon>> sprites = new EnumMap<>(Game.class);
        private static final Map<Game, SpriteCell> spriteCells = new EnumMap<>(Game.class);

        static {
            final Map<Species, ImageIcon> red = new EnumMap<>(Species.class);
            final Map<Species, ImageIcon> blue = new EnumMap<>(Species.class);
            final Map<Species, ImageIcon> yellow = new EnumMap<>(Species.class);

            for (final Species species : Species.values()) {
                final ImageIcon rbImage = SpriteImageUtil.loadWithTransparentBackground(SpriteStorer.class.getResource(
                        "/sprites/rb/%d.png".formatted(species.ordinal() + 1)));
                red.put(species, rbImage);
                blue.put(species, rbImage);

                yellow.put(species, SpriteImageUtil.loadWithTransparentBackground(SpriteStorer.class.getResource(
                        "/sprites/y/%d.png".formatted(species.ordinal() + 1))));
            }

            sprites.put(Game.RED, red);
            sprites.put(Game.BLUE, blue);
            sprites.put(Game.YELLOW, yellow);

            for (final Game g : Game.values()) {
                spriteCells.put(g, maxCellFor(sprites.get(g).values()));
            }
        }

        private static SpriteCell maxCellFor(final Collection<ImageIcon> icons) {
            int maxW = 0;
            int maxH = 0;
            for (final ImageIcon icon : icons) {
                if (icon != null) {
                    maxW = Math.max(maxW, icon.getIconWidth());
                    maxH = Math.max(maxH, icon.getIconHeight());
                }
            }
            return new SpriteCell(maxW, maxH);
        }

        private static Dimension spriteCellDimension(final Game game) {
            final SpriteCell c = spriteCells.get(game);
            return new Dimension(c.width(), c.height());
        }

        private record SpriteCell(int width, int height) {}
    }
}
