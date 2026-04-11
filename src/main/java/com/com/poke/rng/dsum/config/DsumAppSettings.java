package com.com.poke.rng.dsum.config;

import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;

import java.util.List;

/**
 * Persisted user-facing encounter setup (game, route, targets, Yellow modifier, etc.).
 */
public record DsumAppSettings(
        Game game,
        Route route,
        List<EncounterSlot> targetSlots,
        int modifierUi,
        boolean pikaLead,
        int leadLevel,
        boolean showOuterRbCycleUncertaintyBand) {

    public static DsumAppSettings defaults() {
        return new DsumAppSettings(
                Game.RED,
                Route.SAFARI_ZONE_CENTER,
                List.of(EncounterSlot._9),
                0,
                true,
                70,
                false);
    }
}
