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
    private final Runnable requestRepaint;

    public CalibrationKeyboard(
            final EncounterWheelModel model,
            final Runnable notePostConfiguration,
            final Runnable requestRepaint) {
        this.model = model;
        this.notePostConfiguration = notePostConfiguration;
        this.requestRepaint = requestRepaint;
    }

    public static boolean isCalibrationKey(final int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_SPACE,
                 KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                 KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,
                 KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS,
                 KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_CLOSE_BRACKET,
                 KeyEvent.VK_DELETE,
                 KeyEvent.VK_P -> true;
            default -> false;
        };
    }

    /**
     * Foreground routing: {@code R}/{@code B}/{@code A}/{@code T}/{@code N} count as calibration input only while
     * calibrating (post-clear timing before the slot digit).
     */
    public static boolean isCalibrationDispatchKey(final int keyCode, final boolean calibrating) {
        return isCalibrationKey(keyCode)
                || (calibrating
                        && (keyCode == KeyEvent.VK_R
                                || keyCode == KeyEvent.VK_B
                                || keyCode == KeyEvent.VK_A
                                || keyCode == KeyEvent.VK_T
                                || keyCode == KeyEvent.VK_N));
    }

    public void handleKeyCommand(final int keyCode, final int modifiersEx) {
        if ((modifiersEx & (InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0) {
            return;
        }

        final boolean shift = (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;

        switch (keyCode) {
            case KeyEvent.VK_R -> {
                if (shift) {
                    return;
                }
                if (model.isCalibrating()) {
                    model.armPostClearRanAwayTiming();
                    requestRepaint.run();
                }
            }
            case KeyEvent.VK_B -> {
                if (shift) {
                    return;
                }
                if (model.isCalibrating()) {
                    model.armPostClearSentToBoxTiming();
                    requestRepaint.run();
                }
            }
            case KeyEvent.VK_A -> {
                if (shift) {
                    return;
                }
                if (model.isCalibrating()) {
                    model.armPostClearNicknameTiming();
                    requestRepaint.run();
                }
            }
            case KeyEvent.VK_T -> {
                if (shift) {
                    return;
                }
                if (model.isCalibrating()) {
                    model.armPostClearJoinPartyTiming();
                    requestRepaint.run();
                }
            }
            case KeyEvent.VK_N -> {
                if (shift) {
                    return;
                }
                if (model.isCalibrating()) {
                    model.resetPostClearTimingToNormal();
                    requestRepaint.run();
                }
            }
            case KeyEvent.VK_P -> {
                if (shift) {
                    return;
                }
                model.toggleSimulationPaused();
                requestRepaint.run();
            }
            case KeyEvent.VK_SPACE -> model.battleStart(shift);
            case KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                 KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 -> {
                if (shift) {
                    return;
                }
                final int slot = keyCode == KeyEvent.VK_0 ? 10 : keyCode - KeyEvent.VK_0;
                final boolean wasCalibrating = model.isCalibrating();
                model.calibrateSlot(slot);
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
