package com.com.poke.rng.dsum.config;

import com.com.poke.rng.dsum.constants.DsumPreset;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;

import java.util.List;

/**
 * A named encounter setup (game, route, targets, etc.) used by the Presets menu — either bundled
 * {@link DsumPreset} or loaded from {@code preset.*} keys in the config file.
 */
public record EncounterSetupPreset(
        String menuLabel,
        Game game,
        Route route,
        int leadLevel,
        boolean pikaLead,
        int modifierUi,
        List<EncounterSlot> targetSlots) {

    public EncounterSetupPreset {
        targetSlots = List.copyOf(targetSlots);
    }

    public static EncounterSetupPreset fromBundled(final DsumPreset p) {
        return new EncounterSetupPreset(
                p.menuLabel(),
                p.game(),
                p.route(),
                p.leadLevel(),
                p.pikaLead(),
                0,
                List.of(p.targetSlot()));
    }
}
