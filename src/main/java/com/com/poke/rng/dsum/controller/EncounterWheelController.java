package com.com.poke.rng.dsum.controller;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.util.Triplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public final class EncounterWheelController {

    private static final int TIMER_MS = 16;
    /** Quiet period after changing target slots or finishing calibration (no encounters yet). */
    private static final long POST_CONFIGURE_SILENCE_NS = 1_500_000_000L;

    private final EncounterWheelModel model;
    private final JComponent[] repaintTargets;
    private final OverlapHumPlayer humPlayer;

    private final Consumer<Triplet<EncounterSlot, EncounterSlot, EncounterSlot>> onSuggestedChange;

    private Triplet<EncounterSlot, EncounterSlot, EncounterSlot> suggestedSlots;
    /** Previous tick had overlap hum playing (respecting mute / silence). */
    private boolean lastHumActive;

    private boolean userMuted;
    private long silenceUntilNs;

    /** Bits 0…2 = milestone beeps at ~0%, ~33%, ~66% fill for the current approach cycle. */
    private int approachBarBeepMask;
    private double lastApproachBarProgress = -1;

    public EncounterWheelController(
            final EncounterWheelModel model,
            final OverlapHumPlayer humPlayer,
            final Consumer<Triplet<EncounterSlot, EncounterSlot, EncounterSlot>> onSuggestedChange,
            final JComponent... repaintTargets
    ) {
        if (repaintTargets.length == 0) {
            throw new IllegalArgumentException("At least one repaint target is required");
        }
        this.model = model;
        this.repaintTargets = repaintTargets.clone();
        this.humPlayer = humPlayer;
        this.onSuggestedChange = onSuggestedChange;
    }

    public void setSoundMuted(final boolean muted) {
        userMuted = muted;
        if (muted) {
            humPlayer.stop();
            lastHumActive = false;
        }
    }

    /**
     * Call after target slots change or calibration number key so beeps/hum stay off briefly
     * (nothing to react to in-game yet).
     */
    public void notePostConfiguration() {
        silenceUntilNs = System.nanoTime() + POST_CONFIGURE_SILENCE_NS;
        humPlayer.stop();
        lastHumActive = false;
        approachBarBeepMask = 0;
    }

    private boolean soundsAudible() {
        return !userMuted && System.nanoTime() >= silenceUntilNs;
    }

    public void start() {
        for (final JComponent c : repaintTargets) {
            c.setFocusable(true);
        }

        installGlobalHotKeys();

        final Timer timer = new Timer(TIMER_MS, e -> {
            model.update(System.nanoTime());
            updateWarningBeeps();
            updateOverlapHum();
            checkForSlotSwap();
            for (final JComponent c : repaintTargets) {
                c.repaint();
            }
        });
        timer.start();
    }

    /**
     * Runs before the focused component (combo box, spinner, etc.) sees the key, so Space and other game keys
     * still work while UI controls retain focus.
     */
    private void installGlobalHotKeys() {
        final Window win = SwingUtilities.getWindowAncestor(repaintTargets[0]);
        if (win == null) {
            return;
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            final KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            final Window keyWin = kfm.getFocusedWindow();
            if (keyWin == null || !windowOwnedByOrIs(keyWin, win)) {
                return false;
            }
            final Component focus = kfm.getFocusOwner();
            if (focus instanceof JTextArea && ((JTextArea) focus).isEditable()) {
                return false;
            }

            final int ex = e.getModifiersEx();
            if ((ex & (InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0) {
                return false;
            }

            final boolean ctrl = (ex & InputEvent.CTRL_DOWN_MASK) != 0;
            final boolean shift = (ex & InputEvent.SHIFT_DOWN_MASK) != 0;

            return switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE -> {
                    model.battleStart(shift);
                    e.consume();
                    yield true;
                }
                case KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                        KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 -> {
                    if (shift) {
                        yield false;
                    }
                    final int slot = e.getKeyCode() == KeyEvent.VK_0 ? 10 : e.getKeyCode() - KeyEvent.VK_0;
                    final boolean wasCalibrating = model.isCalibrating();
                    model.calibrateSlot(slot, ctrl);
                    if (wasCalibrating) {
                        notePostConfiguration();
                    }
                    e.consume();
                    yield true;
                }
                case KeyEvent.VK_MINUS -> {
                    model.uncertaintyDelta(false);
                    e.consume();
                    yield true;
                }
                case KeyEvent.VK_EQUALS -> {
                    model.uncertaintyDelta(true);
                    e.consume();
                    yield true;
                }
                case KeyEvent.VK_OPEN_BRACKET -> {
                    model.manualAngle(false);
                    e.consume();
                    yield true;
                }
                case KeyEvent.VK_CLOSE_BRACKET -> {
                    model.manualAngle(true);
                    e.consume();
                    yield true;
                }
                case KeyEvent.VK_DELETE -> {
                    model.clearCalibrationState(System.nanoTime());
                    e.consume();
                    yield true;
                }
                default -> false;
            };
        });
    }

    private void updateWarningBeeps() {
        if (soundsAudible() && model.consumeWarningBeep()) {
            Toolkit.getDefaultToolkit().beep();
        }
        updateApproachBarMilestoneBeeps();
    }

    /**
     * Beeps at the start of fill (~0%), then at ⅓ and ⅔ of {@link EncounterWheelModel#getTargetOverlapApproachProgress()}
     * while the bar is filling (amber phase). Resets if the bar recedes or is not shown.
     */
    private void updateApproachBarMilestoneBeeps() {
        final double p = model.getTargetOverlapApproachProgress();
        final boolean overlap = model.targetOverlapsUncertainty();
        final boolean barInactive =
                model.getCalibratedSlot() == null || model.getTargetSlots().isEmpty() || model.isCalibrating();

        if (barInactive || overlap) {
            approachBarBeepMask = 0;
            lastApproachBarProgress = p;
            return;
        }

        if (lastApproachBarProgress >= 0 && p < lastApproachBarProgress - 0.02) {
            approachBarBeepMask = 0;
        }
        lastApproachBarProgress = p;

        final double oneThird = 1.0 / 3.0;
        final double twoThirds = 2.0 / 3.0;
        if (!soundsAudible()) {
            if (p > 0) {
                approachBarBeepMask |= 1 << 0;
            }
            if (p >= oneThird) {
                approachBarBeepMask |= 1 << 1;
            }
            if (p >= twoThirds) {
                approachBarBeepMask |= 1 << 2;
            }
            return;
        }
        final Toolkit tk = Toolkit.getDefaultToolkit();
        if (p > 0 && (approachBarBeepMask & (1 << 0)) == 0) {
            tk.beep();
            approachBarBeepMask |= 1 << 0;
        }
        if (p >= oneThird && (approachBarBeepMask & (1 << 1)) == 0) {
            tk.beep();
            approachBarBeepMask |= 1 << 1;
        }
        if (p >= twoThirds && (approachBarBeepMask & (1 << 2)) == 0) {
            tk.beep();
            approachBarBeepMask |= 1 << 2;
        }
    }

    private void updateOverlapHum() {
        final boolean overlap = model.targetOverlapsUncertainty();
        final boolean humShouldPlay = overlap && !model.isCalibrating() && soundsAudible();

        if (humShouldPlay && !lastHumActive) {
            humPlayer.start();
        } else if (!humShouldPlay && lastHumActive) {
            humPlayer.stop();
        }

        lastHumActive = humShouldPlay;
    }

    private void checkForSlotSwap() {

        final Triplet<Integer, Integer, Integer> dsumRange = model.getDsumRangeForSuggestedSlots();

        final EncounterSlot min = EncounterSlot.getSlot(dsumRange.first());
        final EncounterSlot likeliest = EncounterSlot.getSlot(dsumRange.second());
        final EncounterSlot max = EncounterSlot.getSlot(dsumRange.third());

        final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> newSuggested = new Triplet<>(min, likeliest, max);
        model.setSuggestedSlots(newSuggested);
        if (suggestedSlots != null && !suggestedSlots.equals(newSuggested)) {
            onSuggestedChange.accept(newSuggested);
        }
        suggestedSlots = newSuggested;
    }

    /** True if {@code w} is {@code appRoot} or is owned (directly or indirectly) by {@code appRoot}. */
    private static boolean windowOwnedByOrIs(final Window w, final Window appRoot) {
        for (Window x = w; x != null; x = x.getOwner()) {
            if (x == appRoot) {
                return true;
            }
        }
        return false;
    }
}
