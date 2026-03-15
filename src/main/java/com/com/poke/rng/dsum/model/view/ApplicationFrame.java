package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.controller.EncounterWheelController;
import com.com.poke.rng.dsum.model.EncounterWheelModel;

import javax.swing.*;
import java.awt.*;

public final class ApplicationFrame extends JFrame {

    public ApplicationFrame() {
        final Game initialGame = Game.RED;
        final Route initialRoute = Route.ROUTE_22;
        final EncounterSlot initialTarget = EncounterSlot._4;

        final EncounterWheelModel model = new EncounterWheelModel(initialTarget);
        final EncounterWheel wheel = new EncounterWheel(model);
        final EncounterWheelController controller =
                new EncounterWheelController(model, wheel, new OverlapHumPlayer());

        final SlotsDisplayPanel slotsDisplayPanel = new SlotsDisplayPanel(initialGame, initialRoute, initialTarget, model::setTargetSlots);
        final SlotsSelectorPanel slotsSelectorPanel = new SlotsSelectorPanel(
                initialGame, initialRoute, slotsDisplayPanel::setGame, slotsDisplayPanel::setRoute);

        final JPanel content = new JPanel(new BorderLayout());
        content.add(slotsDisplayPanel, BorderLayout.NORTH);
        content.add(slotsSelectorPanel, BorderLayout.EAST);
        content.add(wheel, BorderLayout.CENTER);

        add(content);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);

        SwingUtilities.invokeLater(wheel::requestFocusInWindow);
        controller.start();
    }
}
