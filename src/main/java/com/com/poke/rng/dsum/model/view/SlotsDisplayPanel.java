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

    private Game game;
    private Route route;

    private final List<JToggleButton> buttons = new ArrayList<>(10);
    private final List<SlotTile> tiles = new ArrayList<>();

    public SlotsDisplayPanel(final Game game, final Route route, final EncounterSlot initialSlot,
                             final Consumer<List<EncounterSlot>> onTargetsChanged) {
        this.game = game;
        setRoute(route);

        setPreferredSize(new Dimension(500, 100));

        setLayout(new GridLayout(1, 10));
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
            add(toggle);
        }
    }


    public void setGame(final Game game) {
        this.game = game;

        synchronized (tiles) {
            this.tiles.clear();

            final Map<EncounterSlot, Encounter> encounterMap = route.getEncounters().get(game);
            for (final EncounterSlot slot : EncounterSlot.values()) {
                tiles.add(new SlotTile(encounterMap.get(slot)));
            }
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setRoute(final Route route) {
        this.route = route;

        synchronized (tiles) {
            this.tiles.clear();

            final Map<EncounterSlot, Encounter> encounterMap = route.getEncounters().get(game);
            for (final EncounterSlot slot : EncounterSlot.values()) {
                tiles.add(new SlotTile(encounterMap.get(slot)));
            }
        }
        SwingUtilities.invokeLater(this::repaint);
    }


    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        this.setBackground(Color.WHITE);
        final List<SlotTile> localTiles;
        synchronized (tiles) {
            localTiles = new ArrayList<>(tiles);
        }
        for (int i = 0; i < localTiles.size(); i++) {
            drawTile(i, localTiles.get(i), g);
        }
    }


    private void drawTile(final int index, final SlotTile tile, final Graphics g) {
        final JToggleButton button = buttons.get(index);

        final TextIcon slotText = new TextIcon(button, "" + (index + 1), TextIcon.Layout.HORIZONTAL);
        slotText.setFont(slotText.getFont().deriveFont(Font.BOLD).deriveFont(18f));
        final TextIcon levelText = new TextIcon(button, "Lv. " + tile.encounter.level(), TextIcon.Layout.HORIZONTAL);
        levelText.setFont(levelText.getFont().deriveFont(13f));
        final CompoundIcon icon = new CompoundIcon(CompoundIcon.Axis.Y_AXIS,
                slotText, SpriteStorer.sprites.get(tile.encounter.species()), levelText);

        button.setIcon(icon);
    }

    private static class SlotTile {
        private final Encounter encounter;

        private SlotTile(final Encounter encounter) {
            this.encounter = encounter;
        }
    }

    private static final class SpriteStorer {
        private static final Map<Species, ImageIcon> sprites = new EnumMap<>(Species.class);

        static {
            for (final Species species : Species.values()) {
                sprites.put(species, new ImageIcon(SpriteStorer.class.getResource("/sprites/%04d.png".formatted(species.ordinal() + 1))));
            }
        }
    }
}
