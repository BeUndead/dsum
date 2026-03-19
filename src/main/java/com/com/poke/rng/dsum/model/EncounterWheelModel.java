package com.com.poke.rng.dsum.model;

import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;

import java.awt.event.KeyEvent;
import java.util.List;

public class EncounterWheelModel {

    // GameBoy's native frame rate.
    private static final double FRAME_RATE = 59.7275;

    // Red/Blue:
    // Average number of frames for one DSum cycle out of battle (counting down).
    private static final double OVERWORLD_DSUM_CYCLE_FRAMES = 375.04;
    // Average number of frames for one DSum cycle in battle (counting up).
    private static final double IN_BATTLE_DSUM_CYCLE_FRAMES = 783.836735;

    // Number of frames which the in-battle DSum cycle runs before the spiral battle entry animation ends.
    private static final long COUNT_UP_BEFORE_SPIRAL_END_FRAMES = 100;
    // Number of frames which the in-battle DSum cycle runs before the blinds battle entry animation ends.
    private static final long COUNT_UP_BEFORE_BLINDS_END_FRAMES = 48;
    // Number of frames which the in-battle DSum cycle runs after clearing 'Got away safely!'.
    private static final double COUNT_UP_AFTER_GOT_AWAY_FRAMES = 37;

    // Yellow:
    // Average number of frames for one DSum cycle out of battle (counting up - hence minus).
    private static final double YELLOW_OVERWORLD_DSUM_CYCLE_FRAMES = -809.510204;
    // Average number of frames for one DSum cycle in battle (counting up).
    private static final double YELLOW_IN_BATTLE_DSUM_CYCLE_FRAMES = 775.944444;

    // No count frames in yellow, since times are similar and go the same direction...

    // The duration (in ns) of a single frame.
    private static final double ONE_FRAME_NS = 1_000_000_000.0 / FRAME_RATE;

    private static final double OVERWORLD_CYCLE_NS = ONE_FRAME_NS * OVERWORLD_DSUM_CYCLE_FRAMES;
    private static final double YELLOW_OVERWORLD_CYCLE_NS = ONE_FRAME_NS * YELLOW_OVERWORLD_DSUM_CYCLE_FRAMES;
    private static final double IN_BATTLE_CYCLE_NS = ONE_FRAME_NS * IN_BATTLE_DSUM_CYCLE_FRAMES;
    private static final double YELLOW_IN_BATTLE_CYCLE_NS = ONE_FRAME_NS * YELLOW_IN_BATTLE_DSUM_CYCLE_FRAMES;

    private static final long PIKACHU_CRY_FRAMES = 36;

    public static final int DSUM_RANGE = 256;
    public static final double OFFSET_STEP_DEG = 3.0;

    private List<EncounterSlot> targetSlots;

    private boolean isBlinds = false;

    private long overworldStartTime = System.nanoTime();
    private long lastNow = overworldStartTime;
    private double angleDeg = 0.0;
    private double manualAngleOffsetDeltaDeg = 0.0;

    private long battleEnterTime = -1;

    private EncounterSlot calibratedSlot;
    private double uncertaintyWedgeExtentDeltaDeg;

    private boolean warningBeepPending;

    private boolean skipPikaCry = true;

    private Game game;

    public EncounterWheelModel(final EncounterSlot targetSlot, final Game game) {
        this.targetSlots = List.of(targetSlot);
        this.game = game;
    }

    public void setIsBlinds(final boolean isBlinds) {
        this.isBlinds = isBlinds;
    }

    public void setGame(final Game game) {
        this.game = game;
    }

    public void setSkipPikaCry(final boolean skip) {
        this.skipPikaCry = skip;
    }

    private static double angleFromDsum(final int dsum) {
        return (dsum / (double) DSUM_RANGE) * 360.0;
    }

    private static int dsumFromAngle(final double angleDegrees) {
        return (int) ((angleDegrees / 360.0) * DSUM_RANGE) & 0xFF;
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

        if (battleEnterTime != -1) {
            // Running backwards
            final double inBattleNs = game == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
            angleDeg += ((delta / inBattleNs) * 360.0) + manualAngleOffsetDeltaDeg;
            uncertaintyWedgeExtentDeltaDeg = 0;
            lastNow = now;
            return;
        }

        final double overworldNs = game == Game.YELLOW ? YELLOW_OVERWORLD_CYCLE_NS : OVERWORLD_CYCLE_NS;
        angleDeg += -((delta / overworldNs) * 360.0) + manualAngleOffsetDeltaDeg;
        uncertaintyWedgeExtentDeltaDeg += 0.01;
        lastNow = now;
    }

