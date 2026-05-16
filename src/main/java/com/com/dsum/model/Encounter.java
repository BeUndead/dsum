package com.com.dsum.model;

public record Encounter(Species species, int level) {

    public static Encounter of(Species species, int level) {
        return new Encounter(species, level);
    }
}
