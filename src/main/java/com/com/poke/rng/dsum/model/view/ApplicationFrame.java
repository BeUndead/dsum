package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.controller.EncounterWheelController;
import com.com.poke.rng.dsum.model.EncounterWheelModel;

import javax.swing.*;
import java.awt.*;

public final class ApplicationFrame extends JFrame {

    public ApplicationFrame() {
        final EncounterSlot initialTarget = EncounterSlot._9;
        final EncounterWheelModel model = new EncounterWheelModel(initialTarget);
        final EncounterWheel wheel = new EncounterWheel(model);
        final EncounterWheelController controller =
                new EncounterWheelController(model, wheel, new OverlapHumPlayer());

        final TargetSlotPanel targetSlotPanel = new TargetSlotPanel(initialTarget, selected -> {
            model.setTargetSlots(selected);
            wheel.repaint();
            wheel.requestFocusInWindow();
        });

        final JPanel content = new JPanel(new BorderLayout());
        content.add(targetSlotPanel, BorderLayout.NORTH);
        content.add(wheel, BorderLayout.CENTER);

        add(content);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);

        SwingUtilities.invokeLater(wheel::requestFocusInWindow);
        controller.start();
    }
}
