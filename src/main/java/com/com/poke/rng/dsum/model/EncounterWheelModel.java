package com.com.poke.rng.dsum.model;

import com.com.poke.rng.dsum.constants.Encounter;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.util.Triplet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public class EncounterWheelModel {

    // GameBoy's native frame rate.
    private static final double FRAME_RATE = 59.7275;

    // Red/Blue:
    // Average number of frames for one DSum cycle out of battle (counting down).
    private static final double OVERWORLD_DSUM_CYCLE_FRAMES = 372.221374;
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
     * in-battle stepping fully applies (down in Red/Blue, up in Yellow — same signed period as {@link #update()}).
     * Suggested-slot prediction offsets by this much overworld rotation.
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
     * Wedge widens by this many degrees on each side for each full in-battle wheel rotation (360° at in-battle rate);
     * total added width is twice this (e.g. 1.5 rotations → 4.5°/side → 9° total).
     */
    private static final double UNCERTAINTY_WEDGE_DEGREES_DELTA_PER_ROTATION = 3.0;

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

    private volatile OverworldMovementMode overworldMovementMode = OverworldMovementMode.BIKE;

    /**
     * Count-up frames assumed at {@link #battleStart(boolean)} ({@code #COUNT_UP_BEFORE_*}) — used to retro-correct
     * calibration when actual animation length differed (see wild vs. lead level).
     */
    private volatile long assumedBattleAnimationFrames = COUNT_UP_BEFORE_SPIRAL_END_FRAMES;

    private volatile long overworldStartTime = System.nanoTime();
    private volatile long lastNow = overworldStartTime;
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

    /** When set (e.g. F2), UI may show where encounter DSum would land if Space were pressed now (overworld only). */
    private volatile boolean showSpaceEncounterPreview;

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

    public boolean isSpaceEncounterPreviewVisible() {
        return showSpaceEncounterPreview;
    }

    public void setSpaceEncounterPreviewVisible(final boolean visible) {
        this.showSpaceEncounterPreview = visible;
    }

    /**
     * Overworld only: estimated encounter-time DSum band (min, center, max) if {@link #battleStart(boolean)} ran now with
     * the same {@code altAnimation} flag as Space (here, Shift = alternate wipe). Empty while calibrating / in battle.
     */
    public Optional<Triplet<Integer, Integer, Integer>> peekEncounterDsumTripletIfSpacePressed(
            final boolean altAnimation) {
        if (battleEnterTime != -1) {
            return Optional.empty();
        }
        final BattleEntryGeometry geom = computeBattleEntryGeometry(altAnimation, angleDeg);
        return Optional.of(dsumTripletForEncounterAtAngle(geom.angleAfterEntryCorrections(), geom.encounterOffsetDeg()));
    }

    /**
     * ° offset for F2 preview wedge from the needle (screen space): in-battle angular span through the entry animation
     * window — same as {@link #computeBattleEntryGeometry}'s {@code angleDelta} (~{@code animationFrames} / in-battle
     * frame length × 360°, e.g. ~62° on Yellow). This is intentionally <em>not</em>
     * {@code encounterAngle − angleDeg}, which algebraically collapses to {@code incorrectDown + pika − overworldLag}
     * (~few °) and matched the triplet sitting near the current DSum, not the “~135 in-battle frames” slice users expect.
     */
    public OptionalDouble peekEncounterPreviewOffsetFromNeedleDeg(final boolean altAnimation) {
        if (battleEnterTime != -1) {
            return OptionalDouble.empty();
        }
        final long animationFrames = countUpFramesBeforeBattleVisible(altAnimation);
        final double inBattleNs = game == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
        final double angleDelta = ((animationFrames * ONE_FRAME_NS) / inBattleNs) * 360.0;
        // Yellow overworld counts the ring the other way vs R/B; same +angleDelta looked correct on R/B but reversed on Yellow.
        final double offsetDeg = game == Game.YELLOW ? -angleDelta : angleDelta;
        return OptionalDouble.of(offsetDeg);
    }

    public void setLeadLevel(final int leadLevel) {
        this.leadLevel = Math.max(1, Math.min(100, leadLevel));
    }

    /**
     * Angle for rotating slot artwork ({@link com.com.poke.rng.dsum.model.view.EncounterWheel}) — live
     * {@link #angleDeg} plus {@link #overworldMovementLagOffsetDeg()} on overworld so the ring matches where the
     * counter will be after step lag (walk / bike / bonk). Overlap, approach timing, and suggested bands use
     * {@link #getAngleDeg()} only — no lag in that logic.
     */
    public double getDisplayAngleDeg() {
        return angleDeg + overworldMovementLagOffsetDeg();
    }

    public void setPikaLead(final boolean skip) {
        this.pikaLead = skip;
    }

    public void setOverworldMovementMode(final OverworldMovementMode mode) {
        overworldMovementMode = Objects.requireNonNull(mode);
        refreshTargetOverlapApproachProgress();
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
        final double uncertaintyWedgeHalf = getUncertaintyWedgeExtentDeg() / 2;
        final double usedDeg =
                game == Game.YELLOW
                        ? angleAfterBattleEntryJumps - encounterOffsetDeg
                        : angleAfterBattleEntryJumps + encounterOffsetDeg;
        final int dsum = dsumFromAngle(usedDeg);
        final int min = dsumFromAngle(usedDeg - uncertaintyWedgeHalf);
        final int max = dsumFromAngle(usedDeg + uncertaintyWedgeHalf);

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
        lastNow = now;
        refreshTargetOverlapApproachProgress();
    }

    public void battleStart(final boolean altAnimation) {
        simulationPaused = false;
        showSpaceEncounterPreview = false;
        final long now = System.nanoTime();
        final long animationFrames = countUpFramesBeforeBattleVisible(altAnimation);
        assumedBattleAnimationFrames = animationFrames;
        battleEnterTime = now - (long) (animationFrames * ONE_FRAME_NS);
        // Retro-correct angleDeg for the entry animation (+ Pikachu cry on Yellow): see computeBattleEntryGeometry.
        final BattleEntryGeometry geom = computeBattleEntryGeometry(altAnimation, angleDeg);
        angleDeg = geom.angleAfterEntryCorrections();
        rangeAtBattleStart = dsumTripletForEncounterAtAngle(angleDeg, geom.encounterOffsetDeg());

        refreshTargetOverlapApproachProgress();
    }

    /**
     * Clears calibration and any in-progress battle transition, returning to overworld rotation
     * (down in Red/Blue, up in Yellow). Does not change game, route blinds flag, targets, or Yellow modifiers.
     */
    public void clearCalibrationState(final long now) {
        calibratedSlot = null;
        hasCalibratedAtLeastOnce = false;
        uncertaintyWedgeExtentDeltaDeg = 0;
        uncertaintyBattleGrowthDeg = 0;
        battleEnterTime = -1;
        rangeAtBattleStart = null;
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
        calibratedSlot = slot;
        hasCalibratedAtLeastOnce = true;
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

    public boolean targetOverlapsUncertainty() {
        if (calibratedSlot == null) {
            return false;
        }

        final double wedgeExtent = getUncertaintyWedgeExtentDeg();
        final double arrowWheelDeg = 90 + angleDeg;
        final double w1 = norm360(arrowWheelDeg - wedgeExtent / 2);
        final double w2 = norm360(arrowWheelDeg + wedgeExtent / 2);
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
     * Fraction of the target slot arc covered by the calibration wedge (max over selected targets).
     * Meaningful when {@link #targetOverlapsUncertainty()} is true; otherwise 0.
     */
    public double getTargetUncertaintyOverlapPortionOfSlot() {
        return targetUncertaintyOverlapPortionOfSlot;
    }

    private double computeMaxTargetWedgeOverlapPortionOfSlot() {
        final double wedgeExtent = getUncertaintyWedgeExtentDeg();
        final double arrowWheelDeg = 90 + angleDeg;
        final double w1 = norm360(arrowWheelDeg - wedgeExtent / 2);
        final double w2 = norm360(arrowWheelDeg + wedgeExtent / 2);
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

    /**
     * Overworld-only: extra ° added to {@link #getDisplayAngleDeg()} (opposite {@link #update()} overworld motion) so
     * the painted ring shows where the counter sits after step lag. Not used for overlap / approach / suggested DSum.
     * Zero in battle or when lag is 0.
     */
    private double overworldMovementLagOffsetDeg() {
        if (battleEnterTime != -1) {
            return 0.0;
        }
        final int lagFrames = overworldMovementMode.suggestionStepLagFrames();
        if (lagFrames <= 0) {
            return 0.0;
        }
        final double overworldNs = overworldCycleNs();
        final double cycleFrames = Math.abs(overworldNs / ONE_FRAME_NS);
        if (cycleFrames <= 1e-6) {
            return 0.0;
        }
        final double magDeg = (lagFrames / cycleFrames) * 360.0;
        return Math.copySign(magDeg, 360.0 / overworldNs);
    }

    private boolean overlapsAtForwardNanoseconds(final long deltaNs) {
        if (calibratedSlot == null) {
            return false;
        }
        final double overworldNs = overworldCycleNs();
        final double base = angleDeg;
        final double futureArrowWheelDeg = 90 + base + (-360.0 / overworldNs) * deltaNs;
        final double wedgeExtent = getUncertaintyWedgeExtentDeg();
        final double w1 = norm360(futureArrowWheelDeg - wedgeExtent / 2);
        final double w2 = norm360(futureArrowWheelDeg + wedgeExtent / 2);
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

    public double getUncertaintyWedgeExtentDeg() {
        if (calibratedSlot != null) {
            final double base =
                    ((calibratedSlot.max() - calibratedSlot.min() + 1) / (double) DSUM_RANGE) * 360.0;
            return Math.max(
                    1,
                    Math.min(360, base + uncertaintyBattleGrowthDeg + uncertaintyWedgeExtentDeltaDeg));
        }
        if (battleEnterTime == -1 || rangeAtBattleStart == null) {
            return 0;
        }
        final Game g = this.game;
        final double inBattleNs = g == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
        final long elapsed = System.nanoTime() - battleEnterTime;
        final double liveBattleAngleDeg = (elapsed / inBattleNs) * 360.0;
        final EncounterSlot atEncounter = slotContainingDsum(rangeAtBattleStart.second());
        final double encounterSlotDeg =
                ((atEncounter.max() - atEncounter.min() + 1) / (double) DSUM_RANGE) * 360.0;
        final double extra =
                uncertaintyWedgeExtraDegForInBattleAngle(liveBattleAngleDeg);
        return Math.max(1, Math.min(360, encounterSlotDeg + extra));
    }

    /** Total ° to add to slot span from in-battle DSum rotation {@code battleAngleDeg} (linear in 360° turns). */
    private static double uncertaintyWedgeExtraDegForInBattleAngle(final double battleAngleDeg) {
        final double rotations = Math.abs(battleAngleDeg / 360.0);
        return 2.0 * UNCERTAINTY_WEDGE_DEGREES_DELTA_PER_ROTATION * rotations;
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
        final double uncertaintyWedgeHalf = getUncertaintyWedgeExtentDeg() / 2;
        final double usedDeg = angleDeg + angleOffset;
        final int dsum = dsumFromAngle(usedDeg);
        final int min = dsumFromAngle(usedDeg - uncertaintyWedgeHalf);
        final int max = dsumFromAngle(usedDeg + uncertaintyWedgeHalf);

        return new Triplet<>(min, dsum, max);
    }

    /**
     * DSum triple for suggested-slot UI only. Overworld: same basis as overlap / needle ({@link #getDsumRange()} —
     * no step-lag offset). Calibrating: {@link #getDsumRangeAtStartOfBattle()}.
     */
    public Triplet<Integer, Integer, Integer> getDsumRangeForSuggestedSlots() {
        if (isCalibrating()) {
            return getDsumRangeAtStartOfBattle();
        }
        return getDsumRange();
    }

    public Triplet<Integer, Integer, Integer> getDsumRangeAtStartOfBattle() {
        return rangeAtBattleStart;
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

    public EncounterSlot getCalibratedSlot() {
        return calibratedSlot;
    }
}