    public void battleStart() {
        final long now = System.nanoTime();
        final Game game = this.game;

        long animationFrames = isBlinds ? COUNT_UP_BEFORE_BLINDS_END_FRAMES : COUNT_UP_BEFORE_SPIRAL_END_FRAMES;
        battleEnterTime = now - (long) (animationFrames * ONE_FRAME_NS);
        // We need to treat the angleDeg as if we've been counting up for that time too
        // Calculate the incorrect angle which has been going down:
        final double overworldNs = game == Game.YELLOW ? YELLOW_OVERWORLD_CYCLE_NS : OVERWORLD_CYCLE_NS;
        final double inBattleNs = game == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;

        final double incorrectDownAngle = ((animationFrames * ONE_FRAME_NS) / overworldNs) * 360.0;
        final double correctUpAngle = ((animationFrames * ONE_FRAME_NS) / inBattleNs) * 360.0;

        final double correction;
        if (game == Game.YELLOW && skipPikaCry) {
            // Yellow pauses DSum incrementing for some time, while Pikachu's cry plays...
            // We're going to handle that by just jumping the appropriate amount at battle start.
            // This shouldn't matter because you can't experience encounters BEFORE Pikachu's cry...
            correction = (PIKACHU_CRY_FRAMES * ONE_FRAME_NS) / inBattleNs * 360.0;
        } else {
            correction = 0.0;
        }

        overworldStartTime = now;
        angleDeg = angleDeg + correction + (correctUpAngle + incorrectDownAngle);
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
        final long timeInBattle = now - battleEnterTime;
        final double inBattleNs = game == Game.YELLOW ? YELLOW_IN_BATTLE_CYCLE_NS : IN_BATTLE_CYCLE_NS;
        final double overwordNs = game == Game.YELLOW ? YELLOW_OVERWORLD_CYCLE_NS : OVERWORLD_CYCLE_NS;
        double angleChangeInBattle = (timeInBattle / inBattleNs) * 360.0;
        // We're going to start reversing immediately, but the game keeps counting up for a few frames after we clear
        // the 'Got away safely!' message.  We account for that here with whatever this formula is...
        // We /would/ have kept counting up for COUNT_UP_AFTER_GOT_AWAY_FRAMES, at the in battle rate.  But for that
        // number of frames instead, we would be counting down.
        // This results in the wheel being wrong for the COUNT_UP_AFTER_GOT_AWAY frames...  But you can't get an
        // encounter in that window anyways - and makes the maths much simpler.
        final double correctUpAngle = ((COUNT_UP_AFTER_GOT_AWAY_FRAMES * ONE_FRAME_NS) / inBattleNs) * 360.0;
        final double incorrectDownAngle = (((COUNT_UP_AFTER_GOT_AWAY_FRAMES - 1) * ONE_FRAME_NS) / overwordNs) * 360.0;

        angleChangeInBattle += (correctUpAngle + incorrectDownAngle);

        final double angleAtBattleStart = angleFromDsum(midDsum);
        final double newAngle = angleAtBattleStart + angleChangeInBattle;

        angleDeg = newAngle + ((now - lastNow) / overwordNs) * 360.0;
        battleEnterTime = -1;
        calibratedSlot = slot;
        uncertaintyWedgeExtentDeltaDeg = 0;
        manualAngleOffsetDeltaDeg = 0;
    }


    public void manualAngle(final boolean positive) {
        manualAngleOffsetDeltaDeg += positive ? OFFSET_STEP_DEG : -OFFSET_STEP_DEG;
    }

    public void uncertaintyDelta(final boolean positive) {
        uncertaintyWedgeExtentDeltaDeg += positive ? OFFSET_STEP_DEG : -OFFSET_STEP_DEG;
    }

    public boolean consumeWarningBeep() {
        final boolean pending = warningBeepPending;
        warningBeepPending = false;
        return pending;
    }

    public boolean isCalibrating() {
        return battleEnterTime != -1;
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

    public double getUncertaintyWedgeExtentDeg() {
        if (calibratedSlot == null) {
            return 0;
        }
        final double base = ((calibratedSlot.max() - calibratedSlot.min() + 1) / (double) DSUM_RANGE) * 360.0;
        return Math.max(1, Math.min(360, base + uncertaintyWedgeExtentDeltaDeg));
    }

    public double getAngleDeg() {
        return angleDeg;
    }

    public int getDsum() {
        return dsumFromAngle(angleDeg);
    }

    public List<EncounterSlot> getTargetSlots() {
        return targetSlots;
    }

    public void setTargetSlots(final List<EncounterSlot> slots) {
        this.targetSlots = slots;
    }

    public EncounterSlot getCalibratedSlot() {
        return calibratedSlot;
    }
}
