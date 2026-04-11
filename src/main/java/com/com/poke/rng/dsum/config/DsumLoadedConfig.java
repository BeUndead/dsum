package com.com.poke.rng.dsum.config;

import java.util.List;

/** Result of loading {@code config.properties}: main app state plus optional user-defined presets. */
public record DsumLoadedConfig(DsumAppSettings appSettings, List<EncounterSetupPreset> userPresets) {

    public DsumLoadedConfig {
        userPresets = List.copyOf(userPresets);
    }
}
