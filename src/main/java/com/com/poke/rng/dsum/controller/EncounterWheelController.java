package com.com.poke.rng.dsum.controller;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.util.Triplet;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
    private volatile boolean lastHumActive;

    private volatile boolean userMuted;
    private volatile long silenceUntilNs;

    /** Bits 0…2 = milestone beeps at start, ⅓ and ⅔ of the <em>remaining</em> time until overlap for this cycle. */
    private volatile int approachBarBeepMask;
    private volatile double lastApproachBarProgress = -1;
    /** Remaining-ns snapshot when the current approach cycle began; used so thirds are even in wall time. */
    private volatile long approachTimeEntryNs = -1L;

    /**
     * When false (default), calibration keys are handled via a {@link KeyboardFocusManager} dispatcher
     * for the main window (except editable fields and open popup menus).
     * When true, JNativeHook captures keys globally (works when another app is focused).
     */
    private volatile boolean globalHookActive;

    /** Main host window for foreground dispatch; modal dialogs use a different focused window and are excluded. */
    private volatile Window calibrationRootWindow;

    private volatile KeyEventDispatcher swingWindowKeyDispatcher;

    private final CalibrationKeyboard calibrationKeys;

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
        this.calibrationKeys = new CalibrationKeyboard(model, this::notePostConfiguration, this::requestRepaintTargets);
    }

    /**
     * Exactly one path is active: global native capture or foreground {@link KeyEventDispatcher} on the main window.
     *
     * @throws RuntimeException if enabling background capture but the native hook could not be registered
     */
    public void setBackgroundKeyboardCapture(final boolean enabled) {
        if (enabled == globalHookActive) {
            return;
        }
        if (enabled) {
            detachForegroundSwingInput();
            unregisterNativeKeyboardCapture();
            try {
                registerNativeKeyboardCapture();
                globalHookActive = true;
            } catch (final RuntimeException ex) {
                attachForegroundSwingInput();
                globalHookActive = false;
                throw ex;
            }
        } else {
            unregisterNativeKeyboardCapture();
            detachForegroundSwingInput();
            attachForegroundSwingInput();
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

    /**
     * Foreground (non-native) mode: listen for calibration keys anywhere in the main window except text entry
     * and popup lists (e.g. combo dropdowns).
     */
    private void attachForegroundSwingInput() {
        calibrationRootWindow = SwingUtilities.getWindowAncestor(repaintTargets[0]);
        if (calibrationRootWindow == null || swingWindowKeyDispatcher != null) {
            return;
        }
        swingWindowKeyDispatcher = this::dispatchCalibrationKeyWhenForegroundSwing;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(swingWindowKeyDispatcher);
    }

    private void detachForegroundSwingInput() {
        if (swingWindowKeyDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(swingWindowKeyDispatcher);
            swingWindowKeyDispatcher = null;
        }
    }

    /**
     * Move keyboard focus to the visible wheel or strip so arrow/tab affordances are consistent after UI picks.
     * Does nothing in background (native) mode.
     */
    public void requestCalibrationSurfaceFocus() {
        if (globalHookActive) {
            return;
        }
        for (final JComponent c : repaintTargets) {
            if (c.isDisplayable() && c.isShowing()) {
                SwingUtilities.invokeLater(c::requestFocusInWindow);
                return;
            }
        }
    }

    /**
     * @return {@code true} if the event should not be dispatched further (handled as calibration input)
     */
    private boolean dispatchCalibrationKeyWhenForegroundSwing(final KeyEvent e) {
        if (globalHookActive) {
            return false;
        }
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }
        final KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (calibrationRootWindow == null || kfm.getFocusedWindow() != calibrationRootWindow) {
            return false;
        }
        final Component focusOwner = kfm.getFocusOwner();
        if (focusOwner == null) {
            return false;
        }
        if (focusOwner instanceof JTextComponent jt && jt.isEditable()) {
            if (!allowCalibrationKeyBypassingEditableText(e)) {
                return false;
            }
        }
        if (SwingUtilities.getAncestorOfClass(JPopupMenu.class, focusOwner) != null) {
            return false;
        }
        if (!CalibrationKeyboard.isCalibrationKey(e.getKeyCode())) {
            return false;
        }
        calibrationKeys.handleKeyCommand(e.getKeyCode(), e.getModifiersEx());
        e.consume();
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            requestCalibrationSurfaceFocus();
        }
        return true;
    }

    /**
     * Spinner editors etc. use editable {@link JTextComponent}s; we still want Space→encounter from the main window.
     * While already in battle, every calibration shortcut should work even if focus stayed in a numeric field.
     */
    private boolean allowCalibrationKeyBypassingEditableText(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_P) {
            return true;
        }
        return model.isCalibrating() && CalibrationKeyboard.isCalibrationKey(e.getKeyCode());
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

    private void requestRepaintTargets() {
        for (final JComponent c : repaintTargets) {
            c.repaint();
        }
    }

    private boolean soundsAudible() {
        return !userMuted && System.nanoTime() >= silenceUntilNs;
    }

    public void start() {
        for (final JComponent c : repaintTargets) {
            c.setFocusable(true);
        }
        attachForegroundSwingInput();

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
        calibrationKeys.handleKeyCommand(
                swingKeyCodeFromNativeRaw(e.getRawCode()),
                awtModifiersExFromNative(e.getModifiers()));
    }

    private static int awtModifiersExFromNative(final int nativeMods) {
        int ex = 0;
        if ((nativeMods & NativeInputEvent.SHIFT_MASK) != 0) {
            ex |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((nativeMods & NativeInputEvent.CTRL_MASK) != 0) {
            ex |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((nativeMods & NativeInputEvent.META_MASK) != 0) {
            ex |= InputEvent.META_DOWN_MASK;
        }
        if ((nativeMods & NativeInputEvent.ALT_MASK) != 0) {
            ex |= InputEvent.ALT_DOWN_MASK;
        }
        return ex;
    }

    private static int swingKeyCodeFromNativeRaw(final int raw) {
        return switch (raw) {
            case NativeKeyEvent.VC_SPACE -> KeyEvent.VK_SPACE;
            case NativeKeyEvent.VC_1 -> KeyEvent.VK_1;
            case NativeKeyEvent.VC_2 -> KeyEvent.VK_2;
            case NativeKeyEvent.VC_3 -> KeyEvent.VK_3;
            case NativeKeyEvent.VC_4 -> KeyEvent.VK_4;
            case NativeKeyEvent.VC_5 -> KeyEvent.VK_5;
            case NativeKeyEvent.VC_6 -> KeyEvent.VK_6;
            case NativeKeyEvent.VC_7 -> KeyEvent.VK_7;
            case NativeKeyEvent.VC_8 -> KeyEvent.VK_8;
            case NativeKeyEvent.VC_9 -> KeyEvent.VK_9;
            case NativeKeyEvent.VC_0 -> KeyEvent.VK_0;
            case NativeKeyEvent.VC_MINUS -> KeyEvent.VK_MINUS;
            case NativeKeyEvent.VC_EQUALS -> KeyEvent.VK_EQUALS;
            case NativeKeyEvent.VC_OPEN_BRACKET -> KeyEvent.VK_OPEN_BRACKET;
            case NativeKeyEvent.VC_CLOSE_BRACKET -> KeyEvent.VK_CLOSE_BRACKET;
            case NativeKeyEvent.VC_DELETE -> KeyEvent.VK_DELETE;
            case NativeKeyEvent.VC_F2 -> KeyEvent.VK_F2;
            case NativeKeyEvent.VC_P -> KeyEvent.VK_P;
            default -> raw;
        };
    }

    private void updateWarningBeeps() {
        if (soundsAudible() && model.consumeWarningBeep()) {
            Toolkit.getDefaultToolkit().beep();
        }
        updateApproachBarMilestoneBeeps();
    }

    /**
     * Beeps at approach start, then when remaining time crosses ⅔ and ⅓ of the time-to-overlap that was left at
     * cycle start — equal thirds in real time. (Using {@link EncounterWheelModel#getTargetOverlapApproachProgress()}
     * milestones would bunch beeps 1–2 when the bar first appears with p already past ⅓: progress is normalized
     * against a fixed {@code TARGET_OVERLAP_WARN_NS}, not against “time left when we entered the warning”.)
     */
    private void updateApproachBarMilestoneBeeps() {
        final double p = model.getTargetOverlapApproachProgress();
        final boolean overlap = model.targetOverlapsUncertainty();
        final boolean barInactive =
                model.getCalibratedSlot() == null || model.getTargetSlots().isEmpty() || model.isCalibrating();

        if (barInactive || overlap) {
            approachBarBeepMask = 0;
            approachTimeEntryNs = -1L;
            lastApproachBarProgress = p;
            return;
        }

        final long t = model.getTargetOverlapApproachTimeRemainingNs();
        if (p <= 0 || t == Long.MAX_VALUE) {
            approachBarBeepMask = 0;
            approachTimeEntryNs = -1L;
            lastApproachBarProgress = p;
            return;
        }

        if (lastApproachBarProgress >= 0 && p < lastApproachBarProgress - 0.02) {
            approachBarBeepMask = 0;
            approachTimeEntryNs = -1L;
        }
        lastApproachBarProgress = p;

        if (approachTimeEntryNs < 0L) {
            approachTimeEntryNs = t;
        }
        final long t0 = approachTimeEntryNs;
        final long beep2WhenTNotAbove = (long) (t0 * (2.0 / 3.0));
        final long beep3WhenTNotAbove = (long) (t0 * (1.0 / 3.0));

        if (!soundsAudible()) {
            approachBarBeepMask |= 1 << 0;
            if (t <= beep2WhenTNotAbove) {
                approachBarBeepMask |= 1 << 1;
            }
            if (t <= beep3WhenTNotAbove) {
                approachBarBeepMask |= 1 << 2;
            }
            return;
        }
        final Toolkit tk = Toolkit.getDefaultToolkit();
        if ((approachBarBeepMask & (1 << 0)) == 0) {
            tk.beep();
            approachBarBeepMask |= 1 << 0;
        }
        if ((approachBarBeepMask & (1 << 1)) == 0 && t <= beep2WhenTNotAbove) {
            tk.beep();
            approachBarBeepMask |= 1 << 1;
        }
        if ((approachBarBeepMask & (1 << 2)) == 0 && t <= beep3WhenTNotAbove) {
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
}
