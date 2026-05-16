package com.com.dsum.model;

import com.com.dsum.util.GameConstants;

public enum EncounterExitStrategy {
    PLAYER_GOT_AWAY(GameConstants.GOT_AWAY_FRAMES),
    POKEMON_RAN(GameConstants.ENEMY_RAN_FRAMES),
    POKEMON_JOINED_PARTY(GameConstants.JOINED_PARTY_FRAMES),
    POKEMON_NICKNAMED_JOINED_PARTY(GameConstants.NICKNAMED_PARTY_FRAMES),
    POKEMON_SENT_TO_BOX(GameConstants.SENT_TO_BOX_FRAMES);

    private final int inBattleFrames;

    EncounterExitStrategy(final int frames) {
        inBattleFrames = frames;
    }

    public int getInBattleFrames() {
        return this.inBattleFrames;
    }
}
