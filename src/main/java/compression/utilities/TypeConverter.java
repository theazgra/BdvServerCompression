package compression.utilities;

import compression.U16;

public class TypeConverter {

    public static int shortToInt(final short value) {
        return ((value & 0xFF00) | (value & 0x00FF));
    }

    public static short intToShort(final int value) {
        return ((short) value);
    }

    public static int[] shortBytesToIntArray(final byte[] bytes) {
        assert (bytes.length % 2 == 0);
        int[] values = new int[bytes.length / 2];

        int index = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            final int value = (int) (((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
            values[index++] = value;
        }
        return values;
    }

    public static short[] shortBytesToShortArray(final byte[] bytes) {
        assert (bytes.length % 2 == 0);
        short[] values = new short[bytes.length / 2];

        int index = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            final short value = (short) (((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
            values[index++] = value;
        }
        return values;
    }


    public static int[] shortArrayToIntArray(final short[] src) {
        int[] result = new int[src.length];
        int intValue;
        for (int i = 0; i < src.length; i++) {
            intValue = shortToInt(src[i]);
            if (intValue < U16.Min || intValue > U16.Max) {
                throw new RuntimeException("Source value is outside of bounds for 16-bit unsigned integer.");
            }
            result[i] = intValue;
        }
        return result;
    }

    public static short[] intArrayToShortArray(final int[] src) {
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

    public static byte[] shortArrayToByteArray(final short[] data) {
        byte[] buffer = new byte[data.length * 2];
        int j = 0;
        for (final short s : data) {
            // NOTE(Moravec): Use little endian.
            buffer[j++] = (byte) (s & 0xff);
            buffer[j++] = (byte) ((s >> 8) & 0xff);
        }
        return buffer;
    }

    public static byte[] intArrayToByteArray(final int[] data) {
        byte[] buffer = new byte[data.length * 4];

        int j = 0;
        for (final int v : data) {
            // NOTE(Moravec): Use little endian.
            buffer[j++] = (byte) (v & 0xFF);
            buffer[j++] = (byte) ((v >>> 8) & 0xFF);
            buffer[j++] = (byte) ((v >>> 16) & 0xFF);
            buffer[j++] = (byte) ((v >>> 24) & 0xFF);
        }
        return buffer;
    }

}
