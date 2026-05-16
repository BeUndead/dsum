package com.com.dsum.util;

import java.util.Map;
import java.util.stream.IntStream;

public class MathUtilities {

    private static final double ANGLE_TO_DSUM = GameConstants.DSUM_RANGE / (2 * Math.PI);
    private static final double DSUM_TO_ANGLE = 2 * Math.PI / GameConstants.DSUM_RANGE;

    public static int mod(final int a) {
        final int result = a % GameConstants.DSUM_RANGE;
        return result < 0 ? result + GameConstants.DSUM_RANGE : result;
    }

    public static double mod(final double a) {
        final double result = Math.IEEEremainder(a, GameConstants.DSUM_RANGE);
        return result < 0 ? result + GameConstants.DSUM_RANGE : result;
    }

    public static double circularMean(final Map<Integer, Integer> values) {
        return circularMean(values.entrySet().stream().flatMapToInt(
                e -> IntStream.range(0, e.getValue()).map(i -> e.getKey())));
    }


    private static double circularMean(final IntStream values) {
        double sumSin = 0.0;
        double sumCos = 0.0;

        final double[] asAngles = values.mapToDouble(MathUtilities::toAngle).toArray();
        for (final double angle : asAngles) {
            sumSin += Math.sin(angle);
            sumCos += Math.cos(angle);
        }

        final double angularResult = Math.atan2(sumSin, sumCos);
        return angularResult * ANGLE_TO_DSUM;
    }

    private static double toAngle(final int value) {
        return value * DSUM_TO_ANGLE;
    }
}
