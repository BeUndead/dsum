package com.com.dsum.ui.util;

import com.com.dsum.model.EncounterExitStrategy;
import com.com.dsum.model.EncounterSlot;
import com.com.dsum.sim.DSumDriver;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mirrors the {@link KeyManager} bindings onto a native keyboard hook, so that the timer can be driven
 * while the emulator has focus rather than only when this window does.
 * <p>
 * A native hook is the only option here: the JVM is only ever handed key events aimed at its own
 * windows.  The hook only watches, it does not consume, so every key still reaches whatever
 * application is actually focused.
 */
public final class GlobalHotKeys {

    private final Map<Integer, Consumer<DSumDriver>> bindings = new HashMap<>();
    private final DSumDriver driver;
    private final Window window;

    private final NativeKeyListener listener = new NativeKeyListener() {
        @Override
        public void nativeKeyPressed(final NativeKeyEvent nativeEvent) {
            handle(nativeEvent);
        }
    };

    private volatile boolean enabled;

    public GlobalHotKeys(final DSumDriver driver, final Window window) {
        this.driver = driver;
        this.window = window;

        // The library is chatty on stderr otherwise, in the same spirit as the flatlaf native warning.
        final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        bindings.put(NativeKeyEvent.VC_P, DSumDriver::togglePause);
        bindings.put(NativeKeyEvent.VC_SPACE, DSumDriver::toggleBattle);
        bindings.put(NativeKeyEvent.VC_DELETE, DSumDriver::reset);
        bindings.put(NativeKeyEvent.VC_OPEN_BRACKET, d -> d.step(-1));
        bindings.put(NativeKeyEvent.VC_CLOSE_BRACKET, d -> d.step(1));
        bindings.put(NativeKeyEvent.VC_R, d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_RAN));
        bindings.put(NativeKeyEvent.VC_B, d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_SENT_TO_BOX));
        bindings.put(NativeKeyEvent.VC_N,
                d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_NICKNAMED_JOINED_PARTY));
        bindings.put(NativeKeyEvent.VC_T, d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_JOINED_PARTY));

        // Listed rather than derived, since the native codes are not required to be contiguous.
        final int[] slotKeys = {
                NativeKeyEvent.VC_1, NativeKeyEvent.VC_2, NativeKeyEvent.VC_3, NativeKeyEvent.VC_4,
                NativeKeyEvent.VC_5, NativeKeyEvent.VC_6, NativeKeyEvent.VC_7, NativeKeyEvent.VC_8,
                NativeKeyEvent.VC_9
        };
        for (int i = 0; i < slotKeys.length; i++) {
            final int ordinal = i;
            bindings.put(slotKeys[i], d -> d.primeEncounterSlot(EncounterSlot.values()[ordinal]));
        }
        bindings.put(NativeKeyEvent.VC_0, d -> d.primeEncounterSlot(EncounterSlot.values()[9]));
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Turns the hook on or off.
     *
     * @return null when it worked, otherwise a message explaining why it did not.
     */
    public String setEnabled(final boolean enable) {
        if (enable == this.enabled) {
            return null;
        }
        if (!enable) {
            GlobalScreen.removeNativeKeyListener(this.listener);
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (final NativeHookException unregisterEx) {
                // Nothing useful to do; the hook is going away with the process regardless.
            }
            this.enabled = false;
            return null;
        }

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this.listener);
            this.enabled = true;
            return null;
        } catch (final NativeHookException | UnsatisfiedLinkError hookEx) {
            this.enabled = false;
            return describeFailure(hookEx);
        }
    }

    private static String describeFailure(final Throwable hookEx) {
        final String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return """
                   Global keys could not be started.

                   macOS requires permission before any application may watch the keyboard:
                   System Settings > Privacy & Security > Accessibility, then enable the app
                   you launched this from (Terminal, or your IDE, if you started it there).
                   The app has to be restarted after granting it.

                   """ + hookEx.getMessage();
        }
        if (os.contains("linux")) {
            return """
                   Global keys could not be started.

                   On Linux this needs an X11 session with the XRecord extension; it does not
                   work under Wayland.

                   """ + hookEx.getMessage();
        }
        return "Global keys could not be started.\n\n" + hookEx.getMessage();
    }

    private void handle(final NativeKeyEvent nativeEvent) {
        final Consumer<DSumDriver> command = this.bindings.get(nativeEvent.getKeyCode());
        if (command == null) {
            return;
        }
        // Leave shortcuts in other applications alone; only the bare key is ours.
        final int chords = NativeInputEvent.CTRL_MASK | NativeInputEvent.ALT_MASK | NativeInputEvent.META_MASK;
        if ((nativeEvent.getModifiers() & chords) != 0) {
            return;
        }

        // Hop to the EDT, both because this arrives on the library's own thread and so that the work
        // runs in exactly the context the focused-window bindings already use.
        SwingUtilities.invokeLater(() -> {
            if (this.window.isActive()) {
                // Focused, so KeyManager is about to handle this key; acting here as well would
                // apply it twice.
                return;
            }
            command.accept(this.driver);
        });
    }
}
