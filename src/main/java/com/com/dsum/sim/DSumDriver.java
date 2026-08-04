package com.com.dsum.sim;

import com.com.dsum.model.*;
import com.com.dsum.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class DSumDriver {

    private static final Object[] monitor = new Object[0];

    private final SelectionsConfig config;
    private final DSumSlotComputer slotComputer;
    private final DSumTracker tracker;

    private final List<Runnable> updateQueue = new LinkedList<>();
    private final List<Consumer<Integer>> uncertaintyChangeQueue = new LinkedList<>();
    private final List<Consumer<Map<EncounterSlot, Integer>>> suggestionQueue = new LinkedList<>();

    private volatile boolean firstCalibration = true;

    private volatile int uncertainty = 1;
    private volatile Integer dsumAtBattleStart = null;
    private volatile Map<Integer, Integer> dsumCalibrationRange = null;
    private volatile Map<Integer, Integer> dsumRangeAtBattleStart = null;
    private volatile Double targetProbabilities = null;

    private volatile double[] battleEntrySlotProbabilities = null;

    /**
     * Cached uniform marginal over the uncertainty window (out of battle only).
     */
    private int slotProbCacheCenter = Integer.MIN_VALUE;
    private Map<Integer, Integer> slotProbCacheCalibrationRef = null;
    private int slotProbCacheU = -1;
    private double[] slotProbCache = new double[EncounterSlot.values().length];

    private volatile EncounterExitStrategy exitStrategy = EncounterExitStrategy.PLAYER_GOT_AWAY;
    // The slot the player says they encountered.  Primed by the number keys at any point during the
    // battle, and consumed when they press [SPACE] again to say the battle is over.
    private volatile EncounterSlot primedSlot = null;
    private volatile Long lastNow;
    private volatile boolean paused = true;

    public DSumDriver(final DSumTracker tracker, final DSumSlotComputer slotComputer, final SelectionsConfig config) {
        this.tracker = tracker;
        this.config = config;
        this.slotComputer = slotComputer;
        new Timer(16, this::runUpdate).start();
    }

    private static double[] normalizeSuggestionWeightsToSlotProbabilities(final Map<EncounterSlot, Integer> weights) {
        long sum = 0L;
        for (final Integer v : weights.values()) {
            if (v != null) {
                sum += v;
            }
        }
        if (sum <= 0L) {
            return null;
        }
        final double[] p = new double[EncounterSlot.values().length];
        for (final EncounterSlot slot : EncounterSlot.values()) {
            final Integer w = weights.get(slot);
            p[slot.ordinal()] = w == null ? 0.0 : w / (double) sum;
        }
        return p;
    }

    public void registerUpdateListener(final Runnable runnable) {
        synchronized (monitor) {
            this.updateQueue.add(runnable);
        }
    }

    public void registerUncertaintyChangeListener(final Consumer<Integer> consumer) {
        synchronized (monitor) {
            this.uncertaintyChangeQueue.add(consumer);
        }
    }

    public void registerSuggestionListener(final Consumer<Map<EncounterSlot, Integer>> consumer) {
        synchronized (monitor) {
            this.suggestionQueue.add(consumer);
        }
    }

    public void resume() {
        this.paused = false;
    }

    public void togglePause() {
        this.paused = !this.paused;
    }

    public double[] getCurrentEncounterSlotProbabilities() {
        synchronized (monitor) {
            if (isInBattle() && battleEntrySlotProbabilities != null) {
                return Arrays.copyOf(battleEntrySlotProbabilities, battleEntrySlotProbabilities.length);
            }
            final int modCenter = MathUtilities.mod((int) Math.round(this.tracker.getDSum()));
            final int uKey = Math.max(1, uncertainty);
            if (modCenter != slotProbCacheCenter || uKey != slotProbCacheU
                    || dsumCalibrationRange != slotProbCacheCalibrationRef) {

                final Map<Integer, Integer> guess = hypothesisMapForBattleEntry(new BattleEntry(modCenter, modCenter));
                slotProbCache = marginalSlotProbOverUncertaintyWindow(modCenter, uncertainty, null);
                double[] probs = normalizeSuggestionWeightsToSlotProbabilities(
                        DSumUtilities.getSuggestionRange(guess, config.getRoute()));
                if (probs == null) {
                    probs = Arrays.copyOf(slotComputer.getSlotProbability(modCenter), EncounterSlot.values().length);
                }
                slotProbCache = probs;
                slotProbCacheCenter = modCenter;
                slotProbCacheCalibrationRef = dsumCalibrationRange;
                slotProbCacheU = uKey;
            }
            return Arrays.copyOf(slotProbCache, slotProbCache.length);
        }
    }

    public double getTargetCumulativeProbability() {
        synchronized (monitor) {
            if (targetProbabilities != null) {
                return targetProbabilities;
            }
            final double[] slotP = getCurrentEncounterSlotProbabilities();
            double totalProbs = 0.0d;
            for (final EncounterSlot target : config.getTargets()) {
                totalProbs += slotP[target.ordinal()];
            }
            targetProbabilities = totalProbs;
            return targetProbabilities;
        }
    }

    public boolean isFirstCalibration() {
        return firstCalibration;
    }

    public EncounterExitStrategy getExitStrategy() {
        return exitStrategy;
    }

    public EncounterSlot getPrimedSlot() {
        return primedSlot;
    }


    private Map<Integer, Integer> hypothesisMapForBattleEntry(final BattleEntry battleEntry) {
        final int atGeneration = battleEntry.atGeneration();
        if (dsumCalibrationRange == null) {
            return Map.of(atGeneration, 1);
        }
        final double calibrationMean = MathUtilities.circularMean(dsumCalibrationRange);
        final double dsumOffset = atGeneration - calibrationMean;
        return RotationUtilities.applyOffsetToKeys(dsumCalibrationRange, dsumOffset);
    }

    private double[] marginalSlotProbOverUncertaintyWindow(
            final int dsumCentre,
            final int uncertainty,
            final Map<Integer, Integer> calibrationOrNull) {

        final int u = Math.max(1, uncertainty);
        final int span = 2 * u + 1;
        final Map<Integer, Integer> calibration = calibrationOrNull == null ? Map.of() : calibrationOrNull;
        final boolean useUniform = calibration.isEmpty();

        long sumW = 0L;
        final int[] ds = new int[span];
        final int[] ws = new int[span];
        for (int i = 0; i < span; i++) {
            final int d = MathUtilities.mod(dsumCentre - u + i);
            ds[i] = d;
            int w;
            if (useUniform) {
                w = 1;
            } else {
                final Integer cw = calibration.get(d);
                w = cw == null || cw <= 0 ? 0 : cw;
            }
            ws[i] = w;
            sumW += w;
        }
        if (sumW <= 0L) {
            Arrays.fill(ws, 1);
            sumW = span;
        }

        final double[] out = new double[EncounterSlot.values().length];
        for (int i = 0; i < span; i++) {
            final double wd = ws[i] / (double) sumW;
            final double[] cond = slotComputer.getSlotProbability(ds[i]);
            for (final EncounterSlot slot : EncounterSlot.values()) {
                out[slot.ordinal()] += wd * cond[slot.ordinal()];
            }
        }
        return out;
    }

    public void step(final int signum) {
        if (!this.paused) {
            return;
        }
        synchronized (monitor) {
            final int dsum = (int) Math.round(this.tracker.getDSum());
            this.tracker.setDSum(dsum + (signum > 0 ? -1.0d : 1.0d));
            markUpdate();
        }
    }

    public void primeEncounterExitStrategy(final EncounterExitStrategy strategy) {
        synchronized (monitor) {
            if (strategy == null) {
                return;
            }
            if (this.exitStrategy == strategy) {
                this.exitStrategy = EncounterExitStrategy.PLAYER_GOT_AWAY;
            } else {
                this.exitStrategy = strategy;
            }
        }
    }

    public void reset() {
        synchronized (monitor) {
            this.firstCalibration = true;
            this.dsumAtBattleStart = null;
            this.dsumRangeAtBattleStart = null;
            this.dsumCalibrationRange = null;
            this.uncertainty = 1;
            this.dsumRangeAtBattleStart = null;
            this.battleEntrySlotProbabilities = null;
            slotProbCacheCenter = Integer.MIN_VALUE;
            this.exitStrategy = EncounterExitStrategy.PLAYER_GOT_AWAY;
            this.primedSlot = null;
            markUpdate();
            markUpdateUncertainty();
            markUpdateSuggestions(null);
        }
    }

    /**
     * [SPACE] marks both ends of an encounter: the wipe finishing on the way in, and the last text box
     * being cleared on the way out.  Both are the timing critical moments, so both read the DSum at the
     * instant the key is pressed; which slot was encountered and how the battle ended are primed
     * separately, at whatever point during the battle the player gets around to entering them.
     */
    public void toggleBattle() {
        synchronized (monitor) {
            if (isInBattle()) {
                battleExited();
            } else {
                battleEntered();
            }
        }
    }

    private void battleEntered() {
        synchronized (monitor) {
            final Game game = this.config.getGame();
            final Route route = this.config.getRoute();

            final int dsum = MathUtilities.mod((int) Math.round(this.tracker.getDSum()));
            final BattleEntry battleEntry = RotationUtilities.onBattleEntry(game, route, dsum);

            tracker.setDSum(battleEntry.atNow());
            dsumAtBattleStart = battleEntry.atGeneration();

            if (dsumCalibrationRange == null) {
                dsumRangeAtBattleStart = Map.of(dsumAtBattleStart, 1);
            } else {
                final double calibrationMean = MathUtilities.circularMean(dsumCalibrationRange);
                // Anchor on encounter generation (rewound for entry animation), same idea as the uncalibrated branch;
                // tracker is already at atNow() — slot suggestions should reflect hypotheses at generation, not after.
                final double dsumOffset = dsumAtBattleStart - calibrationMean;
                dsumRangeAtBattleStart = RotationUtilities.applyOffsetToKeys(dsumCalibrationRange, dsumOffset);
            }

            final Map<EncounterSlot, Integer> encounterSuggestions =
                    DSumUtilities.getSuggestionRange(dsumRangeAtBattleStart, route);
            this.battleEntrySlotProbabilities = normalizeSuggestionWeightsToSlotProbabilities(encounterSuggestions);
            markUpdateSuggestions(encounterSuggestions);
            markUpdate();
        }
    }

    /**
     * Records which slot the player encountered, without ending the battle.  This mirrors
     * {@link #primeEncounterExitStrategy}: press at any point during the battle, press the same number
     * again to clear it, and it is only acted on when [SPACE] ends the battle.
     */
    public void primeEncounterSlot(final EncounterSlot slot) {
        synchronized (monitor) {
            if (slot == null || !isInBattle()) {
                // Only meaningful while an encounter is in progress.
                return;
            }
            this.primedSlot = this.primedSlot == slot ? null : slot;
            markUpdate();
        }
    }

    private void battleExited() {
        synchronized (monitor) {
            final EncounterSlot slot = this.primedSlot;
            if (slot == null) {
                // No slot entered yet, so there is nothing to calibrate against.  Stay in the battle
                // rather than dropping the encounter, which is what happened before this was split out:
                // naming the slot was the only way out of a battle, short of [DELETE].
                return;
            }

            final Game game = this.config.getGame();
            final Route route = this.config.getRoute();
            // Read before the reset further down, which puts this back to the default for the next battle.
            final EncounterExitStrategy exit = this.exitStrategy;

            final double dsumAtBattleStart = this.dsumAtBattleStart;
            final double dsum = tracker.getDSum();
            final double diffFromBattle = dsum - dsumAtBattleStart;

            final BattleExit battleExit = RotationUtilities.onBattleExit(
                    game, route, this.config.getLeadLevel(), slot, exit, dsum);

            final Map<Integer, Integer> slotCalibration = battleExit.suggestions();
            final Map<Integer, Integer> updatedCalibrationRange;
            if (!firstCalibration && dsumRangeAtBattleStart != null) {
                final Map<Integer, Integer> correctedRange = RotationUtilities.applyOffsetToKeys(dsumRangeAtBattleStart, battleExit.entryDelta());
                updatedCalibrationRange = DSumUtilities.overlapOrUseNewRange(correctedRange, slotCalibration);
            } else {
                updatedCalibrationRange = slotCalibration;
            }
            dsumCalibrationRange = updatedCalibrationRange;
            tracker.setDSum(MathUtilities.mod(battleExit.newDSum() + diffFromBattle));

            this.uncertainty = calculateUncertainty(updatedCalibrationRange);
            markUpdateUncertainty();

            this.dsumAtBattleStart = null;
            this.battleEntrySlotProbabilities = null;
            slotProbCacheCenter = Integer.MIN_VALUE;
            this.exitStrategy = EncounterExitStrategy.PLAYER_GOT_AWAY;
            this.primedSlot = null;
            clearFoundTarget(slot);
            markUpdate();
            markUpdateSuggestions(null);

            firstCalibration = false;
        }
    }


    /**
     * Drops a slot from the targets once it has actually been encountered, so that hunting a list of
     * slots does not need the mouse to tick them off one by one.  Off by default.
     */
    private void clearFoundTarget(final EncounterSlot found) {
        if (!config.isClearFoundTarget()) {
            return;
        }
        // getTargets() hands back the live set, so copy before editing it.
        final Set<EncounterSlot> remaining = new LinkedHashSet<>(config.getTargets());
        if (remaining.remove(found)) {
            config.setTargets(remaining);
        }
    }

    private void runUpdate(final ActionEvent ignored) {
        synchronized (monitor) {
            final long now = System.nanoTime();
            targetProbabilities = null;
            if (lastNow == null) {
                // First one will give a delta of 0, but future ones will provide the correct
                // ns for execution.
                lastNow = now;
            }

            if (!paused) {
                final Game game = this.config.getGame();
                final long timeDelta = now - lastNow;

                final double delta = isInBattle()
                        ? DSumUtilities.inBattleDelta(timeDelta) : DSumUtilities.overworldDelta(timeDelta, game);
                this.tracker.advance(delta);
                markUpdate();
            }

            lastNow = now;
        }
    }

    private void markUpdate() {
        for (final Runnable runnable : updateQueue) {
            runnable.run();
        }
    }

    private void markUpdateUncertainty() {
        for (final Consumer<Integer> consumer : uncertaintyChangeQueue) {
            consumer.accept(uncertainty);
        }
    }

    private void markUpdateSuggestions(final Map<EncounterSlot, Integer> encounterSuggestions) {
        for (final Consumer<Map<EncounterSlot, Integer>> suggestionConsumer : suggestionQueue) {
            suggestionConsumer.accept(encounterSuggestions);
        }
    }

    public boolean isUncalibrated() {
        return !isInBattle() && firstCalibration;
    }

    public boolean isInBattle() {
        return dsumAtBattleStart != null;
    }

    private int calculateUncertainty(final Map<Integer, Integer> dsumRange) {
        if (dsumRange == null || dsumRange.isEmpty()) {
            return 1;
        }

        final double mean = MathUtilities.circularMean(dsumRange);

        final double maxDifference = dsumRange.keySet().stream()
                .mapToDouble(key -> DSumUtilities.circularMinDiff(mean, key))
                .max()
                .orElse(1.0);

        return Math.max(1, (int) Math.ceil(maxDifference));
    }
}
