package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.*;
import ext.CompoundIcon;
import ext.TextIcon;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SlotsDisplayPanel extends JPanel {

    private static final Color COLOR_LIKELY = new Color(234, 193, 157, 100);
    private static final Color COLOR_NEXT = new Color(189, 176, 159, 100);

    // TODO - Work out if the scaling is actually helpful...
    private static final double SCALE_NORMAL = 0.8;
    private static final double SCALE_LIKELY = 1;
    private static final double SCALE_NEXT = 1;

    private volatile Game game;
    private volatile Route route;

    private final List<JToggleButton> buttons = new ArrayList<>(10);
    private final List<SlotTile> tiles = new ArrayList<>();

    public SlotsDisplayPanel(final Game game, final Route route, final EncounterSlot initialSlot,
                             final Consumer<List<EncounterSlot>> onTargetsChanged) {
        this.game = game;
        this.route = route;

        setPreferredSize(new Dimension(500, 100));

        setLayout(new GridBagLayout());
        for (int i = 0; i < 10; i++) {
            final JToggleButton toggle = new JToggleButton();
            toggle.setPreferredSize(new Dimension(getWidth() / 10, getHeight()));
            if (i == initialSlot.ordinal()) {
                toggle.setSelected(true);
            }
            toggle.addActionListener(e -> {
                final List<EncounterSlot> newTargets = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    final JToggleButton b = buttons.get(j);
                    if (b.isSelected()) {
                        newTargets.add(EncounterSlot.values()[j]);
                    }
                }
                onTargetsChanged.accept(newTargets);
            });
            toggle.setBackground(Color.WHITE);
            buttons.add(toggle);
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = i;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.ipadx = 3;
            constraints.fill = GridBagConstraints.BOTH;
            add(toggle, constraints);
        }

        this.update();
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

    public void setLikelySlot(final EncounterSlot slot) {
        if (buttons.isEmpty()) {
            return;
        }
        buttons.forEach(b -> {
            b.setBackground(Color.WHITE);
            ((GridBagLayout) getLayout()).getConstraints(b).weightx = SCALE_NORMAL;
        });
        final int index = slot.ordinal();
        final JToggleButton button = buttons.get(index);
        final GridBagLayout layout = (GridBagLayout) getLayout();
        final GridBagConstraints c = layout.getConstraints(button);
        c.weightx = SCALE_LIKELY;
        button.setBackground(COLOR_LIKELY);
        remove(button);
        add(button, c);

        int nextDown = (index - 1) % buttons.size();
        nextDown = nextDown < 0 ? nextDown + buttons.size() : nextDown;
        int nextUp = (index + 1) % buttons.size();

        final JToggleButton buttonUp = buttons.get(nextUp);
        final GridBagConstraints cUp = layout.getConstraints(buttonUp);
        cUp.weightx = SCALE_NEXT;
        buttonUp.setBackground(COLOR_NEXT);
        remove(buttonUp);
        add(buttonUp, cUp);

        final JToggleButton buttonDown = buttons.get(nextDown);
        final GridBagConstraints cDown = layout.getConstraints(buttonDown);
        cDown.weightx = SCALE_NEXT;
        buttonDown.setBackground(COLOR_NEXT);
        remove(buttonDown);
        add(buttonDown, cDown);

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        this.setBackground(Color.WHITE);
    }


    private void updateTileIcon(final int index, final SlotTile tile) {
        final JToggleButton button = buttons.get(index);

        final TextIcon slotText = new TextIcon(button, "" + (index + 1), TextIcon.Layout.HORIZONTAL);
        slotText.setFont(slotText.getFont().deriveFont(Font.BOLD).deriveFont(18f));
        final TextIcon levelText = new TextIcon(button, "Lv. " + tile.encounter.level(), TextIcon.Layout.HORIZONTAL);
        levelText.setFont(levelText.getFont().deriveFont(13f));

        final Map<Species, ImageIcon> spriteMap = SpriteStorer.sprites.get(game);
        final CompoundIcon icon = new CompoundIcon(CompoundIcon.Axis.Y_AXIS,
                slotText, spriteMap.get(tile.encounter.species()), levelText);

        button.setIcon(icon);
    }

    private static class SlotTile {
        private final Encounter encounter;

        private SlotTile(final Encounter encounter) {
            this.encounter = encounter;
        }
    }

    private record GameAndSpecies(Game game, Species species) {}

    private static final class SpriteStorer {
        private static final Map<Game, Map<Species, ImageIcon>> sprites = new EnumMap<>(Game.class);

        static {
            final Map<Species, ImageIcon> red = new EnumMap<>(Species.class);
            final Map<Species, ImageIcon> blue = new EnumMap<>(Species.class);
            final Map<Species, ImageIcon> yellow = new EnumMap<>(Species.class);

            for (final Species species : Species.values()) {
                final ImageIcon rbImage = new ImageIcon(SpriteStorer.class.getResource(
                        "/sprites/rb/%d.png".formatted(species.ordinal() + 1)));
                red.put(species, rbImage);
                blue.put(species, rbImage);

                yellow.put(species, new ImageIcon(SpriteStorer.class.getResource(
                        "/sprites/y/%d.png".formatted(species.ordinal() + 1))));
            }

            sprites.put(Game.RED, red);
            sprites.put(Game.BLUE, blue);
            sprites.put(Game.YELLOW, yellow);
        }
    }
}
