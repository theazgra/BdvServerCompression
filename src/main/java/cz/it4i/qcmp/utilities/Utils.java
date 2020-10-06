package cz.it4i.qcmp.utilities;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.data.Range;

import java.util.ArrayList;


public class Utils {

    public static Range<Integer> singlePlaneRange(final int index) {
        return new Range<>(index, index + 1);
    }

    public static double calculatePsnr(final double mse, final int signalMax) {
        final double psnr = 10.0 * Math.log10((Math.pow(signalMax, 2) / mse));
        return psnr;
    }

    public static <T> boolean arrayContains(final T[] array, final T element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(element))
                return true;
        }
        return false;
    }

    public static double arrayListMax(final ArrayList<Double> array) {
        double max = Double.MIN_VALUE;
        for (final double val : array) {
            if (val > max) {
                max = val;
            }
        }
        return max;
    }

    public static boolean arrayContainsToIndex(final int[] array, final int toIndex, final int element) {
        for (int i = 0; i < toIndex; i++) {
            if (array[i] == element)
                return true;
        }
        return false;
    }


    public static double arrayListSum(final ArrayList<Double> array) {
        double sum = 0.0;
        for (final double val : array) {
            sum += val;
        }
        return sum;
    }


    public static int[] getDifference(final int[] original, final int[] transformed) {
        assert (original.length == transformed.length) : "Array lengths doesn't match";

        final int[] difference = new int[original.length];
        for (int i = 0; i < original.length; i++) {
            difference[i] = (original[i] - transformed[i]);
        }
        return difference;
    }

    public static int[] asAbsoluteValues(final int[] values) {
        final int[] absValues = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            absValues[i] = Math.abs(values[i]);
        }
        return absValues;
    }

    public static void applyAbsFunction(final int[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.abs(values[i]);
        }
    }


    public static MinMaxResult<Integer> getMinAndMax(final int[] data) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < data.length; i++) {
            if (data[i] < min) {
                min = data[i];
            }
            if (data[i] > max) {
                max = data[i];
            }
        }
        return new MinMaxResult<Integer>(min, max);
    }

    /**
     * Calculate individual pixel values frequencies.
     * Same result as of histogram of bin width = 1
     *
     * @param data Pixel data.
     * @return Pixel value frequencies.
     */
    public static double[] calculatePixelFrequencies(final int[] data) {
        final double[] frequencies = new double[U16.Max + 1];
        for (final int pixelValue : data) {
            ++frequencies[pixelValue];
        }
        return frequencies;
    }

    public static double log2(final double v) {
        return (Math.log(v) / Math.log(2));
    }

    public static double calculateEntropy(final int[] pixelData) {
        final double pixelCount = pixelData.length;
        final double[] pixelFrequencies = Utils.calculatePixelFrequencies(pixelData);
        final double[] pixelProbabilities = new double[pixelFrequencies.length];

        for (int i = 0; i < pixelFrequencies.length; i++) {
            pixelProbabilities[i] = pixelFrequencies[i] / pixelCount;
        }

        double entropy = 0.0;

        for (final double pixelProbability : pixelProbabilities) {
            if (pixelProbability > 0.0) {
                entropy += pixelProbability * log2(pixelProbability);
            }
        }
        return (-entropy);
    }

    public static double calculateMse(final int[] difference) {
        double sum = 0.0;
        for (final int val : difference) {
            sum += Math.pow(val, 2);
        }
        final double mse = (sum / (double) difference.length);
        return mse;
    }

    /**
     * Calculate the euclidean distance between two vectors.
     *
     * @param v1 First vector.
     * @param v2 Second vector.
     * @return Euclidean distance.
     */
    public static double calculateEuclideanDistance(final int[] v1, final int[] v2) {
        assert (v1.length == v2.length);
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(((double) v1[i] - (double) v2[i]), 2);
        }
        return Math.sqrt(sum);
    }
}