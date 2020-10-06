package cz.it4i.qcmp.utilities;

import java.util.ArrayList;

public class Means {
    public static double arithmeticMean(final ArrayList<Double> values) {
        double sum = 0.0;
        for (final double val : values) {
            sum += val;
        }

        final double result = sum / (double) values.size();
        return result;
    }

    public static double lehmerMean(final ArrayList<Double> values) {
        double numerator = 0.0;
        double denominator = 0.0;

        for (final double val : values) {
            numerator += Math.pow(val, 2);
            denominator += val;
        }

        final double result = numerator / denominator;
        return result;
    }

    public static double weightedLehmerMean(final ArrayList<Double> values,
                                            final double[] weights) {
        assert (values.size() == weights.length);

        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < values.size(); i++) {
            numerator += (weights[i] * Math.pow(values.get(i), 2));
            denominator += (weights[i] * values.get(i));
        }

        final double result = numerator / denominator;
        return result;
    }
}
