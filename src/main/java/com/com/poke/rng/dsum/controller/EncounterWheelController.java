package com.com.poke.rng.dsum.controller;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.model.view.EncounterWheel;
import com.com.poke.rng.dsum.util.Triplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

public final class EncounterWheelController {

    private static final int TIMER_MS = 16;

    private final EncounterWheelModel model;
    private final EncounterWheel wheel;
    private final OverlapHumPlayer humPlayer;

    private final Consumer<Triplet<EncounterSlot, EncounterSlot, EncounterSlot>> onSuggestedChange;

    private Triplet<EncounterSlot, EncounterSlot, EncounterSlot> suggestedSlots;
    private boolean lastOverlapGreen;

    public EncounterWheelController(
            final EncounterWheelModel model,
            final EncounterWheel wheel,
            final OverlapHumPlayer humPlayer,
            final Consumer<Triplet<EncounterSlot, EncounterSlot, EncounterSlot>> onSuggestedChange
    ) {
        this.model = model;
        this.wheel = wheel;
        this.humPlayer = humPlayer;
        this.onSuggestedChange = onSuggestedChange;
    }

    public void start() {
        wheel.setFocusable(true);

        setupKeyBindings();

        final Timer timer = new Timer(TIMER_MS, e -> {
            model.update(System.nanoTime());
            updateWarningBeeps();
            updateOverlapHum();
            checkForSlotSwap();
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
                        model.battleStart(false);
                    }
                });
        wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "battleStartAlt");
        wheel.getActionMap()
                .put("battleStartAlt", new AbstractAction() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        model.battleStart(true);
                    }
                });

        for (int i = KeyEvent.VK_0; i <= KeyEvent.VK_9; i++) {

            final int slot = i == KeyEvent.VK_0 ? 10 : i - KeyEvent.VK_0;

            wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(i, 0), "calibrateSlot" + slot);
            wheel.getActionMap()
                    .put("calibrateSlot" + slot, new CalibrateSlotAction(false, slot));

            wheel.getInputMap(WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(i, InputEvent.CTRL_DOWN_MASK), "recalibrateSlot" + slot);
            wheel.getActionMap()
                    .put("recalibrateSlot" + slot, new CalibrateSlotAction(true, slot));
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
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, 0), "manualDown");
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

    private void checkForSlotSwap() {

        final Triplet<Integer, Integer, Integer> dsumRange;
        if (model.isCalibrating()) {
            dsumRange = model.getDsumRangeAtStartOfBattle();
        } else {
            dsumRange = model.getDsumRange();
        }

        final EncounterSlot min = EncounterSlot.getSlot(dsumRange.first());
        final EncounterSlot likeliest = EncounterSlot.getSlot(dsumRange.second());
        final EncounterSlot max = EncounterSlot.getSlot(dsumRange.third());

        final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> newSuggested = new Triplet<>(min, likeliest, max);
        if (suggestedSlots != null && !suggestedSlots.equals(newSuggested)) {
            onSuggestedChange.accept(newSuggested);
        }
        suggestedSlots = newSuggested;
    }


    private final class CalibrateSlotAction extends AbstractAction {

        private final boolean recalibrate;
        private final int slot;

        public CalibrateSlotAction(final boolean recalibrate, final int slot) {
            this.recalibrate = recalibrate;
            this.slot = slot;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            model.calibrateSlot(slot, recalibrate);
        }
    }
}
