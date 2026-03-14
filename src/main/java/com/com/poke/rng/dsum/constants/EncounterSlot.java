package com.com.poke.rng.dsum.constants;

public enum EncounterSlot {
    _1(0, 50),
    _2(51, 101),
    _3(102, 140),
    _4(141, 165),
    _5(166, 190),
    _6(191, 215),
    _7(216, 228),
    _8(229, 241),
    _9(242, 252),
    _10(253, 255);

    private final int lowerBound;
    private final int upperBound;

    EncounterSlot(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public static EncounterSlot getSlot(final int value) {
        if (value < 0) {
            throw new IllegalArgumentException("'value' must be between 0 and 255.");
        }

        for (final EncounterSlot slot : EncounterSlot.values()) {
            if (value >= slot.min() && value <= slot.max()) {
                return slot;
            }
        }
        return null;
    }

    public int min() {
        return lowerBound;
    }

    public int max() {
        return upperBound;
    }
}
