package com.com.poke.rng.dsum.controller;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.model.view.EncounterWheel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public final class EncounterWheelController {

    private static final int TIMER_MS = 1;

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
        wheel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                final boolean wasPaused = model.isCalibrating();
                model.handleKeyPress(e, System.nanoTime());
                final boolean isPaused = model.isCalibrating();

                if (e.getKeyCode() == KeyEvent.VK_SPACE || (wasPaused && !isPaused)) {
                    humPlayer.stop();
                }

                wheel.repaint();
            }
        });

        final Timer timer = new Timer(TIMER_MS, e -> {
            model.update(System.nanoTime());
            updateWarningBeeps();
            updateOverlapHum();
            wheel.repaint();
        });
        timer.start();
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
}
