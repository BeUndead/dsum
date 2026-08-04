package com.com.dsum.model;

import com.com.dsum.util.UserPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class SelectionsConfig {

    public static final int MIN_LEAD_LEVEL = 1;
    public static final int MAX_LEAD_LEVEL = 255;

    private static final int DEFAULT_LEAD_LEVEL = 70;

    private final List<Consumer<Game>> onGameChange = new ArrayList<>();
    private final List<Consumer<Route>> onRouteChange = new ArrayList<>();
    private final List<Consumer<Set<EncounterSlot>>> onTargetsChange = new ArrayList<>();
    private final List<Consumer<Integer>> onLeadLevelChange = new ArrayList<>();
    private final Set<EncounterSlot> targets = new LinkedHashSet<>();
    private volatile Game game = Game.RED;
    private volatile Route route = Route.SAFARI_ZONE_CENTER;
    // Restored from the last run, clamped in case the stored value is stale or hand-edited.
    private volatile int leadLevel = clampLeadLevel(UserPreferences.getLeadLevel(DEFAULT_LEAD_LEVEL));
    private volatile double threshold = 0.1;


    public SelectionsConfig() {
        this.targets.add(EncounterSlot._9);
    }

    public void setGame(final Game game) {
        final Game oldGame = this.game;
        this.game = game;
        this.onGameChange.forEach(c -> c.accept(game));
    }

    public void setRoute(final Route route) {
        this.route = route;
        this.onRouteChange.forEach(c -> c.accept(route));
    }

    public void setTargets(final Set<EncounterSlot> targets) {
        this.targets.clear();
        this.targets.addAll(targets);
        this.onTargetsChange.forEach(c -> c.accept(targets));
    }

    public void setLeadLevel(final int leadLevel) {
        if (leadLevel < MIN_LEAD_LEVEL || leadLevel > MAX_LEAD_LEVEL) {
            return;
        }
        this.leadLevel = leadLevel;
        UserPreferences.setLeadLevel(leadLevel);
        this.onLeadLevelChange.forEach(c -> c.accept(leadLevel));
    }

    public void setThreshold(final double threshold) {
        if (threshold < 0 || threshold > 1) {
            return;
        }
        this.threshold = threshold;
    }


    public void registerGameChangeListener(final Consumer<Game> consumer) {
        this.onGameChange.add(consumer);
    }

    public void registerRouteChangeListener(final Consumer<Route> consumer) {
        this.onRouteChange.add(consumer);
    }

    public void registerTargetsChangeListener(final Consumer<Set<EncounterSlot>> consumer) {
        this.onTargetsChange.add(consumer);
    }

    public void registerLeadLevelChangeListener(final Consumer<Integer> consumer) {
        this.onLeadLevelChange.add(consumer);
    }

    public Route getRoute() {
        return this.route;
    }

    public Set<EncounterSlot> getTargets() {
        return targets;
    }

    public Game getGame() {
        return game;
    }

    public double getThreshold() {
        return threshold;
    }

    public int getLeadLevel() {
        return leadLevel;
    }

    private static int clampLeadLevel(final int leadLevel) {
        return Math.min(MAX_LEAD_LEVEL, Math.max(MIN_LEAD_LEVEL, leadLevel));
    }
}
