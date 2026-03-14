package com.com.poke.rng.dsum.model;

import com.com.poke.rng.dsum.constants.EncounterSlot;

import java.awt.event.KeyEvent;

public class EncounterWheelModel {

    public static final double CYCLE_NS = 6_200_900_000.0;
    public static final int DSUM_RANGE = 256;
    public static final int LEAD_DSUM = 64;
    public static final double WIPE_ANIMATION_TIME = 1.23;
    public static final long WIPE_ANIMATION_TIME_NS = (long) (WIPE_ANIMATION_TIME * 1_000_000_000);
    public static final double WIPE_ROTATION_COMPENSATION_DEG =
            (WIPE_ANIMATION_TIME_NS / (CYCLE_NS * 2)) * 360.0;
    public static final double OFFSET_STEP_DEG = 3.0;

    private EncounterSlot targetSlot;
    private int[] warningPoints;

    private double angleDeg;
    private double angleOffset;
    private double manualAngleOffsetDeltaDeg;
    private int lastDSum;
    private long spacePressedNanos;
    private EncounterSlot calibratedSlot;
    private double uncertaintyWedgeExtentDeltaDeg;

    private boolean warningBeepPending;

    public EncounterWheelModel(final EncounterSlot targetSlot) {
        this.targetSlot = targetSlot;
        this.warningPoints = computeWarningPoints(targetSlot);
        this.lastDSum = -1;
        this.spacePressedNanos = -1;
    }

    private static double angleFromDsum(final int dsum) {
        return (dsum / (double) DSUM_RANGE) * 360.0;
    }

    private static int dsumFromAngle(final double angleDegrees) {
        return (int) ((angleDegrees / 360.0) * DSUM_RANGE) & 0xFF;
    }

    private static int[] computeWarningPoints(final EncounterSlot target) {
        int enterAt = target.max();
        return new int[]{
                (enterAt + LEAD_DSUM) & 0xFF,
                (enterAt + LEAD_DSUM * 2 / 3) & 0xFF,
                (enterAt + LEAD_DSUM / 3) & 0xFF
        };
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

    private static boolean crossedBoundary(final int prev, final int current, final int target) {
        if (prev == -1) {
            return false;
        }
        if (prev >= current) {
            return target < prev && target >= current;
        }
        return target < prev || target >= current;
    }

    public void update(final long now) {
        if (spacePressedNanos != -1) {
            final long timeSinceSpace = now - spacePressedNanos;
            final double elapsedAtSpace = (spacePressedNanos % CYCLE_NS) / CYCLE_NS;
            final double angleAtSpace = -(elapsedAtSpace * 360.0) + angleOffset + manualAngleOffsetDeltaDeg;
            final double angleChangeSinceSpace = (timeSinceSpace / (CYCLE_NS * 2.0)) * 360.0;
            angleDeg = angleAtSpace + angleChangeSinceSpace;
            uncertaintyWedgeExtentDeltaDeg = 0;
            return;
        }

        final double elapsed = (now % CYCLE_NS);
        angleDeg = -(elapsed / CYCLE_NS) * 360.0 + angleOffset + manualAngleOffsetDeltaDeg;
        uncertaintyWedgeExtentDeltaDeg += 0.01;

        final int dsum = getDsum();
        for (int warningPoint : warningPoints) {
            if (crossedBoundary(lastDSum, dsum, warningPoint)) {
                warningBeepPending = true;
                break;
            }
        }
        lastDSum = dsum;
    }

    public void handleKeyPress(final KeyEvent e, final long now) {
        if (e.getKeyCode() == KeyEvent.VK_OPEN_BRACKET) {
            manualAngleOffsetDeltaDeg -= OFFSET_STEP_DEG;
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET) {
            manualAngleOffsetDeltaDeg += OFFSET_STEP_DEG;
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_MINUS) {
            uncertaintyWedgeExtentDeltaDeg -= OFFSET_STEP_DEG;
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_EQUALS) {
            uncertaintyWedgeExtentDeltaDeg += OFFSET_STEP_DEG;
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (spacePressedNanos != -1) {
                double elapsed = (now % CYCLE_NS);
                angleOffset = angleDeg + (elapsed / CYCLE_NS) * 360.0;
                spacePressedNanos = -1;
                return;
            }
            spacePressedNanos = now - WIPE_ANIMATION_TIME_NS;
            return;
        }

        if (spacePressedNanos == -1) {
            return;
        }

        if (e.getKeyChar() < '0' || e.getKeyChar() > '9') {
            return;
        }

        int slotIndex = e.getKeyChar() == '0' ? 9 : (e.getKeyChar() - '1');
        EncounterSlot[] slots = EncounterSlot.values();
        if (slotIndex < 0 || slotIndex >= slots.length) {
            return;
        }

        final EncounterSlot slot = slots[slotIndex];
        final int midDsum = (slot.min() + slot.max()) / 2;
        final long timeSinceSpace = now - spacePressedNanos;
        final double angleChangeSinceSpace = (timeSinceSpace / (CYCLE_NS * 2.0)) * 360.0;

        final double angleAtSpace = angleFromDsum(midDsum);
        final double newAngle = angleAtSpace + angleChangeSinceSpace + WIPE_ROTATION_COMPENSATION_DEG;

        final double elapsed = (now % CYCLE_NS);
        angleOffset = newAngle + (elapsed / CYCLE_NS) * 360.0;
        angleDeg = newAngle;
        spacePressedNanos = -1;
        calibratedSlot = slot;
        uncertaintyWedgeExtentDeltaDeg = 0;
        manualAngleOffsetDeltaDeg = 0;
    }

    public boolean consumeWarningBeep() {
        final boolean pending = warningBeepPending;
        warningBeepPending = false;
        return pending;
    }

    public boolean isCalibrating() {
        return spacePressedNanos != -1;
    }

    public boolean targetOverlapsUncertainty() {
        if (calibratedSlot == null) {
            return false;
        }

        final double wedgeExtent = getUncertaintyWedgeExtentDeg();
        final double arrowWheelDeg = 90 + angleDeg;
        final double w1 = norm360(arrowWheelDeg - wedgeExtent / 2);
        final double w2 = norm360(arrowWheelDeg + wedgeExtent / 2);
        final double t1 = norm360((targetSlot.min() / (double) DSUM_RANGE) * 360 + 90);
        final double t2 = norm360(((targetSlot.max() + 1) / (double) DSUM_RANGE) * 360 + 90);

        return angularRangesOverlap(w1, w2, t1, t2);
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

    public EncounterSlot getTargetSlot() {
        return targetSlot;
    }

    public void setTargetSlot(final EncounterSlot slot) {
        this.targetSlot = slot;
        this.warningPoints = computeWarningPoints(slot);
    }

    public EncounterSlot getCalibratedSlot() {
        return calibratedSlot;
    }
}
