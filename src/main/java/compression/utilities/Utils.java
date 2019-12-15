package compression.utilities;

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


    public static int[] getAbsoluteDifference(final short[] original, final short[] transformed) {
        assert (original.length == transformed.length) : "Array lengths doesn't match";

        int[] difference = new int[original.length];
        for (int i = 0; i < original.length; i++) {
            difference[i] = Math.abs( TypeConverter.shortToInt(original[i]) - TypeConverter.shortToInt(transformed[i]));
        }
        return difference;
    }

    public static int[] getSquaredDifference(final short[] original, final short[] transformed) {
        assert (original.length == transformed.length) : "Array lengths doesn't match";

        int[] difference = new int[original.length];
        for (int i = 0; i < original.length; i++) {
            difference[i] = (int) Math.pow((TypeConverter.shortToInt(original[i]) - TypeConverter.shortToInt(transformed[i])), 2);
        }
        return difference;
    }


}