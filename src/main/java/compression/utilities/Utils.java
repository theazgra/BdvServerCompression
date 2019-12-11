package compression.utilities;

import compression.U16;

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
//            final byte[] bytes = IOUtils.toByteArray(fileStream);
//            return bytes;
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

    public static int shortBitsToInt(final short value) {
        return ((value & 0xff00) | (value & 0x00ff));
    }

    public static short u16BitsToShort(final int value) {
        return ((short) value);
    }

    public static int[] convertU16ByteArrayToIntArray(final byte[] bytes) {
        assert (bytes.length % 2 == 0);
        int[] values = new int[bytes.length / 2];

        int index = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            final int value = (int) (((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
            values[index++] = value;
        }
        return values;
    }

    public static short[] convertU16ByteArrayToShortArray(final byte[] bytes) {
        assert (bytes.length % 2 == 0);
        short[] values = new short[bytes.length / 2];

        int index = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            final short value = (short) (((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
            values[index++] = value;
        }
        return values;
    }

    public static byte[] convertShortArrayToByteArray(final short[] data) {
        byte[] buffer = new byte[data.length * 2];

        int j = 0;
        for (final short s : data) {
            buffer[j++] = (byte) ((s >> 8) & 0xff);
            buffer[j++] = (byte) (s & 0xff);
        }

        return buffer;
    }

    public static int[] convertShortArrayToIntArray(final short[] src) {
        int[] result = new int[src.length];
        int intValue;
        for (int i = 0; i < src.length; i++) {
            intValue = shortBitsToInt(src[i]);
            if (intValue < U16.Min || intValue > U16.Max) {
                throw new RuntimeException("Source value is outside of bounds for 16-bit unsigned integer.");
            }
            result[i] = intValue;
        }
        return result;
    }

    public static short[] convertIntArrayToShortArray(final int[] src) {
        short[] result = new short[src.length];

        short shortValue;
        for (int i = 0; i < src.length; i++) {
            if (src[i] < 0 || src[i] > U16.Max) {
                throw new RuntimeException("Source value is outside of bounds for 16-bit unsigned integer.");
            }
            result[i] = (short) src[i];
        }
        return result;
    }
}
