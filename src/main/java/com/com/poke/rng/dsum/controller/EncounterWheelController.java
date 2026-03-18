package com.com.poke.rng.dsum.controller;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.model.view.EncounterWheel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;

import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

public final class EncounterWheelController {

    private static final int TIMER_MS = 16;

    private final EncounterWheelModel model;
    private final EncounterWheel wheel;
    private final OverlapHumPlayer humPlayer;

    private boolean lastOverlapGreen;

    public EncounterWheelController(
            final EncounterWheelModel model,
            final EncounterWheel wheel,
            final OverlapHumPlayer humPlayer
    ) {
        this.model = model;
        this.wheel = wheel;
        this.humPlayer = humPlayer;
    }

    public void start() {
        wheel.setFocusable(true);

        setupKeyBindings();

        final Timer timer = new Timer(TIMER_MS, e -> {
            model.update(System.nanoTime());
            updateWarningBeeps();
            updateOverlapHum();
            wheel.repaint();
        });
        timer.start();
    }

    private void setupKeyBindings() {
        wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "battleStart");
        wheel.getActionMap()
                .put("battleStart", new AbstractAction() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        model.battleStart();
                    }
                });

        for (int i = KeyEvent.VK_0; i <= KeyEvent.VK_9; i++) {

            final int slot = i == KeyEvent.VK_0 ? 10 : i - KeyEvent.VK_0;

            wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(i, 0), "calibrateSlot" + slot);
            wheel.getActionMap()
                    .put("calibrateSlot" + slot, new CalibrateSlotAction(slot));
        }

        wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "decreaseUncertainty");
        wheel.getActionMap()
                .put("decreaseUncertainty", new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.uncertaintyDelta(false);
                    }
                });
        wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "increaseUncertainty");
        wheel.getActionMap()
                .put("increaseUncertainty", new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.uncertaintyDelta(true);
                    }
                });

        wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, 0), "manualDown");
        wheel.getActionMap()
                .put("manualDown", new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.manualAngle(false);
                    }
                });
        wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, 0), "manualUp");
        wheel.getActionMap()
                .put("manualUp", new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.manualAngle(true);
                    }
                });
    }

    private void updateWarningBeeps() {
        if (model.consumeWarningBeep()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void updateOverlapHum() {
        final boolean overlap = model.targetOverlapsUncertainty();

        if (overlap && !lastOverlapGreen && !model.isCalibrating()) {
            humPlayer.start();
        } else if (!overlap && lastOverlapGreen) {
            humPlayer.stop();
        }

        lastOverlapGreen = overlap;
    }


    private final class CalibrateSlotAction extends AbstractAction {

        private final int slot;

        public CalibrateSlotAction(final int slot) {
            this.slot = slot;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.calibrateSlot(slot);
        }
    }
}
