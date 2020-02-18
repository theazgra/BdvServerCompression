package azgracompress.utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


public class Utils {

    public static double calculatePsnr(final double mse, final int signalMax) {
        double psnr = 10.0 * Math.log10((Math.pow(signalMax, 2) / mse));
        return psnr;
    }

    public static byte[] readFileBytes(final String path) throws FileNotFoundException {
        FileInputStream fileStream = new FileInputStream(path);
        try {
            return fileStream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
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

        int[] difference = new int[original.length];
        for (int i = 0; i < original.length; i++) {
            difference[i] = (original[i] - transformed[i]);
        }
        return difference;
    }

    public static int[] asAbsoluteValues(int[] values) {
        int[] absValues = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            absValues[i] = Math.abs(values[i]);
        }
        return absValues;
    }

    public static void applyAbsFunction(int[] values) {
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


    public static double calculateMse(final int[] difference) {
        double sum = 0.0;
        for (final int val : difference) {
            sum += Math.pow(val, 2);
        }
        final double mse = (sum / (double) difference.length);
        return mse;
    }
}