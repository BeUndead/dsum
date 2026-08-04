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
    public static final double MIN_THRESHOLD = 0.0;
    public static final double MAX_THRESHOLD = 1.0;

    private static final Game DEFAULT_GAME = Game.RED;
    private static final Route DEFAULT_ROUTE = Route.SAFARI_ZONE_CENTER;
    private static final int DEFAULT_LEAD_LEVEL = 70;
    private static final double DEFAULT_THRESHOLD = 0.1;

    private final List<Consumer<Game>> onGameChange = new ArrayList<>();
    private final List<Consumer<Route>> onRouteChange = new ArrayList<>();
    private final List<Consumer<Set<EncounterSlot>>> onTargetsChange = new ArrayList<>();
    private final List<Consumer<Integer>> onLeadLevelChange = new ArrayList<>();
    private final Set<EncounterSlot> targets = new LinkedHashSet<>();
    // All four are restored from the last run.  The numeric ones are clamped on the way in, since a
    // value stored by an older version of the app is not necessarily one this version accepts, and
    // an out of range value here would be rejected by the spinner models that read it at startup.
    private volatile Game game = UserPreferences.getEnum(UserPreferences.GAME, DEFAULT_GAME);
    private volatile Route route = UserPreferences.getEnum(UserPreferences.ROUTE, DEFAULT_ROUTE);
    private volatile int leadLevel = clamp(UserPreferences.getInt(UserPreferences.LEAD_LEVEL, DEFAULT_LEAD_LEVEL),
            MIN_LEAD_LEVEL, MAX_LEAD_LEVEL);
    private volatile double threshold = clamp(UserPreferences.getDouble(UserPreferences.THRESHOLD, DEFAULT_THRESHOLD),
            MIN_THRESHOLD, MAX_THRESHOLD, DEFAULT_THRESHOLD);


    public SelectionsConfig() {
        this.targets.add(EncounterSlot._9);
    }

    public void setGame(final Game game) {
        this.game = game;
        UserPreferences.putEnum(UserPreferences.GAME, game);
        this.onGameChange.forEach(c -> c.accept(game));
    }

    public void setRoute(final Route route) {
        this.route = route;
        UserPreferences.putEnum(UserPreferences.ROUTE, route);
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
        UserPreferences.putInt(UserPreferences.LEAD_LEVEL, leadLevel);
        this.onLeadLevelChange.forEach(c -> c.accept(leadLevel));
    }

    public void setThreshold(final double threshold) {
        if (Double.isNaN(threshold) || threshold < MIN_THRESHOLD || threshold > MAX_THRESHOLD) {
            return;
        }
        this.threshold = threshold;
        UserPreferences.putDouble(UserPreferences.THRESHOLD, threshold);
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

    private static int clamp(final int value, final int min, final int max) {
        return Math.min(max, Math.max(min, value));
    }

    // NaN cannot be clamped into range, and it compares greater than every bound, so a stored NaN
    // would sail past a plain min/max check and then be rejected by SpinnerNumberModel at startup.
    private static double clamp(final double value, final double min, final double max, final double fallback) {
        if (Double.isNaN(value)) {
            return fallback;
        }
        return Math.min(max, Math.max(min, value));
    }
}
