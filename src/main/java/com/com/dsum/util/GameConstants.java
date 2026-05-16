package com.com.dsum.util;

public final class GameConstants {

    // This is the frame rate of the GameBoy
    public static final double FRAME_RATE = 59.7275;

    public static final double ONE_FRAME_NS = 1_000_000_000 / FRAME_RATE;

    public static final int DSUM_RANGE = 256;

    // This is the average number of frames that a full DSum cycle takes in the overworld.
    public static final double OVERWORLD_CYCLE_FRAMES = 373.4160401;
    // This is the average number of frames that a full DSum cycle takes in-battle.
    public static final  double IN_BATTLE_CYCLE_FRAMES = 791.5555556;

    // These are how many frames from encounter generation, to the screen being full black,
    // for each animation type.
    public static final int HORIZONTAL_BLINDS_FRAMES = 70;
    public static final int VERTICAL_BLINDS_FRAMES = 64;
    public static final int SPLIT_SPIRAL_FRAMES = 110;
    public static final int FULL_SPIRAL_FRAMES = 142;

    // This is how many frames from encounter generation, that the overworld DSum
    // rate continues.  For the remainder of the frames above, DSum will advance
    // at the in-battle rate.
    public static final int BATTLE_ENTRY_OVERWORLD_FRAMES = 8;

    // These are how many frames, after clearing the last text box for each battle-end type,
    // that the DSum will advance at the in-battle rate.
    // After these frames, the overworld DSum rate will resume.
    public static final int GOT_AWAY_FRAMES = 37;
    public static final int ENEMY_RAN_FRAMES = 77;
    public static final int SENT_TO_BOX_FRAMES = 22;
    public static final int JOINED_PARTY_FRAMES = 37;
    public static final int NICKNAMED_PARTY_FRAMES = 30;
}
