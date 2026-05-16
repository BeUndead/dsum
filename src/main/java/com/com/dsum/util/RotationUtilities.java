package com.com.dsum.util;

import com.com.dsum.model.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.com.dsum.util.GameConstants.*;

public final class RotationUtilities {

    public static BattleEntry onBattleEntry(final Game game, final Route route, final int dsum) {

        // We assume the battle entry is the standard one.
        // There is logic when calibrating (as at that point we know the encounter they got) to account for the
        // alternate animations (vertical blinds / full spiral) if their lead level is 3 or more less than the
        // encountered poke.
        final int animationFrames = route.isBlinds()
                ? HORIZONTAL_BLINDS_FRAMES : SPLIT_SPIRAL_FRAMES;

        final int inBattleFrames = animationFrames - BATTLE_ENTRY_OVERWORLD_FRAMES;
        final int sign = game == Game.YELLOW ? 1 : -1;

        // The simulation has been going in OVERWORLD mode, for 'animationFrames' extra frames.
        // The DSum value at the time the battle was entered (animation frame 0), was the current dsum,
        // minus the amount of dsum delta experienced by staying in OVERWORLD mode.
        final double simDeltaFromOverworld = ((sign * animationFrames) / OVERWORLD_CYCLE_FRAMES) * DSUM_RANGE;
        final double dsumAtBattleEntry = dsum - simDeltaFromOverworld;

        // In the actual game, the dsum will advance for BATTLE_ENTRY_OVERWORLD_FRAMES at the overworld rate, and
        // then swap to IN_BATTLE rate for the remainder of the animation.
        // We calculate these separately.
        // The /in-game/ dsum value now will be:
        // dsumAtBattleEntry + deltaFromOverworld + deltaFromBattle.
        final double deltaFromOverworld = ((sign * BATTLE_ENTRY_OVERWORLD_FRAMES) / OVERWORLD_CYCLE_FRAMES) * DSUM_RANGE;
        final double deltaFromBattle = (inBattleFrames / IN_BATTLE_CYCLE_FRAMES) * DSUM_RANGE;

        final double dsumNow = dsumAtBattleEntry + deltaFromOverworld + deltaFromBattle;

        return new BattleEntry(
                MathUtilities.mod((int) dsumAtBattleEntry),
                MathUtilities.mod((int) dsumNow));
    }

    public static Map<Integer, Integer> applyOffsetToKeys(final Map<Integer, Integer> dsumRange, final double offset) {
        if (offset == 0) {
            return dsumRange;
        }
        final LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();
        for (final Map.Entry<Integer, Integer> entry : dsumRange.entrySet()) {
            result.put(MathUtilities.mod((int) (entry.getKey() + offset)), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    public static BattleExit onBattleExit(final Game game, final Route route, final int leadLevel, final EncounterSlot calibrated,
                                   final EncounterExitStrategy exit, final double dsum) {
        final Encounter encounter = route.encounterFor(game, calibrated);
        final int assumedEntryFrames = route.isBlinds() ? HORIZONTAL_BLINDS_FRAMES : SPLIT_SPIRAL_FRAMES;
        final int actualEntryFrames;
        if (leadLevel <= encounter.level() - 3) {
            actualEntryFrames = route.isBlinds() ? VERTICAL_BLINDS_FRAMES : FULL_SPIRAL_FRAMES;
        } else {
            actualEntryFrames = assumedEntryFrames;
        }
        final int entryFrameDelta = actualEntryFrames - assumedEntryFrames;
        // When the battle started, this means we would have been moving for 'frameDelta' extra frames before space
        // was clicked.
        // This would have been running at the IN_BATTLE rate, so we do the conversion to a DSum diff:
        final double entryDSumDelta = (entryFrameDelta / IN_BATTLE_CYCLE_FRAMES) * DSUM_RANGE;

        final Map<Integer, Integer> calibrationForSlot = DSumUtilities.calibrationRangeForSlot(calibrated, route);

        final int inBattleExitFrames = exit.getInBattleFrames();

        // In the sim, we're going to NOW update the dsum to a new value.
        // Since the hardware will be running at the IN_BATTLE cycle rate for 'inBattleExitFrames',
        // we need to offset this for us now running at OVERWORLD cycle rate.
        // To do this, we're also going to add the offset for running the in battle frames at overworld rate,
        // just in reverse.
        // Let's say overworld rate is x per frame, and in battle is y per frame, and our current DSum is 0.
        // The hardware would run at IN_BATTLE rate for inBattleExitFrames,
        // So in 'inBattleExitFrames' frames, we'd be at DSum = y * inBattleExitFrames.
        // At this point, it will return to OVERWORLD rate.
        // For the sim, we will be running at OVERWORLD rate for those 'inBattleExitFrames'.
        // So to end up at the same value, we need y * inBattleExitFrames = x * inBattleExitFrames + c.
        // In this case, we can see that c = (y - x) * inBattleExitFrames.
        // Here we have y = rateForBattleCycle.
        // And then c = simDeltaFromBattleExit.

        final double rateForBattleCycle = 1 / IN_BATTLE_CYCLE_FRAMES * DSUM_RANGE;
        final int sign = game == Game.YELLOW ? 1 : -1;
        final double simDeltaFromBattleExit = (inBattleExitFrames - 1) * (rateForBattleCycle - (sign / OVERWORLD_CYCLE_FRAMES * DSUM_RANGE));

        // The 'mean' needs to be calculated noting that DSum is cyclical.
        // So, for example, the mean of '255, 0, 1] should be '0', not 103 (because 255 is acting effectively
        // as '-1' mod 256.
        // To account for this, we can use the 'circularMean' method.  This will convert all of them to angles, average
        // the angles, and then convert back to the expected DSum.
        final double mean = MathUtilities.circularMean(calibrationForSlot);

        // The new DSum is calculated using the mean of the calibrated slot, as well as:
        // entryDSumDelta (to account for if we'd used the wrong assumed battle entry animation, now that we know the
        // encounter they got)
        // simDeltaFromBattleExit (as above) to account for us changing to overworld state BEFORE the hardware will.
        final double newDSum = MathUtilities.mod(mean + entryDSumDelta + simDeltaFromBattleExit);

        return new BattleExit(newDSum, calibrationForSlot, entryDSumDelta, mean);
    }
}
