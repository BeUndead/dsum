package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SlotsSelectorPanel extends JPanel {

    private final JComboBox<Game> gameCombo = new JComboBox<>(Game.values());
    private final JComboBox<Route> routesCombo = new JComboBox<>(Route.values());

    public SlotsSelectorPanel(
            final Game game,
            final Route route,
            final Consumer<Game> onGameChanged,
            final Consumer<Route> onRouteChanged) {
        gameCombo.setSelectedItem(game);
        gameCombo.setRenderer(new RenamingListCellRenderer<>(Enum::name));
        gameCombo.addActionListener(e -> onGameChanged.accept((Game) gameCombo.getSelectedItem()));

        routesCombo.setSelectedItem(route);
        routesCombo.setRenderer(new RenamingListCellRenderer<>(r -> " " + r.name().replace("_",  " ")));
        routesCombo.addActionListener(e -> onRouteChanged.accept((Route) routesCombo.getSelectedItem()));

        add(gameCombo);
        add(routesCombo);
    }


    private class RenamingListCellRenderer<T> implements ListCellRenderer<T> {

        private final Function<T, String> renamer;

        private RenamingListCellRenderer(final Function<T, String> renamer) {
            this.renamer = renamer;
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends T> list,
                                                      final T value,
                                                      final int index,
                                                      final boolean isSelected,
                                                      final boolean cellHasFocus) {
            final String valueString = value == null ? "" : renamer.apply(value);
            final JLabel label = new JLabel(valueString);
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return label;
        }
    }
}
