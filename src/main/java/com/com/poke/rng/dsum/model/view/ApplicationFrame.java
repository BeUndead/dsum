package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.controller.EncounterWheelController;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public final class ApplicationFrame extends JFrame {

    public ApplicationFrame() {
        final Game initialGame = Game.RED;
        final Route initialRoute = Route.SAFARI_ZONE_CENTER;
        final EncounterSlot initialTarget = EncounterSlot._9;
        final boolean initialPika = true;
        final boolean initialOnBike = true;
        final int initialLeadLevel = 70;

        final EncounterWheelModel model = new EncounterWheelModel(initialTarget, initialGame);
        model.setRoute(initialRoute);
        final EncounterWheel wheel = new EncounterWheel(model);
        final EncounterWheelBar wheelBar = new EncounterWheelBar(model);

        final SlotsDisplayPanel slotsDisplayPanel = new SlotsDisplayPanel(initialGame, initialRoute, initialTarget, model::setTargetSlots);

        final EncounterWheelController controller =
                new EncounterWheelController(model, new OverlapHumPlayer(), slotsDisplayPanel::setSuggestedSlots, wheel, wheelBar);
        model.setOnTargetSlotsChanged(controller::notePostConfiguration);
        final SlotsSelectorPanel slotsSelectorPanel = new SlotsSelectorPanel(
                initialGame, initialRoute, initialPika, initialOnBike,
                initialLeadLevel,
                game -> {
                    slotsDisplayPanel.setGame(game);
                    model.setGame(game);
                },
                newRoute -> {
                    slotsDisplayPanel.setRoute(newRoute);
                    model.setRoute(newRoute);
                },
                model::setPikaLead,
                model::modifyYellowOverworldDsumCycleModifier,
                model::setLeadLevel,
                controller::setSoundMuted,
                model::setOnBike
        );

        final JPanel viewPick = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        viewPick.setOpaque(false);
        final JLabel viewLabel = new JLabel("View:");
        viewLabel.setForeground(UiTheme.TEXT_MUTED);
        viewPick.add(viewLabel);
        final JComboBox<String> viewMode = new JComboBox<>(new String[] {"Full", "Compact"});
        viewMode.putClientProperty(FlatClientProperties.STYLE_CLASS, "compact");
        viewMode.setPreferredSize(new Dimension(108, 30));
        viewPick.add(viewMode);
        slotsSelectorPanel.addLeadingToolbar(viewPick);
        // Single child so preferred height is not max(wheel, bar) — CardLayout would keep 500×500 with the bar.
        final JPanel centerHost = new JPanel(new BorderLayout());
        centerHost.setOpaque(false);
        centerHost.add(wheel, BorderLayout.CENTER);

        viewMode.addActionListener(e -> {
            final boolean minimal = viewMode.getSelectedIndex() == 1;
            centerHost.removeAll();
            if (minimal) {
                wheelBar.setStripMetrics(
                        EncounterWheel.diameterForGeomScale(EncounterWheel.COMPACT_GEOM_SCALE),
                        SlotsDisplayPanel.COMPACT_PREFERRED_HEIGHT);
                centerHost.add(wheelBar, BorderLayout.CENTER);
            } else {
                wheelBar.setStripMetrics(500, EncounterWheelBar.DEFAULT_PREFERRED_HEIGHT);
                centerHost.add(wheel, BorderLayout.CENTER);
            }
            centerHost.revalidate();
            slotsDisplayPanel.setCompact(minimal);
            slotsSelectorPanel.setCompactChrome(minimal);
            SwingUtilities.invokeLater(() -> {
                pack();
                (minimal ? wheelBar : wheel).requestFocusInWindow();
            });
        });

        final JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(UiTheme.SURFACE_ALT);
        final Border pad = BorderFactory.createEmptyBorder(12, 16, 16, 16);
        content.setBorder(pad);

        final JPanel slots = new JPanel(new BorderLayout());
        slots.setOpaque(false);
        slots.add(slotsDisplayPanel, BorderLayout.SOUTH);
        slots.add(slotsSelectorPanel, BorderLayout.NORTH);
        content.add(slots, BorderLayout.NORTH);
        content.add(centerHost, BorderLayout.CENTER);

        add(content);

        setTitle("DSum");
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);

        SwingUtilities.invokeLater(wheel::requestFocusInWindow);
        controller.start();
    }
}
