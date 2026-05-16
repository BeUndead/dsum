package com.com.dsum.util;

import com.com.dsum.model.EncounterSlot;
import com.com.dsum.model.SelectionsConfig;

import java.awt.*;

public class Colours {

    private final Color UNCERTAINTY = new Color(255, 255, 255, 100);

    private final Color SUGGESTION = new Color(255, 149, 11, 144);
    private final Color TARGET = new Color(128, 220, 36, 144);

    private final Color STATE_CHIP = new Color(62, 160, 202, 95);
    private final Color STATE_CHIP_TEXT = new Color(25, 25, 25, 90);

    private final Color INSTRUCTION_CHIP = new Color(62, 160, 202, 190);
    private final Color INSTRUCTION_CHIP_TEXT = new Color(25, 25, 25, 150);

    private final Color SLOT_BAR_BACKGROUND = new Color(90, 90, 90);

    private final Color UNCALIBRATED = new Color(251, 227, 94, 128);

    private final Color[] slotColors = new Color[] {
            new Color(255, 130, 150),
            new Color(255, 180, 100),
            new Color(255, 220, 100),
            new Color(120, 220, 150),
            new Color(100, 210, 230),
            new Color(120, 170, 255),
            new Color(210, 130, 220),
            new Color(255, 150, 200),
            new Color(149, 149, 149),
            new Color(113, 125, 151)
    };

    private final SelectionsConfig config;

    public Colours(final SelectionsConfig config) {
        this.config = config;
    }

    public Color fillColor(final EncounterSlot slot) {
        return slotColors[slot.ordinal()];
    }

    public Color targetColor() {
        return TARGET;
    }

    public Color uncalibrated() {
        return UNCALIBRATED;
    }

    public Color background(final double prob) {
        final double threshold = config.getThreshold();
        if (prob < threshold) {
            return null;
        }
        return new Color(103, 207, 103, (int) (prob * 255));
    }

    public Color suggestionColor() {
        return SUGGESTION;
    }

    public Color uncertaintyColor() {
        return UNCERTAINTY;
    }

    public Color stateChip() {
        return STATE_CHIP;
    }

    public Color stateText() {
        return STATE_CHIP_TEXT;
    }

    public Color instructionChip() {
        return INSTRUCTION_CHIP;
    }

    public Color instructionText() {
        return INSTRUCTION_CHIP_TEXT;
    }

    public Color oddsBarBackground() {
        return SLOT_BAR_BACKGROUND;
    }
}
