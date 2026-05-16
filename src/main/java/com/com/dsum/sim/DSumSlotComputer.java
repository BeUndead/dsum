package com.com.dsum.sim;

import com.com.dsum.model.EncounterSlot;
import com.com.dsum.model.Route;
import com.com.dsum.model.SelectionsConfig;
import com.com.dsum.util.DSumUtilities;
import com.com.dsum.util.GameConstants;

import java.util.Arrays;
import java.util.Map;

public class DSumSlotComputer {

    private final SelectionsConfig config;
    private final double[][] slots;

    private volatile int encounterRate;
    private volatile int slotsEpoch;

    public DSumSlotComputer(final SelectionsConfig config) {
        this.config = config;
        this.config.registerRouteChangeListener(this::recomputeIfNeeded);
        this.slots = new double[256][10];
        this.encounterRate = this.config.getRoute().getEncounterRate();

        recomputeSlots();
    }

    public double[] getSlotProbability(final int dsum) {
        synchronized (slots) {
            return slots[dsum];
        }
    }

    public int getSlotsEpoch() {
        return slotsEpoch;
    }

    /**
     * Copies the full probability table and returns the {@link #slotsEpoch} value for that snapshot (same critical section).
     */
    public int snapshotSlotsInto(final double[][] dest) {
        if (dest.length != GameConstants.DSUM_RANGE || dest[0].length != EncounterSlot.values().length) {
            throw new IllegalArgumentException("dest must be [" + GameConstants.DSUM_RANGE + "][10]");
        }
        synchronized (slots) {
            for (int i = 0; i < GameConstants.DSUM_RANGE; i++) {
                System.arraycopy(slots[i], 0, dest[i], 0, EncounterSlot.values().length);
            }
            return slotsEpoch;
        }
    }

    private void recomputeIfNeeded(final Route route) {
        final int oldEncounterRate = this.encounterRate;
        this.encounterRate = route.getEncounterRate();

        if (oldEncounterRate != this.encounterRate) {
            // We only need to recompute slots if the encounter rate for the selected route has changed.
            recomputeSlots();
        }
    }

    private void recomputeSlots() {
        synchronized (slots) {
            // Clear the slots first
            for (int i = 0; i < slots.length; i++) {
                Arrays.fill(slots[i], 0.0d);
            }

            // Then recompute the values
            for (int dsum = 0; dsum < GameConstants.DSUM_RANGE; dsum++) {
                final Map<EncounterSlot, Integer> map = DSumUtilities.getSuggestionRange(dsum, encounterRate);
                final int sum = map.values().stream()
                        .mapToInt(i -> i)
                        .sum();
                for (final EncounterSlot slot : EncounterSlot.values()) {
                    final Integer frequency = map.get(slot);
                    if (frequency != null) {
                        slots[dsum][slot.ordinal()] = frequency / (double) sum;
                    }
                }
            }
            this.slotsEpoch++;
        }
    }
}
