package com.com.poke.rng.dsum.model;

/**
 * Overworld movement style: canonical Game Boy frames after a step before that step fully “resolves” on the DSum
 * counter (corner bonk / bike / walk). {@link #suggestionStepLagFrames()} rotates the encounter <em>wheel artwork</em>
 * by that much (opposite active overworld rotation) so the ring matches post-step alignment; overlap / beeps / amber
 * suggestions follow the live counter without that offset.
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

    /** Frames of step lag — drives visual wheel rotation in {@link com.com.poke.rng.dsum.model.EncounterWheelModel#getDisplayAngleDeg()}. */
    public int suggestionStepLagFrames() {
        return referenceStepLagFrames;
    }
}
