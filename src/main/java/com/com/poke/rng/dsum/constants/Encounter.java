package com.com.poke.rng.dsum.constants;

public record Encounter(Species species, int level) {

    public static Encounter of(Species species, int level) {
        return new Encounter(species, level);
    }
}
