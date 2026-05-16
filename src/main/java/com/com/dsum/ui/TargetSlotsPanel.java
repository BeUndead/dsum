package com.com.dsum.ui;

import com.com.dsum.model.*;
import com.com.dsum.sim.DSumDriver;
import com.com.dsum.util.Colours;
import com.com.dsum.util.SpriteImageUtil;
import com.sun.jdi.request.MonitorContendedEnteredRequest;
import ext.CenteredBoxIcon;
import ext.CompoundIcon;
import ext.TextIcon;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class TargetSlotsPanel extends JPanel {

    private static final int CORNER_ROUNDNESS = 10;
    private static final int BAR_MARGIN = 5;

    private final java.util.List<SlotButton> slotButtons = new ArrayList<>(10);
    private final SelectionsConfig config;

    public TargetSlotsPanel(final SelectionsConfig config, final DSumDriver driver, final Colours colours) {
        this.config = config;

        setLayout(new BorderLayout(0, 0));

        final JPanel buttonRow = new JPanel(new GridLayout(1, EncounterSlot.values().length));
        final JPanel oddsRow = new JPanel(new GridLayout(1, EncounterSlot.values().length));
        setupButtons(buttonRow, oddsRow, driver, colours);

        add(buttonRow, BorderLayout.NORTH);
        add(oddsRow, BorderLayout.SOUTH);

        driver.registerUpdateListener(() -> SwingUtilities.invokeLater(oddsRow::repaint));

        this.config.registerGameChangeListener(game -> refreshButtons());
        this.config.registerRouteChangeListener(route -> refreshButtons());
    }

    private static Icon buildIcon(final Map<Species, ImageIcon> sprites, final Game game, final SlotButton button, final EncounterSlot slot, final Encounter encounter) {
        final TextIcon slotIcon = new TextIcon(button, slot.name().substring(1));
        slotIcon.setFont(new Font(slotIcon.getFont().getName(), Font.BOLD, 16));
        final SpriteStorer.SpriteCell cell = SpriteStorer.spriteCells.get(game);
        final Icon spriteIcon = new CenteredBoxIcon(sprites.get(encounter.species()), cell.width, cell.height);
        final TextIcon levelIcon = new TextIcon(button, "Lv. " + encounter.level());
        levelIcon.setFont(new Font(slotIcon.getFont().getName(), Font.PLAIN, 12));
        return new CompoundIcon(CompoundIcon.Axis.Y_AXIS, slotIcon, spriteIcon, levelIcon);
    }

    private void setupButtons(
            final JPanel buttonRow,
            final JPanel oddsRow,
            final DSumDriver driver,
            final Colours colours) {
        final Game game = this.config.getGame();
        final Route route = this.config.getRoute();

        final Set<EncounterSlot> targets = new HashSet<>(config.getTargets());
        for (final EncounterSlot slot : EncounterSlot.values()) {
            final Encounter encounter = route.encounterFor(game, slot);
            final SlotButton button = buildSlotButton(game, slot, encounter, targets);
            slotButtons.add(button);
            buttonRow.add(button);
            oddsRow.add(new SlotOddsBarCell(driver, colours, slot));
        }
    }

    private SlotButton buildSlotButton(final Game game, final EncounterSlot slot, final Encounter encounter, final Set<EncounterSlot> targets) {
        final SlotButton button = new SlotButton(slot, encounter);
        button.setSelected(targets.contains(slot));

        final Map<Species, ImageIcon> sprites = SpriteStorer.sprites.get(game);
        button.setIcon(buildIcon(sprites, game, button, slot, encounter));
        button.addActionListener(e -> {
            final Set<EncounterSlot> localTargets = new HashSet<>(config.getTargets());
            if (localTargets.contains(slot)) {
                localTargets.remove(slot);
            } else {
                localTargets.add(slot);
            }
            config.setTargets(localTargets);
        });
        return button;
    }

    private void refreshButtons() {
        final Game game = this.config.getGame();
        final Route route = this.config.getRoute();

        for (final SlotButton slotButton : slotButtons) {
            slotButton.setEncounter(route.encounterFor(game, slotButton.slot));
        }
        SwingUtilities.invokeLater(() -> {
            this.revalidate();
            this.repaint();
        });
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

        private record SpriteCell(int width, int height) {
        }
    }

    private static final class SlotOddsBarCell extends JPanel {

        private static final int PREFERRED_ODDS_ROW_HEIGHT = 44;

        private final DSumDriver driver;
        private final Colours colours;
        private final EncounterSlot slot;

        private SlotOddsBarCell(final DSumDriver driver, final Colours colours, final EncounterSlot slot) {
            this.driver = driver;
            this.colours = colours;
            this.slot = slot;
            setOpaque(true);
            setBackground(colours.oddsBarBackground());
            setPreferredSize(new Dimension(0, PREFERRED_ODDS_ROW_HEIGHT));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);

            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final double[] slotP = driver.getCurrentEncounterSlotProbabilities();
            final double p = slotP[slot.ordinal()];

            final int w = getWidth();
            final int h = getHeight();
            final int labelPad = 2;
            final int labelH = Math.min(14, Math.max(10, g2.getFontMetrics().getHeight()));
            final int chartH = Math.max(0, h - labelH - labelPad);

            final int barH = Math.max(0, (int) Math.round(p * chartH));
            final int baseline = labelPad + chartH;
            final int y = baseline - barH;

            final Color colour = colours.fillColor(slot);
            g2.setColor(colour);
            g2.fillRoundRect(BAR_MARGIN, y, w - 2 * BAR_MARGIN, barH, CORNER_ROUNDNESS, CORNER_ROUNDNESS);
            g2.setStroke(new BasicStroke(3.0f));
            g2.setColor(colour.darker().darker());
            g2.drawRoundRect(BAR_MARGIN, y, w - 2 * BAR_MARGIN, barH, CORNER_ROUNDNESS, CORNER_ROUNDNESS);

            final String label = "%.0f%%".formatted(p * 100.0);
            final int tw = g2.getFontMetrics().stringWidth(label);
            g2.setColor(colour.brighter());
            g2.drawString(label, Math.max(0, (w - tw) / 2), h - 2);
        }
    }

    private final class SlotButton extends JToggleButton {

        private final EncounterSlot slot;
        private volatile Encounter encounter;

        private SlotButton(final EncounterSlot slot, final Encounter encounter) {
            this.slot = slot;
            this.encounter = encounter;
        }

        private void setEncounter(final Encounter encounter) {
            this.encounter = encounter;
            updateIcon();
        }

        private void updateIcon() {
            final Game game = config.getGame();
            final Map<Species, ImageIcon> sprites = SpriteStorer.sprites.get(game);
            final Icon icon = buildIcon(sprites, game, this, slot, encounter);
            this.setIcon(icon);
        }
    }
}
