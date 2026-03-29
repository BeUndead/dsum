package com.com.poke.rng.dsum.controller;

import com.com.poke.rng.dsum.model.EncounterWheelModel;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Maps hardware / Swing key codes and modifier masks to {@link EncounterWheelModel} calibration actions.
 */
public final class CalibrationKeyboard {

    private final EncounterWheelModel model;
    private final Runnable notePostConfiguration;

    public CalibrationKeyboard(final EncounterWheelModel model, final Runnable notePostConfiguration) {
        this.model = model;
        this.notePostConfiguration = notePostConfiguration;
    }

    public static boolean isCalibrationKey(final int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_SPACE,
                 KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                 KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,
                 KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS,
                 KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_CLOSE_BRACKET,
                 KeyEvent.VK_DELETE -> true;
            default -> false;
        };
    }

    public void handleKeyCommand(final int keyCode, final int modifiersEx) {
        if ((modifiersEx & (InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0) {
            return;
        }

        final boolean ctrl = (modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0;
        final boolean shift = (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;

        switch (keyCode) {
            case KeyEvent.VK_SPACE -> model.battleStart(shift);
            case KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                 KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 -> {
                if (shift) {
                    return;
                }
                final int slot = keyCode == KeyEvent.VK_0 ? 10 : keyCode - KeyEvent.VK_0;
                final boolean wasCalibrating = model.isCalibrating();
                model.calibrateSlot(slot, ctrl);
                if (wasCalibrating) {
                    notePostConfiguration.run();
                }
            }
            case KeyEvent.VK_MINUS -> model.uncertaintyDelta(false);
            case KeyEvent.VK_EQUALS -> model.uncertaintyDelta(true);
            case KeyEvent.VK_OPEN_BRACKET -> model.manualAngle(false);
            case KeyEvent.VK_CLOSE_BRACKET -> model.manualAngle(true);
            case KeyEvent.VK_DELETE -> model.clearCalibrationState(System.nanoTime());
            default -> {}
        }
    }
}
