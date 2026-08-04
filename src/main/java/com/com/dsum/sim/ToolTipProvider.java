package com.com.dsum.sim;

import com.com.dsum.model.EncounterExitStrategy;
import com.com.dsum.model.EncounterSlot;
import com.com.dsum.model.SelectionsConfig;

public final class ToolTipProvider {

    private final DSumDriver driver;
    private final SelectionsConfig config;

    public ToolTipProvider(final DSumDriver driver, final SelectionsConfig config) {
        this.driver = driver;
        this.config = config;
    }


    public String getStateText() {
        if (!driver.isInBattle()) {
            return "Overworld";
        } else {
            return "Battle";
        }
    }

    public String getStateSubText() {
        if (!driver.isInBattle()) {
            if (driver.isFirstCalibration()) {
                return "[Uncalibrated]";
            } else {
                final double cumulativeProb = driver.getTargetCumulativeProbability();
                if (cumulativeProb >= config.getThreshold()) {
                    return "[Search]";
                } else {
                    return "[Wait]";
                }
            }
        }

        final EncounterExitStrategy localExitStrategy = driver.getExitStrategy();
        final String exitText = switch (localExitStrategy) {
            case PLAYER_GOT_AWAY -> "Player Ran";
            case POKEMON_RAN -> "[R] Pokemon Ran";
            case POKEMON_JOINED_PARTY -> "[T] Joined Party";
            case POKEMON_SENT_TO_BOX -> "[B] Sent to Box";
            case POKEMON_NICKNAMED_JOINED_PARTY -> "[N] Nicknamed";
        };

        // The slot is what [SPACE] is waiting on, so show whether one has been entered yet.
        final EncounterSlot primedSlot = driver.getPrimedSlot();
        final String slotText = primedSlot == null ? "Slot ?" : "Slot " + (primedSlot.ordinal() + 1);
        return slotText + " | " + exitText;
    }

    public String getInstructionText() {
        if (!driver.isInBattle()) {
            if (driver.isFirstCalibration()) {
                return """
                       Get an encounter, and then press
                       [SPACE] when the screen turns black
                       """;
            }
            return null;
        }
        if (driver.isFirstCalibration()) {
            return """
                   Press the number for the slot you got (0 = Slot 10),
                   then [SPACE] when clearing the last text box.

                   Press the button corresponding to how the battle ends first.
                   <default> = Player Ran from Pokémon
                   [R] = Pokémon Ran from Player
                   [T] = Pokémon caught, and joined party
                   [N] = Pokémon caught, nicknamed, and joined party
                   [B] = Pokémon caught, and sent to box
                   """;
        }
        if (driver.getPrimedSlot() == null) {
            // [SPACE] deliberately does nothing until a slot has been named, so say so rather than
            // leaving the key looking broken.
            return """
                   Press the number for the slot you got (0 = Slot 10),
                   then [SPACE] when clearing the last text box.
                   """;
        }
        return null;
    }
}
