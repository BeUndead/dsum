package com.com.poke.rng.dsum.model;

import com.com.poke.rng.dsum.constants.Encounter;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.util.Triplet;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Central DSum / wheel state. Mutable instance fields are {@code volatile} so UI paint (and any thread that reads the
 * model) always sees writes from {@link #update(long)} without stale cached values.
 */
public class EncounterWheelModel {

    // GameBoy's native frame rate.
    private static final double FRAME_RATE = 59.7275;

    // Red/Blue:
    // Average number of frames for one DSum cycle out of battle (counting down).
    private static final double OVERWORLD_DSUM_CYCLE_FRAMES = 375.221374;
    /**
     * Empirical envelope for Red/Blue overworld DSum cycle length (frames). The model advances the needle using
     * {@link #OVERWORLD_DSUM_CYCLE_FRAMES}; wedge width on calibrated overworld adds spread from the min/max rates.
     */
    private static final double RB_OVERWORLD_CYCLE_LENGTH_MIN_FRAMES = 367.0;
    private static final double RB_OVERWORLD_CYCLE_LENGTH_MAX_FRAMES = 415.0;
    // Average number of frames for one DSum cycle in battle (counting up).
    private static final double IN_BATTLE_DSUM_CYCLE_FRAMES = 775.1083333;

    // Number of frames which the in-battle DSum cycle runs before the spiral battle entry animation ends.
    private static final long COUNT_UP_BEFORE_SPIRAL_END_FRAMES = 100;
    // Number of frames which the in-battle DSum cycle runs before the full spiral battle entry animation ends.
    private static final long COUNT_UP_BEFORE_FULL_SPIRAL_END_FRAMES = 117;
    // Number of frames which the in-battle DSum cycle runs before the spiral battle entry animation ends.
    private static final long COUNT_UP_BEFORE_YELLOW_SPIRAL_END_FRAMES = 135;
    // Number of frames which the in-battle DSum cycle runs before the full spiral battle entry animation ends.
    private static final long COUNT_UP_BEFORE_YELLOW_FULL_SPIRAL_END_FRAMES = 152;

    // Number of frames which the in-battle DSum cycle runs before the blinds battle entry animation ends.
    private static final long COUNT_UP_BEFORE_BLINDS_END_FRAMES = 48;
    // Number of frames which the in-battle DSum cycle runs before the blinds battle entry animation ends.
    private static final long COUNT_UP_BEFORE_VERTICAL_BLINDS_END_FRAMES = 43;
    // Number of frames which the in-battle DSum cycle runs after clearing 'Got away safely!'.
    private static final long COUNT_UP_AFTER_GOT_AWAY_FRAMES = 37;

    /**
     * After an encounter triggers, DSum keeps advancing at overworld rate for about this many frames before
     * in-battle stepping fully applies (down in Red/Blue, up in Yellow — same signed period as {@link #update(long)}).
     * Used in {@link #computeBattleEntryGeometry} for Space / encounter triplet math, not for the painted wheel angle.
     */
    private static final long ENCOUNTER_OVERWORLD_CONTINUATION_FRAMES = 28;

    // Yellow:
    // Yellow's DSum is...  Interesting.
    // It can range anywhere from ~670 - 850 frames.  But once the count has been determined on a route, it
    // won't change.  To account for this, we have a base rate of ~800, and give the ability to increase or lower the
    // cycle duration.  Sorry
    private static final double YELLOW_OVERWORLD_DSUM_CYCLE_FRAMES_BASE = -800.8647059;

    private volatile double overworldDsumCycleModifier = 0.0;
    private volatile double overworldDsumCycleModifierNs = 0.0;

    // Average number of frames for one DSum cycle in battle (counting up).
    private static final double YELLOW_IN_BATTLE_DSUM_CYCLE_FRAMES = 781.6914894;

    // No count frames in yellow, since times are similar and go the same direction...

    // The duration (in ns) of a single frame.
    private static final double ONE_FRAME_NS = 1_000_000_000.0 / FRAME_RATE;

    private static final double OVERWORLD_CYCLE_NS = ONE_FRAME_NS * OVERWORLD_DSUM_CYCLE_FRAMES;
    private static final double YELLOW_OVERWORLD_CYCLE_NS = ONE_FRAME_NS * YELLOW_OVERWORLD_DSUM_CYCLE_FRAMES_BASE;
    private static final double IN_BATTLE_CYCLE_NS = ONE_FRAME_NS * IN_BATTLE_DSUM_CYCLE_FRAMES;
    private static final double YELLOW_IN_BATTLE_CYCLE_NS = ONE_FRAME_NS * YELLOW_IN_BATTLE_DSUM_CYCLE_FRAMES;

    private static final long PIKACHU_CRY_FRAMES = 36;

    public static final int DSUM_RANGE = 256;
    public static final double OFFSET_STEP_DEG = 3.0;

    /**
     * Magnitude (°) added to the angle offset for suggested-slot band on calibrated overworld — positive for R/B
     * (wheel sense), negated for Yellow where DSum cycles the other way.
     */
    private static final double SUGGESTED_SLOTS_ANGLE_OFFSET_DEG = 0.0;

    /**
     * Wedge widens by this many degrees on each side for each full in-battle wheel rotation (360° at in-battle rate);
     * total added width is twice this (e.g. 1.5 rotations → 4.5°/side → 9° total).
     */
    private static final double UNCERTAINTY_WEDGE_DEGREES_DELTA_PER_ROTATION = 2.0;

    /** Over this lead time (seconds), the approach bar ramps from empty to full. */
    private static final double TARGET_OVERLAP_WARN_NS = 2e9;

    private volatile List<EncounterSlot> targetSlots;

    private volatile Runnable onTargetSlotsChanged;

    private volatile Triplet<EncounterSlot, EncounterSlot, EncounterSlot> suggestedSlots;

    private volatile boolean isBlinds = false;

    /** Route for encounter metadata (e.g. wild level when calibrating alt animation). */
    private volatile Route route;

    /** Party lead level; used with route to infer alternate battle animation when calibrating. */
    private volatile int leadLevel = 70;

    /**
     * Count-up frames assumed at {@link #battleStart(boolean)} ({@code #COUNT_UP_BEFORE_*}) — used to retro-correct
     * calibration when actual animation length differed (see wild vs. lead level).
     */
    private volatile long assumedBattleAnimationFrames = COUNT_UP_BEFORE_SPIRAL_END_FRAMES;

    private volatile long overworldStartTime = System.nanoTime();
    private volatile long lastNow = overworldStartTime;
    /**
     * Red/Blue only: overworld time since {@link #calibrateSlot} (or last clear) accumulated in {@link #update} —
     * drives cycle-length uncertainty on the wedge while the needle stays on the nominal integral.
     */
    private volatile long rbOverworldElapsedForCycleUncertaintyNs;
    private volatile double angleDeg = 0.0;
    private volatile double manualAngleOffsetDeltaDeg;
    /** When true, {@link #update(long)} does not advance time-based rotation (P toggles). Manual [ ] nudges still apply. */
    private volatile boolean simulationPaused;
    /**
     * Set when {@link #calibrateSlot} completes successfully; cleared by {@link #clearCalibrationState}. Drives
     * first-time “Calibrating” + hint chip vs later “Encounter” runs without the hint.
     */
    private volatile boolean hasCalibratedAtLeastOnce;

    private volatile long battleEnterTime = -1;
    private volatile Triplet<Integer, Integer, Integer> rangeAtBattleStart = null;
    /**
     * Linear-span inner / full uncertainty triplets at the instant {@link #battleStart(boolean)} fixed {@link #angleDeg}
     * — used for suggested-slot overlap and clipping while {@link #isCalibrating()} so highlights do not drift with the
     * live needle (the wedge is not painted during calibration).
     */
    private volatile Triplet<Integer, Integer, Integer> innerSuggestedRangeAtBattleStart = null;
    private volatile Triplet<Integer, Integer, Integer> fullSuggestedRangeAtBattleStart = null;

    private volatile EncounterSlot calibratedSlot;
    /** Manual wedge tweak via hotkeys ({@link #uncertaintyDelta}). */
    private volatile double uncertaintyWedgeExtentDeltaDeg;
    /** Extra wedge width (total °) from in-battle rotations; see {@link #uncertaintyWedgeExtraDegForInBattleAngle}. */
    private volatile double uncertaintyBattleGrowthDeg;

    /** Cached in {@link #refreshTargetOverlapApproachProgress()} — not in paint (too expensive). */
    private volatile double targetOverlapApproachProgress;
    /**
     * Nanoseconds until target ∩ wedge per {@link #findNextTargetOverlapNanoseconds()}; {@link Long#MAX_VALUE} if none
     * in the search horizon. Used for evenly spaced approach beeps (see controller).
     */
    private volatile long targetOverlapApproachTimeRemainingNs = Long.MAX_VALUE;

    /**
     * When a target ∩ wedge: max over target slots of (overlap arc ° ÷ target slot arc °), 0…1 (not in paint).
     */
    private volatile double targetUncertaintyOverlapPortionOfSlot;

    private volatile boolean warningBeepPending;

    private volatile boolean pikaLead = true;

    private volatile Game game;

    /**
     * Red/Blue calibrated overworld: when true, draw an extra outer uncertainty band for cycle-length slack (367–415
     * frames) in addition to the inner band. Approach beeps/hum/wash use the inner band only; when false, DSum range /
     * painted outer ring omit that slack.
     */
    private volatile boolean showOuterRbCycleUncertaintyWedge = false;

    public EncounterWheelModel(final EncounterSlot targetSlot, final Game game) {
        this.targetSlots = List.of(targetSlot);
        this.game = game;
    }

    public void setRoute(final Route route) {
        this.route = route;
        this.isBlinds = route.isBlinds();
    }

    public void setGame(final Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    public Route getRoute() {
        return route;
    }

    public boolean isPikaLead() {
        return pikaLead;
    }

    public int getLeadLevel() {
        return leadLevel;
    }

    /**
     * Value shown in the details modifier spinner (Yellow uses opposite sign internally in
     * {@link #modifyYellowOverworldDsumCycleModifier(double)}).
     */
    public int getCycleModifierUiValue() {
        final int internal = (int) Math.round(overworldDsumCycleModifier);
        return game == Game.YELLOW ? -internal : internal;
    }

    public boolean isShowOuterRbCycleUncertaintyWedge() {
        return showOuterRbCycleUncertaintyWedge;
    }

    public void setShowOuterRbCycleUncertaintyWedge(final boolean show) {
        showOuterRbCycleUncertaintyWedge = show;
        refreshTargetOverlapApproachProgress();
    }

    /**
     * Whether to paint the muted outer band (non-zero R/B cycle slack and {@link #isShowOuterRbCycleUncertaintyWedge()}).
     */
    public boolean isDrawingOuterRbCycleUncertaintyBand() {
        if (!showOuterRbCycleUncertaintyWedge || calibratedSlot == null) {
            return false;
        }
        if (game == Game.YELLOW) {
            return false;
        }
        return rbOverworldCycleLengthUncertaintyNegDeg() + rbOverworldCycleLengthUncertaintyPosDeg() > 1e-6;
    }

    public void setLeadLevel(final int leadLevel) {
        this.leadLevel = Math.max(1, Math.min(100, leadLevel));
    }

    /**
     * Angle for rotating slot artwork ({@link com.com.poke.rng.dsum.model.view.EncounterWheel}) — same as live
     * {@link #angleDeg} (hardware DSum). Encounters can occur on the first frame of a button press before a step
     * resolves, so the UI does not apply a post-step lag rotation.
     */
    public double getDisplayAngleDeg() {
        return angleDeg;
    }

    public void setPikaLead(final boolean skip) {
        this.pikaLead = skip;
    }

    public void modifyYellowOverworldDsumCycleModifier(final double newModifier) {
        if (game == Game.YELLOW) {
            overworldDsumCycleModifier = -newModifier;
        } else {
            overworldDsumCycleModifier = newModifier;
        }
        overworldDsumCycleModifierNs = ONE_FRAME_NS * overworldDsumCycleModifier;
        refreshTargetOverlapApproachProgress();
    }

    private record BattleEntryGeometry(double angleAfterEntryCorrections, double encounterOffsetDeg) {}

    /** Same geometry as {@link #battleStart(boolean)}: entry jumps + encounter rewind offset (does not mutate state). */
    private BattleEntryGeometry computeBattleEntryGeometry(final boolean altAnimation, final double overworldAngleDeg) {
        final Game game = this.game;
        final long animationFrames = countUpFramesBeforeBattleVisible(altAnimation);
        final double overworldNs = overworldCycleNs();
        final double inBattleNs = game == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
        final double incorrectDownAngle = ((animationFrames * ONE_FRAME_NS) / overworldNs) * 360.0;
        final double correctUpAngle = ((animationFrames * ONE_FRAME_NS) / inBattleNs) * 360.0;
        final double correction =
                (game == Game.YELLOW && pikaLead)
                        ? (PIKACHU_CRY_FRAMES * ONE_FRAME_NS) / inBattleNs * 360.0
                        : 0.0;
        final double angleAfter =
                overworldAngleDeg + correctUpAngle + incorrectDownAngle + correction;
        // Rewind by the same in-battle window as {@code battleEnterTime} (encounter → Space), not full spiral length.
        final double angleDelta = (animationFrames * ONE_FRAME_NS) / inBattleNs * 360.0;
        final double overworldLagDeg =
                ((ENCOUNTER_OVERWORLD_CONTINUATION_FRAMES) * ONE_FRAME_NS / overworldNs) * 360.0;
        final double encounterOffsetDeg =
                game == Game.YELLOW ? angleDelta + overworldLagDeg : angleDelta - overworldLagDeg;
        return new BattleEntryGeometry(angleAfter, encounterOffsetDeg);
    }

    private Triplet<Integer, Integer, Integer> dsumTripletForEncounterAtAngle(
            final double angleAfterBattleEntryJumps,
            final double encounterOffsetDeg) {
        final double[] np = new double[2];
        fillUncertaintyWedgeNegPosDeg(np);
        final double usedDeg =
                game == Game.YELLOW
                        ? angleAfterBattleEntryJumps - encounterOffsetDeg
                        : angleAfterBattleEntryJumps + encounterOffsetDeg;
        final int dsum = dsumFromAngle(usedDeg);
        final int min = dsumFromAngle(usedDeg - np[0]);
        final int max = dsumFromAngle(usedDeg + np[1]);

        return new Triplet<>(min, dsum, max);
    }

    private static double angleFromDsum(final int dsum) {
        return (dsum / (double) DSUM_RANGE) * 360.0;
    }

    private static int dsumFromAngle(final double angleDegrees) {
        double a = angleDegrees % 360.0;
        if (a < 0.0) {
            a += 360.0;
        }
        return (int) ((a / 360.0) * DSUM_RANGE) & 0xFF;
    }

    /** Same wrap as {@link com.com.poke.rng.dsum.model.view.EncounterWheelBar}: fraction of cycle for continuous strip. */
    private static double needleCycleFraction(final double angleDeg) {
        double a = angleDeg % 360.0;
        if (a < 0.0) {
            a += 360.0;
        }
        return a / 360.0;
    }

    /**
     * A unit DSum cell is {@code [d, d + 1)} on the 256-period line. Returns whether it intersects the closed linear
     * span {@code [lo, hi]} (same as the uncertainty band in {@link com.com.poke.rng.dsum.model.view.EncounterWheelBar}),
     * including copies shifted by ±256 so negative / oversized {@code lo, hi} still match.
     */
    private static boolean dsumUnitCellIntersectsLinearSpan(final int d, final double lo, final double hi) {
        for (int k = -1; k <= 1; k++) {
            final double cellLo = d + k * DSUM_RANGE;
            final double cellHi = cellLo + 1;
            if (cellLo <= hi && cellHi >= lo) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds {@code (wMin, wMax)} for {@link #dsumInInclusiveCircularArc} from the boolean hit mask (contiguous on the
     * circle; uses wMin &gt; wMax when the arc crosses 0).
     */
    private static int[] hitMaskToInclusiveCircularArcEndpoints(final boolean[] hit) {
        final int n = DSUM_RANGE;
        int bestLen = 0;
        int bestStart = 0;
        for (int s = 0; s < n; s++) {
            if (!hit[s] || hit[(s - 1 + n) % n]) {
                continue;
            }
            int len = 0;
            for (int k = 0; k < n; k++) {
                if (!hit[(s + k) % n]) {
                    break;
                }
                len++;
            }
            if (len > bestLen) {
                bestLen = len;
                bestStart = s;
            }
        }
        if (bestLen == 0) {
            int any = -1;
            for (int d = 0; d < n; d++) {
                if (hit[d]) {
                    any = d;
                    break;
                }
            }
            if (any < 0) {
                return new int[]{0, 0};
            }
            int len = 0;
            for (int k = 0; k < n; k++) {
                if (!hit[(any + k) % n]) {
                    break;
                }
                len++;
            }
            return new int[]{any, (any + len - 1) % n};
        }
        return new int[]{bestStart, (bestStart + bestLen - 1) % n};
    }

    /** Linear {@code [lo, hi]} DSum interval (EncounterWheelBar) → inclusive circular arc endpoints for wedge helpers. */
    private static int[] linearDsumSpanToInclusiveArcEndpoints(final double lo, final double hi) {
        final boolean[] hit = new boolean[DSUM_RANGE];
        for (int d = 0; d < DSUM_RANGE; d++) {
            if (dsumUnitCellIntersectsLinearSpan(d, lo, hi)) {
                hit[d] = true;
            }
        }
        return hitMaskToInclusiveCircularArcEndpoints(hit);
    }

    private long countUpFramesBeforeBattleVisible(final boolean altAnimation) {
        if (isBlinds) {
            return altAnimation ? COUNT_UP_BEFORE_VERTICAL_BLINDS_END_FRAMES : COUNT_UP_BEFORE_BLINDS_END_FRAMES;
        }
        return game == Game.YELLOW ?
                (altAnimation ? COUNT_UP_BEFORE_YELLOW_FULL_SPIRAL_END_FRAMES : COUNT_UP_BEFORE_YELLOW_SPIRAL_END_FRAMES) :
                (altAnimation ? COUNT_UP_BEFORE_FULL_SPIRAL_END_FRAMES : COUNT_UP_BEFORE_SPIRAL_END_FRAMES);
    }

    /** Gen 1 alternate battle wipe when the wild is at least 3 levels above the player's lead. */
    private static boolean altAnimationForLevels(final int wildLevel, final int playerLeadLevel) {
        return wildLevel - playerLeadLevel >= 3;
    }

    private static double norm360(double angle) {
        angle = angle % 360;
        return angle < 0 ? angle + 360 : angle;
    }

    private static boolean angularRangesOverlap(final double a1, final double a2, final double b1, final double b2) {
        return angleInRange(a1, b1, b2)
                || angleInRange(a2, b1, b2)
                || angleInRange(b1, a1, a2)
                || angleInRange(b2, a1, a2);
    }

    private static boolean angleInRange(final double x, final double start, final double end) {
        if (start <= end) {
            return x >= start && x <= end;
        }
        return x >= start || x <= end;
    }

    public void update(final long now) {
        final long delta = now - lastNow;
        final Game game = this.game;

        if (simulationPaused) {
            lastNow = now;
            angleDeg += manualAngleOffsetDeltaDeg;
            manualAngleOffsetDeltaDeg = 0;
            refreshTargetOverlapApproachProgress();
            return;
        }

        if (battleEnterTime != -1) {
            // In battle: DSum counts up at in-battle cycle rate
            final double inBattleNs = game == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
            angleDeg += ((delta / inBattleNs) * 360.0) + manualAngleOffsetDeltaDeg;
            uncertaintyWedgeExtentDeltaDeg = 0;
            manualAngleOffsetDeltaDeg = 0;
            lastNow = now;
            refreshTargetOverlapApproachProgress();
            return;
        }

        final double overworldNs = game == Game.YELLOW
                ? (YELLOW_OVERWORLD_CYCLE_NS + overworldDsumCycleModifierNs) : OVERWORLD_CYCLE_NS;
        angleDeg += -((delta / overworldNs) * 360.0) + manualAngleOffsetDeltaDeg;
        uncertaintyWedgeExtentDeltaDeg += uncertaintyWedgeExtraDegForInBattleAngle((delta / overworldNs) * 360);
        manualAngleOffsetDeltaDeg = 0;
        if (calibratedSlot != null
                && (game == Game.RED || game == Game.BLUE)) {
            rbOverworldElapsedForCycleUncertaintyNs += delta;
        }
        lastNow = now;
        refreshTargetOverlapApproachProgress();
    }

    public void battleStart(final boolean altAnimation) {
        simulationPaused = false;
        final long now = System.nanoTime();
        final long animationFrames = countUpFramesBeforeBattleVisible(altAnimation);
        assumedBattleAnimationFrames = animationFrames;
        // Retro-correct angleDeg for the entry animation (+ Pikachu cry on Yellow): see computeBattleEntryGeometry.
        // Set battleEnterTime only after rangeAtBattleStart so getUncertaintyWedgeExtentDeg() still sees overworld
        // state (e.g. rangeAtBattleStart null, calibrated-slot wedge) while building the encounter triplet — not a
        // half-initialized in-battle branch with a null range.
        final BattleEntryGeometry geom = computeBattleEntryGeometry(altAnimation, angleDeg);
        angleDeg = geom.angleAfterEntryCorrections();
        rangeAtBattleStart = dsumTripletForEncounterAtAngle(angleDeg, geom.encounterOffsetDeg());
        battleEnterTime = now - (long) (animationFrames * ONE_FRAME_NS);
        /*
         * After battleEnterTime is set: {@link #fillInBattleOrZeroNegPosDeg} yields a non-zero in-battle wedge when
         * uncalibrated, so suggested-slot overlap/segments match the hidden wedge for this encounter (fixed for the
         * whole calibration window — see {@link #innerSuggestedRangeAtBattleStart}).
         */
        final double sugOff = suggestedSlotsEffectiveAngleOffsetDeg();
        innerSuggestedRangeAtBattleStart = computeDsumRangeInnerOnlyAtAngle(angleDeg, sugOff);
        fullSuggestedRangeAtBattleStart = computeDsumRangeFullAtAngle(angleDeg, sugOff);

        refreshTargetOverlapApproachProgress();
    }

    /**
     * Clears calibration and any in-progress battle transition, returning to overworld rotation
     * (down in Red/Blue, up in Yellow). Does not change game, route blinds flag, targets, or Yellow modifiers.
     */
    public void clearCalibrationState(final long now) {
        calibratedSlot = null;
        hasCalibratedAtLeastOnce = false;
        rbOverworldElapsedForCycleUncertaintyNs = 0;
        uncertaintyWedgeExtentDeltaDeg = 0;
        uncertaintyBattleGrowthDeg = 0;
        battleEnterTime = -1;
        rangeAtBattleStart = null;
        innerSuggestedRangeAtBattleStart = null;
        fullSuggestedRangeAtBattleStart = null;
        manualAngleOffsetDeltaDeg = 0;
        warningBeepPending = false;
        lastNow = now;
        refreshTargetOverlapApproachProgress();
    }

    public void calibrateSlot(final int givenSlot) {

        if (battleEnterTime == -1) {
            return;
        }

        final long now = System.nanoTime();
        final Game game = this.game;
        final int slotIndex = givenSlot - 1;
        final EncounterSlot[] slots = EncounterSlot.values();
        if (slotIndex < 0 || slotIndex >= slots.length) {
            return;
        }

        final EncounterSlot slot = slots[slotIndex];
        final int midDsum = (slot.min() + slot.max()) / 2;
        long timeInBattle = now - battleEnterTime;

        if (route != null) {
            final Encounter enc = route.encounterFor(game, slot);
            if (enc != null) {
                final long actualFrames = countUpFramesBeforeBattleVisible(altAnimationForLevels(enc.level(), leadLevel));
                final long deltaFrames = actualFrames - assumedBattleAnimationFrames;
                timeInBattle += (long) (deltaFrames * ONE_FRAME_NS);
            }
        }
        final double inBattleNs = game == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
        double overworldNs = game == Game.YELLOW ? (YELLOW_OVERWORLD_CYCLE_NS + overworldDsumCycleModifierNs) : OVERWORLD_CYCLE_NS;
        double angleChangeInBattle = (timeInBattle / inBattleNs) * 360.0;
        // We're going to start reversing immediately, but the game keeps counting up for a few frames after we clear
        // the 'Got away safely!' message.  We account for that here with whatever this formula is...
        // We /would/ have kept counting up for COUNT_UP_AFTER_GOT_AWAY_FRAMES, at the in battle rate.  But for that
        // number of frames instead, we would be counting down.
        // This results in the wheel being wrong for the COUNT_UP_AFTER_GOT_AWAY frames...  But you can't get an
        // encounter in that window anyways - and makes the maths much simpler.
        final double correctUpAngle = ((COUNT_UP_AFTER_GOT_AWAY_FRAMES * ONE_FRAME_NS) / inBattleNs) * 360.0;
        final double incorrectDownAngle = (((COUNT_UP_AFTER_GOT_AWAY_FRAMES - 1) * ONE_FRAME_NS) / overworldNs) * 360.0;

        angleChangeInBattle += (correctUpAngle + incorrectDownAngle);

        final double angleAtBattleStart = angleFromDsum(midDsum);
        final double newAngle = angleAtBattleStart + angleChangeInBattle;

        // Match {@link #update()} overworld: angleDeg += -((delta / overworldNs) * 360).
        angleDeg = newAngle - ((now - lastNow) / overworldNs) * 360.0;
        battleEnterTime = -1;
        innerSuggestedRangeAtBattleStart = null;
        fullSuggestedRangeAtBattleStart = null;
        calibratedSlot = slot;
        hasCalibratedAtLeastOnce = true;
        rbOverworldElapsedForCycleUncertaintyNs = 0;
        uncertaintyBattleGrowthDeg = uncertaintyWedgeExtraDegForInBattleAngle(angleChangeInBattle);
        uncertaintyWedgeExtentDeltaDeg = 0;
        manualAngleOffsetDeltaDeg = 0;
        overworldStartTime = now + (long) (COUNT_UP_AFTER_GOT_AWAY_FRAMES * ONE_FRAME_NS);
        refreshTargetOverlapApproachProgress();
    }

    public void manualAngle(final boolean positive) {
        manualAngleOffsetDeltaDeg += positive ? OFFSET_STEP_DEG : -OFFSET_STEP_DEG;
    }

    public void uncertaintyDelta(final boolean positive) {
        uncertaintyWedgeExtentDeltaDeg += positive ? OFFSET_STEP_DEG : -OFFSET_STEP_DEG;
        refreshTargetOverlapApproachProgress();
    }

    public boolean consumeWarningBeep() {
        final boolean pending = warningBeepPending;
        warningBeepPending = false;
        return pending;
    }

    public boolean isCalibrating() {
        return battleEnterTime != -1;
    }

    public boolean isSimulationPaused() {
        return simulationPaused;
    }

    /** Toggle freeze of time-based DSum rotation (hotkey {@code P}). */
    public void toggleSimulationPaused() {
        simulationPaused = !simulationPaused;
    }

    /**
     * Second line on the DSum readout chip: overworld vs in-battle vs uncalibrated, with optional {@code Paused ·}
     * prefix when {@link #isSimulationPaused()}. After the first {@link #calibrateSlot}, in-battle shows
     * {@code Encounter} until {@link #clearCalibrationState}.
     */
    public String getDsumChipStateFootnote(final boolean compact) {
        final String base;
        if (isCalibrating()) {
            base =
                    hasCalibratedAtLeastOnce
                            ? "Encounter"
                            : (compact ? "Calibr." : "Calibrating");
        } else if (getCalibratedSlot() == null) {
            base = compact ? "Uncalib." : "Uncalibrated";
        } else {
            base = compact ? "Field" : "Overworld";
        }
        if (!simulationPaused) {
            return base;
        }
        if (compact) {
            return "P \u00b7 " + base;
        }
        return "Paused (P) \u00b7 " + base;
    }

    /**
     * Bottom-left hint chip (uncalibrated, or first {@link #calibrateSlot} only). Hidden during later encounters until
     * {@link #clearCalibrationState}. Use {@code \\n} in full strings for line breaks.
     */
    public String getDsumInstructionChipText(final boolean compact) {
        if (isCalibrating()) {
            if (hasCalibratedAtLeastOnce) {
                return null;
            }
            return compact ? "Press 0—10 for your slot" : "Press the number for the\nencounter slot you got\nwhen clearing 'Got away'.";
        }
        if (getCalibratedSlot() == null) {
            return compact ? "Encounter, then Space" : "Get an encounter,\nthen press Space\nwhen wipe ends.";
        }
        return null;
    }

    /**
     * Whether any target slot intersects the <em>inner</em> uncertainty wedge (slot + growth + manual only), not the
     * optional outer R/B cycle-length band — drives overlap wash, approach bar, beeps, and hum.
     */
    public boolean targetOverlapsUncertainty() {
        if (calibratedSlot == null) {
            return false;
        }

        final double[] np = new double[2];
        fillInnerUncertaintyWedgeNegPosDeg(np);
        final double arrowWheelDeg = 90 + angleDeg;
        final double w1 = norm360(arrowWheelDeg - np[0]);
        final double w2 = norm360(arrowWheelDeg + np[1]);
        for (final EncounterSlot slot : targetSlots) {
            final double t1 = norm360((slot.min() / (double) DSUM_RANGE) * 360 + 90);
            final double t2 = norm360(((slot.max() + 1) / (double) DSUM_RANGE) * 360 + 90);

            if (angularRangesOverlap(w1, w2, t1, t2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fraction of the target slot arc covered by the <em>inner</em> uncertainty wedge (max over selected targets).
     * Meaningful when {@link #targetOverlapsUncertainty()} is true; otherwise 0.
     */
    public double getTargetUncertaintyOverlapPortionOfSlot() {
        return targetUncertaintyOverlapPortionOfSlot;
    }

    private double computeMaxTargetWedgeOverlapPortionOfSlot() {
        final double[] np = new double[2];
        fillInnerUncertaintyWedgeNegPosDeg(np);
        final double arrowWheelDeg = 90 + angleDeg;
        final double w1 = norm360(arrowWheelDeg - np[0]);
        final double w2 = norm360(arrowWheelDeg + np[1]);
        double best = 0.0;
        for (final EncounterSlot slot : targetSlots) {
            final double t1 = norm360((slot.min() / (double) DSUM_RANGE) * 360 + 90);
            final double t2 = norm360(((slot.max() + 1) / (double) DSUM_RANGE) * 360 + 90);
            if (!angularRangesOverlap(w1, w2, t1, t2)) {
                continue;
            }
            final double slotSpanDeg = ((slot.max() - slot.min() + 1) / (double) DSUM_RANGE) * 360.0;
            if (slotSpanDeg <= 1e-6) {
                continue;
            }
            final double interDeg = arcIntersectionMeasureDegrees(w1, w2, t1, t2);
            best = Math.max(best, interDeg / slotSpanDeg);
        }
        return Math.max(0.0, Math.min(1.0, best));
    }

    /** Measure of intersection of two circle arcs defined by paired bounds (same wrap rules as overlap checks). */
    private static double arcIntersectionMeasureDegrees(
            final double a1, final double a2, final double b1, final double b2) {
        final int samples = 720;
        int hit = 0;
        for (int i = 0; i < samples; i++) {
            final double x = 360.0 * i / samples;
            if (angleInRange(x, a1, a2) && angleInRange(x, b1, b2)) {
                hit++;
            }
        }
        return 360.0 * hit / samples;
    }

    /**
     * Last value from {@link #refreshTargetOverlapApproachProgress()} (once per tick / state change).
     */
    public double getTargetOverlapApproachProgress() {
        return targetOverlapApproachProgress;
    }

    /**
     * Remaining simulated time (ns) until overlap with a target slot, or {@link Long#MAX_VALUE} if not approaching.
     */
    public long getTargetOverlapApproachTimeRemainingNs() {
        return targetOverlapApproachTimeRemainingNs;
    }

    private void refreshTargetOverlapApproachProgress() {
        if (calibratedSlot == null || targetSlots.isEmpty() || battleEnterTime != -1) {
            targetOverlapApproachProgress = 0.0;
            targetUncertaintyOverlapPortionOfSlot = 0.0;
            targetOverlapApproachTimeRemainingNs = Long.MAX_VALUE;
            return;
        }
        if (targetOverlapsUncertainty()) {
            targetUncertaintyOverlapPortionOfSlot = computeMaxTargetWedgeOverlapPortionOfSlot();
            targetOverlapApproachProgress = 1.0;
            targetOverlapApproachTimeRemainingNs = 0L;
            return;
        }
        targetUncertaintyOverlapPortionOfSlot = 0.0;
        final long t = findNextTargetOverlapNanoseconds();
        if (t == Long.MAX_VALUE) {
            targetOverlapApproachProgress = 0.0;
            targetOverlapApproachTimeRemainingNs = Long.MAX_VALUE;
            return;
        }
        targetOverlapApproachTimeRemainingNs = t;
        targetOverlapApproachProgress =
                Math.max(0.0, Math.min(1.0, 1.0 - (t / TARGET_OVERLAP_WARN_NS)));
    }

    private double overworldCycleNs() {
        return game == Game.YELLOW
                ? (YELLOW_OVERWORLD_CYCLE_NS + overworldDsumCycleModifierNs)
                : OVERWORLD_CYCLE_NS;
    }

    private boolean overlapsAtForwardNanoseconds(final long deltaNs) {
        if (calibratedSlot == null) {
            return false;
        }
        final double overworldNs = overworldCycleNs();
        final double base = angleDeg;
        final double futureArrowWheelDeg = 90 + base + (-360.0 / overworldNs) * deltaNs;
        final double[] np = new double[2];
        fillInnerUncertaintyWedgeNegPosDeg(np);
        final double w1 = norm360(futureArrowWheelDeg - np[0]);
        final double w2 = norm360(futureArrowWheelDeg + np[1]);
        for (final EncounterSlot slot : targetSlots) {
            final double t1 = norm360((slot.min() / (double) DSUM_RANGE) * 360 + 90);
            final double t2 = norm360(((slot.max() + 1) / (double) DSUM_RANGE) * 360 + 90);
            if (angularRangesOverlap(w1, w2, t1, t2)) {
                return true;
            }
        }
        return false;
    }

    private long findNextTargetOverlapNanoseconds() {
        final double overworldNs = overworldCycleNs();
        final long step = Math.max(1L, (long) (overworldNs / 720));
        final long horizon = (long) (overworldNs * 2.5);
        for (long t = step; t <= horizon; t += step) {
            if (overlapsAtForwardNanoseconds(t)) {
                return refineFirstOverlapForward(0, t);
            }
        }
        return Long.MAX_VALUE;
    }

    private long refineFirstOverlapForward(final long lo, final long hi) {
        long low = lo;
        long high = hi;
        while (high - low > 50_000L) {
            final long mid = (low + high) / 2;
            if (overlapsAtForwardNanoseconds(mid)) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return high;
    }

    /**
     * Total angular span (°) of the uncertainty wedge at the needle — {@link #getUncertaintyWedgeExtentNegDeg()} plus
     * {@link #getUncertaintyWedgeExtentPosDeg()}.
     */
    public double getUncertaintyWedgeExtentDeg() {
        final double[] np = new double[2];
        fillUncertaintyWedgeNegPosDeg(np);
        return np[0] + np[1];
    }

    /**
     * Degrees of arc on one side of the fixed needle (toward smaller screen-space slice start angle) for the grey
     * wedge / band — half of the slot/battle/manual core plus Red/Blue cycle-length slack toward the
     * {@linkplain #RB_OVERWORLD_CYCLE_LENGTH_MIN_FRAMES minimum} overworld frame count.
     */
    public double getUncertaintyWedgeExtentNegDeg() {
        final double[] np = new double[2];
        fillUncertaintyWedgeNegPosDeg(np);
        return np[0];
    }

    /**
     * Degrees of arc on the other side of the needle — core half plus slack toward the
     * {@linkplain #RB_OVERWORLD_CYCLE_LENGTH_MAX_FRAMES maximum} overworld frame count.
     */
    public double getUncertaintyWedgeExtentPosDeg() {
        final double[] np = new double[2];
        fillUncertaintyWedgeNegPosDeg(np);
        return np[1];
    }

    /** Inner band only: calibrated slot span + 2°/rotation growth + manual bracket tweak (no R/B cycle-length slack). */
    public double getInnerUncertaintyWedgeExtentNegDeg() {
        final double[] np = new double[2];
        fillInnerUncertaintyWedgeNegPosDeg(np);
        return np[0];
    }

    /** @see #getInnerUncertaintyWedgeExtentNegDeg() */
    public double getInnerUncertaintyWedgeExtentPosDeg() {
        final double[] np = new double[2];
        fillInnerUncertaintyWedgeNegPosDeg(np);
        return np[1];
    }

    private void fillUncertaintyWedgeNegPosDeg(final double[] out) {
        if (calibratedSlot != null) {
            final double half = symmetricCalibratedWedgeCoreExtentDeg() * 0.5;
            if (showOuterRbCycleUncertaintyWedge && (game == Game.RED || game == Game.BLUE)) {
                out[0] = half + rbOverworldCycleLengthUncertaintyNegDeg();
                out[1] = half + rbOverworldCycleLengthUncertaintyPosDeg();
            } else {
                out[0] = half;
                out[1] = half;
            }
            normalizeNegPosTotalDeg(out);
            return;
        }
        fillInBattleOrZeroNegPosDeg(out);
    }

    private void fillInnerUncertaintyWedgeNegPosDeg(final double[] out) {
        if (calibratedSlot != null) {
            final double half = symmetricCalibratedWedgeCoreExtentDeg() * 0.5;
            out[0] = half;
            out[1] = half;
            normalizeNegPosTotalDeg(out);
            return;
        }
        fillInBattleOrZeroNegPosDeg(out);
    }

    private void fillInBattleOrZeroNegPosDeg(final double[] out) {
        if (battleEnterTime == -1 || rangeAtBattleStart == null) {
            out[0] = 0.0;
            out[1] = 0.0;
            return;
        }
        final Game g = this.game;
        final double inBattleNs = g == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
        final long elapsed = System.nanoTime() - battleEnterTime;
        final double liveBattleAngleDeg = (elapsed / inBattleNs) * 360.0;
        final EncounterSlot atEncounter = slotContainingDsum(rangeAtBattleStart.second());
        final double encounterSlotDeg =
                ((atEncounter.max() - atEncounter.min() + 1) / (double) DSUM_RANGE) * 360.0;
        final double extra = uncertaintyWedgeExtraDegForInBattleAngle(liveBattleAngleDeg);
        final double total = Math.max(1.0, Math.min(360.0, encounterSlotDeg + extra));
        final double h = total * 0.5;
        out[0] = h;
        out[1] = h;
    }

    private double symmetricCalibratedWedgeCoreExtentDeg() {
        final double base =
                ((calibratedSlot.max() - calibratedSlot.min() + 1) / (double) DSUM_RANGE) * 360.0;
        return base + uncertaintyBattleGrowthDeg + uncertaintyWedgeExtentDeltaDeg;
    }

    /** Keeps total wedge span in {@code [1°, 360°]} without changing the neg/pos ratio. */
    private static void normalizeNegPosTotalDeg(final double[] out) {
        double n = out[0];
        double p = out[1];
        final double t = n + p;
        if (t <= 0.0) {
            out[0] = 0.5;
            out[1] = 0.5;
            return;
        }
        if (t < 1.0) {
            final double s = 1.0 / t;
            out[0] = n * s;
            out[1] = p * s;
        } else if (t > 360.0) {
            final double s = 360.0 / t;
            out[0] = n * s;
            out[1] = p * s;
        }
    }

    /** Total ° to add to slot span from in-battle DSum rotation {@code battleAngleDeg} (linear in 360° turns). */
    private static double uncertaintyWedgeExtraDegForInBattleAngle(final double battleAngleDeg) {
        final double rotations = Math.abs(battleAngleDeg / 360.0);
        return 2.0 * UNCERTAINTY_WEDGE_DEGREES_DELTA_PER_ROTATION * rotations;
    }

    /**
     * Red/Blue: magnitude (°) the true DSum could lie on the “min frames” side of the nominal needle after
     * {@link #rbOverworldElapsedForCycleUncertaintyNs} — true cycle shorter → faster rotation vs nominal.
     */
    private double rbOverworldCycleLengthUncertaintyNegDeg() {
        if (game == Game.YELLOW) {
            return 0.0;
        }
        final long dtNs = rbOverworldElapsedForCycleUncertaintyNs;
        if (dtNs <= 0L) {
            return 0.0;
        }
        final double scale = 360.0 / ONE_FRAME_NS;
        final double invNom = 1.0 / OVERWORLD_DSUM_CYCLE_FRAMES;
        return scale * dtNs * (1.0 / RB_OVERWORLD_CYCLE_LENGTH_MIN_FRAMES - invNom);
    }

    /** Red/Blue: counterpart on the “max frames” (slower) side of the needle. */
    private double rbOverworldCycleLengthUncertaintyPosDeg() {
        if (game == Game.YELLOW) {
            return 0.0;
        }
        final long dtNs = rbOverworldElapsedForCycleUncertaintyNs;
        if (dtNs <= 0L) {
            return 0.0;
        }
        final double scale = 360.0 / ONE_FRAME_NS;
        final double invNom = 1.0 / OVERWORLD_DSUM_CYCLE_FRAMES;
        return scale * dtNs * (invNom - 1.0 / RB_OVERWORLD_CYCLE_LENGTH_MAX_FRAMES);
    }

    private static EncounterSlot slotContainingDsum(final int dsum) {
        final int d = dsum & (DSUM_RANGE - 1);
        for (final EncounterSlot s : EncounterSlot.values()) {
            if (d >= s.min() && d <= s.max()) {
                return s;
            }
        }
        return EncounterSlot.values()[0];
    }

    public double getAngleDeg() {
        return angleDeg;
    }

    public int getDsum() {
        return dsumFromAngle(angleDeg);
    }

    /** DSum at the fixed needle for the painted wheel / bar (same as {@link #getDsum()}). */
    public int getDisplayDsum() {
        return getDsum();
    }

    public Triplet<Integer, Integer, Integer> getDsumRange() {
        return getDsumRange(0);
    }

    public Triplet<Integer, Integer, Integer> getDsumRange(final double angleOffset) {
        return computeDsumRangeFullAtAngle(angleDeg, angleOffset);
    }

    /**
     * Like {@link #getDsumRange(double)} but uses the inner uncertainty wedge only (no R/B outer cycle band).
     */
    public Triplet<Integer, Integer, Integer> getDsumRangeInnerOnly(final double angleOffset) {
        return computeDsumRangeInnerOnlyAtAngle(angleDeg, angleOffset);
    }

    /** Same linear-span construction as {@link #getDsumRange(double)} at an explicit live wheel angle. */
    private Triplet<Integer, Integer, Integer> computeDsumRangeFullAtAngle(
            final double liveAngleDeg, final double angleOffset) {
        final double usedDeg = liveAngleDeg + angleOffset;
        final int dsum = dsumFromAngle(usedDeg);
        /*
         * Match EncounterWheelBar#drawUncertaintyBand: centre is linear cycle fraction × 256; span uses asymmetric
         * neg/pos ° → DSum units per side. Endpoints can disagree with two-angle sampling when the strip wraps.
         */
        final double[] np = new double[2];
        fillUncertaintyWedgeNegPosDeg(np);
        final double wdNeg = (np[0] / 360.0) * DSUM_RANGE;
        final double wdPos = (np[1] / 360.0) * DSUM_RANGE;
        final double dCenter = needleCycleFraction(usedDeg) * DSUM_RANGE;
        final int[] ends = linearDsumSpanToInclusiveArcEndpoints(dCenter - wdNeg, dCenter + wdPos);
        return new Triplet<>(ends[0], dsum, ends[1]);
    }

    /** Same as {@link #getDsumRangeInnerOnly(double)} at an explicit live wheel angle. */
    private Triplet<Integer, Integer, Integer> computeDsumRangeInnerOnlyAtAngle(
            final double liveAngleDeg, final double angleOffset) {
        final double usedDeg = liveAngleDeg + angleOffset;
        final int dsum = dsumFromAngle(usedDeg);
        final double[] np = new double[2];
        fillInnerUncertaintyWedgeNegPosDeg(np);
        final double wdNeg = (np[0] / 360.0) * DSUM_RANGE;
        final double wdPos = (np[1] / 360.0) * DSUM_RANGE;
        final double dCenter = needleCycleFraction(usedDeg) * DSUM_RANGE;
        final int[] ends = linearDsumSpanToInclusiveArcEndpoints(dCenter - wdNeg, dCenter + wdPos);
        return new Triplet<>(ends[0], dsum, ends[1]);
    }

    /**
     * DSum triple for suggested-slot UI only. Overworld (calibrated): {@link #getDsumRange(double)} with
     * {@link #suggestedSlotsEffectiveAngleOffsetDeg()} (no step-lag offset). Calibrating:
     * {@link #getDsumRangeAtStartOfBattle()}.
     */
    public Triplet<Integer, Integer, Integer> getDsumRangeForSuggestedSlots() {
        if (isCalibrating()) {
            return getDsumRangeAtStartOfBattle();
        }
        return getDsumRange(suggestedSlotsEffectiveAngleOffsetDeg());
    }

    public Triplet<Integer, Integer, Integer> getDsumRangeAtStartOfBattle() {
        return rangeAtBattleStart;
    }

    private double suggestedSlotsEffectiveAngleOffsetDeg() {
        final double sign = game == Game.YELLOW ? -1.0 : 1.0;
        return sign * SUGGESTED_SLOTS_ANGLE_OFFSET_DEG;
    }

    /**
     * For suggested-slot tinting: fraction {@code 0…1} of the <em>full</em> suggested wedge (inner + optional outer)
     * that lies in {@code slot}. Prefer {@link #encounterInnerWedgeOverlapPortionOfSlot} and
     * {@link #encounterOuterOnlyWedgeOverlapPortionOfSlot} for layered colouring.
     */
    public OptionalDouble encounterWedgeOverlapPortionOfSlot(final EncounterSlot slot) {
        final Triplet<Integer, Integer, Integer> wedgeTriplet;
        if (isCalibrating()) {
            if (fullSuggestedRangeAtBattleStart == null) {
                return OptionalDouble.empty();
            }
            wedgeTriplet = fullSuggestedRangeAtBattleStart;
        } else if (getCalibratedSlot() != null) {
            wedgeTriplet = getDsumRange(suggestedSlotsEffectiveAngleOffsetDeg());
        } else {
            return OptionalDouble.empty();
        }
        return dsumArcOverlapPortionOfSlot(wedgeTriplet.first(), wedgeTriplet.third(), slot);
    }

    /**
     * Fraction of the <em>inner</em> uncertainty wedge that lies in {@code slot}'s DSum range — use for strong
     * suggestion colour (darker when this slot accounts for more of the wedge).
     */
    public OptionalDouble encounterInnerWedgeOverlapPortionOfSlot(final EncounterSlot slot) {
        final Triplet<Integer, Integer, Integer> wedgeTriplet;
        if (isCalibrating()) {
            if (innerSuggestedRangeAtBattleStart == null) {
                return OptionalDouble.empty();
            }
            wedgeTriplet = innerSuggestedRangeAtBattleStart;
        } else if (getCalibratedSlot() != null) {
            wedgeTriplet = getDsumRangeInnerOnly(suggestedSlotsEffectiveAngleOffsetDeg());
        } else {
            return OptionalDouble.empty();
        }
        return dsumArcOverlapPortionOfSlot(wedgeTriplet.first(), wedgeTriplet.third(), slot);
    }

    /**
     * Fraction of the <em>outer-only</em> ring (full wedge minus inner) that lies in {@code slot} — faint hint colour.
     * Empty when the outer band is not shown or there is no outer-only overlap.
     */
    public OptionalDouble encounterOuterOnlyWedgeOverlapPortionOfSlot(final EncounterSlot slot) {
        if (isCalibrating() || getCalibratedSlot() == null) {
            return OptionalDouble.empty();
        }
        if (!isDrawingOuterRbCycleUncertaintyBand()) {
            return OptionalDouble.empty();
        }
        final double offset = suggestedSlotsEffectiveAngleOffsetDeg();
        final Triplet<Integer, Integer, Integer> innerT = getDsumRangeInnerOnly(offset);
        final Triplet<Integer, Integer, Integer> fullT = getDsumRange(offset);
        return dsumOuterOnlyOverlapPortionOfSlot(
                innerT.first(), innerT.third(), fullT.first(), fullT.third(), slot);
    }

    private OptionalDouble dsumOuterOnlyOverlapPortionOfSlot(
            final int innerMinRaw,
            final int innerMaxRaw,
            final int fullMinRaw,
            final int fullMaxRaw,
            final EncounterSlot slot) {
        final int innerMin = innerMinRaw & (DSUM_RANGE - 1);
        final int innerMax = innerMaxRaw & (DSUM_RANGE - 1);
        final int fullMin = fullMinRaw & (DSUM_RANGE - 1);
        final int fullMax = fullMaxRaw & (DSUM_RANGE - 1);
        int den = 0;
        int num = 0;
        for (int d = 0; d < DSUM_RANGE; d++) {
            final boolean inFull = dsumInInclusiveCircularArc(d, fullMin, fullMax);
            final boolean inInner = dsumInInclusiveCircularArc(d, innerMin, innerMax);
            if (inFull && !inInner) {
                den++;
                if (d >= slot.min() && d <= slot.max()) {
                    num++;
                }
            }
        }
        if (num <= 0 || den <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.max(0.0, Math.min(1.0, num / (double) den)));
    }

    /**
     * Contiguous inclusive DSum runs where {@code slot}'s range meets the <em>inner</em> suggested uncertainty wedge
     * (empty when uncalibrated / no overlap). Used to clip suggestion paint to actual overlap geometry.
     */
    public List<int[]> suggestedInnerWedgeDsumSegmentsForSlot(final EncounterSlot slot) {
        final Triplet<Integer, Integer, Integer> wedgeTriplet;
        if (isCalibrating()) {
            if (innerSuggestedRangeAtBattleStart == null) {
                return List.of();
            }
            wedgeTriplet = innerSuggestedRangeAtBattleStart;
        } else if (getCalibratedSlot() != null) {
            wedgeTriplet = getDsumRangeInnerOnly(suggestedSlotsEffectiveAngleOffsetDeg());
        } else {
            return List.of();
        }
        final int wMin = wedgeTriplet.first() & (DSUM_RANGE - 1);
        final int wMax = wedgeTriplet.third() & (DSUM_RANGE - 1);
        return contiguousDsumHitsInLinearRange(slot.min(), slot.max(), wMin, wMax);
    }

    /**
     * Contiguous inclusive DSum runs where {@code slot} meets the outer R/B cycle band only (not the inner core).
     */
    public List<int[]> suggestedOuterOnlyWedgeDsumSegmentsForSlot(final EncounterSlot slot) {
        if (isCalibrating() || getCalibratedSlot() == null || !isDrawingOuterRbCycleUncertaintyBand()) {
            return List.of();
        }
        final double offset = suggestedSlotsEffectiveAngleOffsetDeg();
        final Triplet<Integer, Integer, Integer> innerT = getDsumRangeInnerOnly(offset);
        final Triplet<Integer, Integer, Integer> fullT = getDsumRange(offset);
        final int iLo = innerT.first() & (DSUM_RANGE - 1);
        final int iHi = innerT.third() & (DSUM_RANGE - 1);
        final int fLo = fullT.first() & (DSUM_RANGE - 1);
        final int fHi = fullT.third() & (DSUM_RANGE - 1);
        final List<int[]> segs = new ArrayList<>();
        int runStart = -1;
        for (int d = slot.min(); d <= slot.max(); d++) {
            final boolean inFull = dsumInInclusiveCircularArc(d, fLo, fHi);
            final boolean inInner = dsumInInclusiveCircularArc(d, iLo, iHi);
            final boolean want = inFull && !inInner;
            if (want) {
                if (runStart < 0) {
                    runStart = d;
                }
            } else {
                if (runStart >= 0) {
                    segs.add(new int[]{runStart, d - 1});
                    runStart = -1;
                }
            }
        }
        if (runStart >= 0) {
            segs.add(new int[]{runStart, slot.max()});
        }
        return segs;
    }

    private static List<int[]> contiguousDsumHitsInLinearRange(
            final int sLo, final int sHi, final int arcLo, final int arcHi) {
        final List<int[]> segs = new ArrayList<>();
        int runStart = -1;
        for (int d = sLo; d <= sHi; d++) {
            if (dsumInInclusiveCircularArc(d, arcLo, arcHi)) {
                if (runStart < 0) {
                    runStart = d;
                }
            } else {
                if (runStart >= 0) {
                    segs.add(new int[]{runStart, d - 1});
                    runStart = -1;
                }
            }
        }
        if (runStart >= 0) {
            segs.add(new int[]{runStart, sHi});
        }
        return segs;
    }

    private OptionalDouble dsumArcOverlapPortionOfSlot(
            final int wMinRaw, final int wMaxRaw, final EncounterSlot slot) {
        final int wMin = wMinRaw & (DSUM_RANGE - 1);
        final int wMax = wMaxRaw & (DSUM_RANGE - 1);
        final int wedgeCount = countDsumValuesInInclusiveCircularArc(wMin, wMax);
        if (wedgeCount <= 0) {
            return OptionalDouble.empty();
        }
        final int overlap = countDsumValuesInWedgeAndSlot(wMin, wMax, slot.min(), slot.max());
        if (overlap <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.max(0.0, Math.min(1.0, overlap / (double) wedgeCount)));
    }

    private static boolean dsumInInclusiveCircularArc(final int d, final int arcLo, final int arcHi) {
        final int x = d & (DSUM_RANGE - 1);
        final int lo = arcLo & (DSUM_RANGE - 1);
        final int hi = arcHi & (DSUM_RANGE - 1);
        if (lo <= hi) {
            return x >= lo && x <= hi;
        }
        return x >= lo || x <= hi;
    }

    private static int countDsumValuesInInclusiveCircularArc(final int arcLo, final int arcHi) {
        int n = 0;
        for (int d = 0; d < DSUM_RANGE; d++) {
            if (dsumInInclusiveCircularArc(d, arcLo, arcHi)) {
                n++;
            }
        }
        return n;
    }

    private static int countDsumValuesInWedgeAndSlot(
            final int wLo, final int wHi, final int sLo, final int sHi) {
        int n = 0;
        for (int d = 0; d < DSUM_RANGE; d++) {
            if (dsumInInclusiveCircularArc(d, wLo, wHi) && d >= sLo && d <= sHi) {
                n++;
            }
        }
        return n;
    }

    public List<EncounterSlot> getTargetSlots() {
        return targetSlots;
    }

    public void setOnTargetSlotsChanged(final Runnable onTargetSlotsChanged) {
        this.onTargetSlotsChanged = onTargetSlotsChanged;
    }

    public void setTargetSlots(final List<EncounterSlot> slots) {
        final List<EncounterSlot> next = List.copyOf(slots);
        final boolean changed = !next.equals(this.targetSlots);
        this.targetSlots = next;
        if (changed && onTargetSlotsChanged != null) {
            onTargetSlotsChanged.run();
        }
        refreshTargetOverlapApproachProgress();
    }

    public void setSuggestedSlots(final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> suggested) {
        this.suggestedSlots = suggested;
    }

    public Triplet<EncounterSlot, EncounterSlot, EncounterSlot> getSuggestedSlots() {
        return suggestedSlots;
    }

    /**
     * True if any slot on the suggested walk (first → … → third) has inner or outer-only uncertainty overlap.
     * Used by the wheel / bar to avoid painting full-slot distance-only amber on non-overlapping slots when other slots
     * in the same run show partial wedge overlap (which otherwise looks like a stray disconnected ring).
     */
    public boolean suggestedRunHasAnyWedgeOverlap(final Triplet<EncounterSlot, EncounterSlot, EncounterSlot> t) {
        if (t == null) {
            return false;
        }
        final int firstIndex = t.first().ordinal();
        final int end = t.third().ordinal();
        final int n = EncounterSlot.values().length;
        int idx = firstIndex;
        while (true) {
            final EncounterSlot slot = EncounterSlot.values()[idx];
            final OptionalDouble inner = encounterInnerWedgeOverlapPortionOfSlot(slot);
            final OptionalDouble outer = encounterOuterOnlyWedgeOverlapPortionOfSlot(slot);
            if (inner.isPresent() && inner.getAsDouble() > 1e-9) {
                return true;
            }
            if (outer.isPresent() && outer.getAsDouble() > 1e-9) {
                return true;
            }
            if (idx == end) {
                break;
            }
            idx = (idx + 1) % n;
        }
        return false;
    }

    public EncounterSlot getCalibratedSlot() {
        return calibratedSlot;
    }
}
