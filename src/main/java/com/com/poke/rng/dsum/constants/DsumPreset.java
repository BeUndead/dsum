package com.com.poke.rng.dsum.constants;

/** Bundled settings for quick setup from the Details ▸ Presets menu. */
public enum DsumPreset {
    RED_NIDORAN(
            "Red Nidoran",
            Game.RED,
            Route.ROUTE_22,
            6,
            false,
            EncounterSlot._4),
    YELLOW_NIDORAN(
            "Yellow Nidoran",
            Game.YELLOW,
            Route.ROUTE_2,
            6,
            true,
            EncounterSlot._7),
    BLUE_SAFARI_124(
            "124 Safari",
            Game.BLUE,
            Route.SAFARI_ZONE_CENTER,
            29,
            false,
            EncounterSlot._9);

    private final String menuLabel;
    private final Game game;
    private final Route route;
    private final int leadLevel;
    private final boolean pikaLead;
    private final EncounterSlot targetSlot;

    DsumPreset(
            final String menuLabel,
            final Game game,
            final Route route,
            final int leadLevel,
            final boolean pikaLead,
            final EncounterSlot targetSlot) {
        this.menuLabel = menuLabel;
        this.game = game;
        this.route = route;
        this.leadLevel = leadLevel;
        this.pikaLead = pikaLead;
        this.targetSlot = targetSlot;
    }

    public String menuLabel() {
        return menuLabel;
    }

    public Game game() {
        return game;
    }

    public Route route() {
        return route;
    }

    public int leadLevel() {
        return leadLevel;
    }

    public boolean pikaLead() {
        return pikaLead;
    }

    public EncounterSlot targetSlot() {
        return targetSlot;
    }
}
