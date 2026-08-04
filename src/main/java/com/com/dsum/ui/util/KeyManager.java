package com.com.dsum.ui.util;

import com.com.dsum.model.EncounterExitStrategy;
import com.com.dsum.model.EncounterSlot;
import com.com.dsum.sim.DSumDriver;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public final class KeyManager {

    private static final String A_TOGGLE_PAUSE = "dsum.togglePause";
    private static final String A_BATTLE_TOGGLE = "dsum.battleToggle";
    private static final String A_STEP_BACK = "dsum.stepBack";
    private static final String A_STEP_FORWARD = "dsum.stepForward";
    private static final String A_RESET = "dsum.reset";
    private static final String A_EXIT_RAN = "dsum.exitRan";
    private static final String A_EXIT_BOX = "dsum.exitBox";
    private static final String A_EXIT_NICKNAME = "dsum.exitNickname";
    private static final String A_EXIT_TEAM = "dsum.exitTeam";

    private KeyManager() {
    }

    /**
     * Registers keyboard shortcuts on {@code component} while it has focus (same scope as the previous {@link java.awt.event.KeyListener}).
     */
    public static void install(final JComponent component, final DSumDriver driver) {
        final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), A_TOGGLE_PAUSE);
        actionMap.put(A_TOGGLE_PAUSE, new DriverAction(driver, DSumDriver::togglePause));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), A_BATTLE_TOGGLE);
        actionMap.put(A_BATTLE_TOGGLE, new DriverAction(driver, DSumDriver::toggleBattle));

        for (int vk = KeyEvent.VK_1; vk <= KeyEvent.VK_9; vk++) {
            final int ordinal = vk - KeyEvent.VK_1;
            final String id = "dsum.primeSlot." + ordinal;
            inputMap.put(KeyStroke.getKeyStroke(vk, 0), id);
            actionMap.put(id, new DriverAction(driver, d -> d.primeEncounterSlot(EncounterSlot.values()[ordinal])));
        }
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, 0), "dsum.primeSlot.9");
        actionMap.put("dsum.primeSlot.9", new DriverAction(driver, d -> d.primeEncounterSlot(EncounterSlot.values()[9])));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, 0), A_STEP_BACK);
        actionMap.put(A_STEP_BACK, new DriverAction(driver, d -> d.step(-1)));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, 0), A_STEP_FORWARD);
        actionMap.put(A_STEP_FORWARD, new DriverAction(driver, d -> d.step(1)));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), A_EXIT_RAN);
        actionMap.put(A_EXIT_RAN, new DriverAction(driver, d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_RAN)));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), A_EXIT_BOX);
        actionMap.put(A_EXIT_BOX, new DriverAction(driver, d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_SENT_TO_BOX)));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), A_EXIT_NICKNAME);
        actionMap.put(A_EXIT_NICKNAME,
                new DriverAction(driver, d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_NICKNAMED_JOINED_PARTY)));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), A_EXIT_TEAM);
        actionMap.put(A_EXIT_TEAM, new DriverAction(driver, d -> d.primeEncounterExitStrategy(EncounterExitStrategy.POKEMON_JOINED_PARTY)));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), A_RESET);
        actionMap.put(A_RESET, new DriverAction(driver, DSumDriver::reset));
    }

    @FunctionalInterface
    private interface DriverCommand {
        void run(DSumDriver driver);
    }

    private static final class DriverAction extends AbstractAction {
        private final DSumDriver driver;
        private final DriverCommand command;

        private DriverAction(final DSumDriver driver, final DriverCommand command) {
            this.driver = driver;
            this.command = command;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            this.command.run(this.driver);
        }
    }
}
