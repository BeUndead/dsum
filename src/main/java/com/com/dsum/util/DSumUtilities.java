package com.com.dsum.util;

import com.com.dsum.model.EncounterSlot;
import com.com.dsum.model.Game;
import com.com.dsum.model.Route;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.com.dsum.util.GameConstants.*;

public class DSumUtilities {

    public static Map</* DSum */ Integer, /* Frequency */ Integer> calibrationRangeForSlot(final EncounterSlot slot, final Route route) {
        final LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();

        for (int dsum = slot.min(); dsum < slot.max() + route.getEncounterRate(); dsum++) {
            result.put(MathUtilities.mod(dsum),
                    Math.max(0, Math.min(route.getEncounterRate() - 1, dsum - slot.min()) - Math.max(0, dsum - slot.max()) + 1));
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<EncounterSlot, /* Frequency */ Integer> getSuggestionRange(final int dsum, final Route route) {
        return getSuggestionRange(dsum, route.getEncounterRate());
    }

    public static Map<EncounterSlot, /* Frequency */ Integer> getSuggestionRange(final int dsum, final int encounterRate) {
        final LinkedHashMap<EncounterSlot, Integer> result = new LinkedHashMap<>();
        final int hRandomAddMin = dsum - encounterRate + 1;
        for (int hRandomAdd = hRandomAddMin; hRandomAdd <= dsum; hRandomAdd++) {
            final EncounterSlot slot = EncounterSlot.getSlot(MathUtilities.mod(hRandomAdd));
            if (slot != null) {
                result.compute(slot, (k, v) -> v == null ? 1 : v + 1);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<EncounterSlot, /* Frequency */ Integer> getSuggestionRange(final Map<Integer, Integer> suggested, final Route route) {
        final LinkedHashMap<EncounterSlot, Integer> result = new LinkedHashMap<>();
        for (final Map.Entry<Integer, Integer> dsumEntry : suggested.entrySet()) {
            final Map<EncounterSlot, Integer> dsumResult = getSuggestionRange(dsumEntry.getKey(), route);
            final int dsumFrequency = dsumEntry.getValue();
            for (final Map.Entry<EncounterSlot, Integer> entry : dsumResult.entrySet()) {
                final int weightedFrequency = entry.getValue() * dsumFrequency;
                result.compute(entry.getKey(), (k, v) -> v == null ? weightedFrequency : v + weightedFrequency);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<Integer, Integer> overlapOrUseNewRange(final Map<Integer, Integer> previous, final Map<Integer, Integer> next) {
        if (next == null || next.isEmpty()) {
            return Collections.emptyMap();
        }
        if (previous == null || previous.isEmpty()) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(next));
        }

        final Map<Integer, Integer> previousCopy = new LinkedHashMap<>(previous);
        final Map<Integer, Integer> nextCopy = new LinkedHashMap<>(next);
        previousCopy.keySet().retainAll(nextCopy.keySet());
        nextCopy.keySet().retainAll(previousCopy.keySet());
        previousCopy.keySet().retainAll(nextCopy.keySet());

        final LinkedHashMap<Integer, Integer> overlap = new LinkedHashMap<>();
        int gcd = 0;
        for (final Map.Entry<Integer, Integer> entry : previousCopy.entrySet()) {
            final Integer nextFrequency = nextCopy.get(entry.getKey());
            if (nextFrequency == null) {
                continue;
            }
            final int weightedFrequency = entry.getValue() + nextFrequency;
            if (weightedFrequency <= 0) {
                continue;
            }
            overlap.put(entry.getKey(), weightedFrequency);
            gcd = gcd == 0 ? weightedFrequency : gcd(gcd, weightedFrequency);
        }

        if (overlap.isEmpty()) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(next));
        }

        if (gcd <= 1) {
            return Collections.unmodifiableMap(overlap);
        }

        final LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();
        for (final Map.Entry<Integer, Integer> entry : overlap.entrySet()) {
            final long reduced = entry.getValue() / gcd;
            result.put(entry.getKey(), (int) Math.max(1, reduced));
        }
        return Collections.unmodifiableMap(result);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            final int t = b;
            b = a % b;
            a = t;
        }
        return Math.abs(a);
    }

    public static double overworldDelta(final long nanos, final Game game) {
        final int sign = game == Game.YELLOW ? 1 : -1;
        final double frames = nanos / ONE_FRAME_NS;
        return frames / OVERWORLD_CYCLE_FRAMES * DSUM_RANGE * sign;
    }

    public static double inBattleDelta(final long nanos) {
        final double frames = nanos / ONE_FRAME_NS;
        return frames / IN_BATTLE_CYCLE_FRAMES * DSUM_RANGE;
    }

    public static double circularMinDiff(final double a, final double b) {
        double diff = a - b;
        if (diff < 0) {
            diff += DSUM_RANGE;
        }
        if (diff > DSUM_RANGE / 2) {
            diff = DSUM_RANGE - diff;
        }
        return diff;
    }
}
