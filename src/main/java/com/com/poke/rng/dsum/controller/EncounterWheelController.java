package com.com.poke.rng.dsum.controller;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.util.Triplet;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

public final class EncounterWheelController implements NativeKeyListener {

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

    /**
     * When false (default), keys are handled via Swing on the wheel / bar when they have focus.
     * When true, JNativeHook captures keys globally (works when another app is focused).
     */
    private boolean globalHookActive;

    private final KeyListener swingKeyListener = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            handleKeyCommand(e.getKeyCode(), e.getModifiersEx());
        }
    };

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

    /**
     * Exactly one path is active: global native capture or per-component Swing listeners on the wheel / bar.
     *
     * @throws RuntimeException if enabling background capture but the native hook could not be registered
     */
    public void setBackgroundKeyboardCapture(final boolean enabled) {
        if (enabled == globalHookActive) {
            return;
        }
        if (enabled) {
            // Background: drop Swing listeners first so keys are not handled twice once the hook runs.
            detachSwingKeyListeners();
            unregisterNativeKeyboardCapture();
            try {
                registerNativeKeyboardCapture();
                globalHookActive = true;
            } catch (final RuntimeException ex) {
                attachSwingKeyListeners();
                globalHookActive = false;
                throw ex;
            }
        } else {
            // Swing: tear down native capture completely, then attach listeners.
            unregisterNativeKeyboardCapture();
            detachSwingKeyListeners();
            attachSwingKeyListeners();
            globalHookActive = false;
        }
    }

    public boolean isBackgroundKeyboardCapture() {
        return globalHookActive;
    }

    private void registerNativeKeyboardCapture() {
        GlobalScreen.setEventDispatcher(new SwingDispatchService());
        try {
            GlobalScreen.registerNativeHook();
        } catch (final NativeHookException nhEx) {
            throw new RuntimeException(nhEx);
        }
        GlobalScreen.addNativeKeyListener(this);
    }

    /** Remove this controller from the native hook and unregister GlobalScreen so no global capture remains. */
    private void unregisterNativeKeyboardCapture() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
        } catch (final Exception ignored) {
            // listener may not be registered
        }
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.unregisterNativeHook();
            }
        } catch (final NativeHookException nhEx) {
            throw new RuntimeException(nhEx);
        }
    }

    private void attachSwingKeyListeners() {
        for (final JComponent c : repaintTargets) {
            c.removeKeyListener(swingKeyListener);
            c.addKeyListener(swingKeyListener);
        }
    }

    /** Remove Swing key handling from wheel / bar (used when switching to background capture). */
    private void detachSwingKeyListeners() {
        for (final JComponent c : repaintTargets) {
            c.removeKeyListener(swingKeyListener);
        }
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
        attachSwingKeyListeners();

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

    @Override
    public void nativeKeyPressed(final NativeKeyEvent e) {
        handleKeyCommand(e.getRawCode(), e.getModifiers());
    }

    private void handleKeyCommand(final int keyCode, final int modifiersEx) {
        if ((modifiersEx & (InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0) {
            return;
        }

        final boolean ctrl = (modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0;
        final boolean shift = (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;

        switch (keyCode) {
            case KeyEvent.VK_SPACE -> {
                model.battleStart(shift);
            }
            case KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                 KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 -> {
                if (shift) {
                    return;
                }
                final int slot = keyCode == KeyEvent.VK_0 ? 10 : keyCode - KeyEvent.VK_0;
                final boolean wasCalibrating = model.isCalibrating();
                model.calibrateSlot(slot, ctrl);
                if (wasCalibrating) {
                    notePostConfiguration();
                }
            }
            case KeyEvent.VK_MINUS -> {
                model.uncertaintyDelta(false);
            }
            case KeyEvent.VK_EQUALS -> {
                model.uncertaintyDelta(true);
            }
            case KeyEvent.VK_OPEN_BRACKET -> {
                model.manualAngle(false);
            }
            case KeyEvent.VK_CLOSE_BRACKET -> {
                model.manualAngle(true);
            }
            case KeyEvent.VK_DELETE -> {
                model.clearCalibrationState(System.nanoTime());
            }
            default -> {}
        }
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
