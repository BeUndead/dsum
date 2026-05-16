package com.com.dsum.ui;

import com.com.dsum.model.Game;
import com.com.dsum.model.Named;
import com.com.dsum.model.Route;
import com.com.dsum.model.SelectionsConfig;

import javax.swing.*;
import java.awt.*;

public final class ConfigPanel extends JPanel {

    public ConfigPanel(final SelectionsConfig config) {
        final JComboBox<Game> gameSelector = new JComboBox<>(Game.values());
        gameSelector.setSelectedItem(config.getGame());
        gameSelector.setRenderer(new NamedListCellRenderer());
        gameSelector.addActionListener(e -> config.setGame((Game) gameSelector.getSelectedItem()));
        final JLabel gameLabel = new JLabel("Game");
        gameLabel.setLabelFor(gameSelector);

        final JComboBox<Route> routeSelector = new JComboBox<>(Route.values());
        routeSelector.setSelectedItem(config.getRoute());
        routeSelector.setRenderer(new NamedListCellRenderer());
        routeSelector.addActionListener(e -> config.setRoute((Route) routeSelector.getSelectedItem()));
        final JLabel routeLabel = new JLabel("Route");
        routeLabel.setLabelFor(routeSelector);

        final JSpinner leadSpinner = new JSpinner(new SpinnerNumberModel(config.getLeadLevel(), 1, 100, 1));
        leadSpinner.addChangeListener(e -> config.setLeadLevel(((Number) leadSpinner.getValue()).intValue()));
        final JLabel leadLabel = new JLabel("Lead Lv.");
        leadLabel.setLabelFor(leadSpinner);

        final JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(config.getThreshold(), 0.0, 1.0, 0.05));
        thresholdSpinner.setPreferredSize(new Dimension(leadSpinner.getPreferredSize().width, thresholdSpinner.getPreferredSize().height));
        thresholdSpinner.addChangeListener(e -> config.setThreshold(((Number) thresholdSpinner.getValue()).doubleValue()));
        final JLabel thresholdLabel = new JLabel("Threshold");
        thresholdLabel.setLabelFor(thresholdSpinner);

        setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = 0;
        c.weighty = 1.0;
        final Insets noSpace = new Insets(0, 0, 0, 0);
        final Insets leftSpace = new Insets(0, 5, 0, 0);

        c.insets = leftSpace;
        c.gridx = 0;
        c.weightx = 0.0;
        add(gameLabel, c);
        c.gridx = 1;
        c.weightx = 1.0;
        add(gameSelector, c);

        c.insets = leftSpace;
        c.gridx = 2;
        c.weightx = 0.0;
        add(routeLabel, c);
        c.insets = noSpace;
        c.gridx = 3;
        c.weightx = 1.0;
        add(routeSelector, c);

        c.insets = leftSpace;
        c.gridx = 4;
        c.weightx = 0.0;
        add(leadLabel, c);
        c.insets = noSpace;
        c.gridx = 5;
        c.weightx = 1.0;
        add(leadSpinner, c);

        c.insets = leftSpace;
        c.gridx = 6;
        c.weightx = 0.0;
        add(thresholdLabel, c);
        c.insets = noSpace;
        c.gridx = 7;
        c.weightx = 1.0;
        add(thresholdSpinner, c);
    }


    // Simple ListCellRenderer which supports using Named::getName as the display icon for the JComboBox-es above.
    private static final class NamedListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
            Object usedValue = value;
            if (value instanceof Named named) {
                usedValue = named.getName();
            }
            return super.getListCellRendererComponent(list, usedValue, index, isSelected, cellHasFocus);
        }
    }
}
