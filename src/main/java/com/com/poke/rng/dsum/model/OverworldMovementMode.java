package com.com.poke.rng.dsum.model;

/**
 * Overworld movement style: canonical Game Boy frames after a step before that step fully “resolves” on the DSum
 * counter (corner bonk / bike / walk). Exposed for UI tooltips only — it does not shift {@link
 * com.com.poke.rng.dsum.model.EncounterWheelModel} suggested-slot ranges; those follow battle-start DSum ± wedge.
 */
public enum OverworldMovementMode {

    /** Corner bonking; no extra step lag. */
    CORNER_BONK(0),
    /** Riding the bicycle. */
    BIKE(9),
    /** Walking. */
    WALKING(17);

    private final int referenceStepLagFrames;

    OverworldMovementMode(final int referenceStepLagFrames) {
        this.referenceStepLagFrames = referenceStepLagFrames;
    }

    /**
     * Frames of step lag (reference for when to press relative to movement). Does not affect suggested-slot math.
     */
    public int suggestionStepLagFrames() {
        return referenceStepLagFrames;
    }
}
