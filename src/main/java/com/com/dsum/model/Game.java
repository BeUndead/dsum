package com.com.dsum.model;

import com.com.dsum.util.GameConstants;

public enum Game implements Named {
    RED("Red", GameConstants.OVERWORLD_CYCLE_FRAMES, GameConstants.IN_BATTLE_CYCLE_FRAMES),
    BLUE("Blue", GameConstants.OVERWORLD_CYCLE_FRAMES, GameConstants.IN_BATTLE_CYCLE_FRAMES),
    YELLOW("Yellow", -GameConstants.OVERWORLD_CYCLE_FRAMES, GameConstants.IN_BATTLE_CYCLE_FRAMES);

    private final String name;
    private final double overworldCycle;
    private final double inBattleCycle;

    Game(final String name, final double overworldCycle, final double inBattleCycle) {
        this.name = name;
        this.overworldCycle = overworldCycle;
        this.inBattleCycle = inBattleCycle;
    }


    @Override
    public String getName() {
        return this.name;
    }

    public double getOverworldCycle() {
        return this.overworldCycle;
    }

    public double getInBattleCycle() {
        return this.inBattleCycle;
    }
}
