package com.com.poke.rng.dsum.model;

/**
 * Overworld movement style for suggested-slot timing only: Game Boy frames before a step “resolves” on the DSum
 * wheel (corner bonk / bike / walk).
 */
public enum OverworldMovementMode {

    /** Corner bonking; no extra step lag. */
    CORNER_BONK(0),
    /** Riding the bicycle. */
    BIKE(9),
    /** Walking. */
    WALKING(17);

    private final int suggestionStepLagFrames;

    OverworldMovementMode(final int suggestionStepLagFrames) {
        this.suggestionStepLagFrames = suggestionStepLagFrames;
    }

    /** Frames used in {@link EncounterWheelModel#getDsumRangeForSuggestedSlots()} step-lag term. */
    public int suggestionStepLagFrames() {
        return suggestionStepLagFrames;
    }
}
