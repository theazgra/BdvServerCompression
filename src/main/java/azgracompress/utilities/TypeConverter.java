package azgracompress.utilities;

import azgracompress.U16;

public class TypeConverter {

    public static int shortToInt(final short value) {
        return ((value & 0xFF00) | (value & 0x00FF));
    }

    public static int[] unsignedShortBytesToIntArray(final byte[] bytes) {
        assert (bytes.length % 2 == 0);
        int[] values = new int[bytes.length / 2];

        int index = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            final int value = (int) (((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
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

        for (int i = 0; i < src.length; i++) {
            if (src[i] < 0 || src[i] > U16.Max) {
                throw new RuntimeException("Source value is outside of bounds for 16-bit unsigned integer.");
            }
            result[i] = (short) src[i];
        }
        return result;
    }

    public static byte[] unsignedShortArrayToByteArray(final int[] data, final boolean littleEndian) {
        byte[] buffer = new byte[data.length * 2];

        int j = 0;
        for (final int v : data) {
            if (littleEndian) {
                buffer[j++] = (byte) (v & 0xFF);
                buffer[j++] = (byte) ((v >> 8) & 0xFF);
            } else {
                buffer[j++] = (byte) ((v >> 8) & 0xFF);
                buffer[j++] = (byte) (v & 0xFF);
            }
        }
        return buffer;
    }

    public static byte[] intArrayToByteArray(final int[] data, final boolean littleEndian) {
        byte[] buffer = new byte[data.length * 4];

        int j = 0;
        for (final int v : data) {
            if (littleEndian) {
                buffer[j++] = (byte) (v & 0xFF);
                buffer[j++] = (byte) ((v >>> 8) & 0xFF);
                buffer[j++] = (byte) ((v >>> 16) & 0xFF);
                buffer[j++] = (byte) ((v >>> 24) & 0xFF);
            } else {
                buffer[j++] = (byte) ((v >>> 24) & 0xFF);
                buffer[j++] = (byte) ((v >>> 16) & 0xFF);
                buffer[j++] = (byte) ((v >>> 8) & 0xFF);
                buffer[j++] = (byte) (v & 0xFF);
            }
        }
        return buffer;
    }

    /**
     * Convert unsigned short bytes to integers. Place integers into values array from offset position.
     *
     * @param bytes  Unsigned short bytes.
     * @param values Integer value buffer.
     * @param offset Offset into integer array.
     */
    public static void unsignedShortBytesToIntArray(final byte[] bytes, final int[] values, final int offset) {
        assert (bytes.length % 2 == 0);
        int valuesIndex = offset;
        for (int i = 0; i < bytes.length; i += 2) {
            final int value = (int) (((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
            values[valuesIndex++] = value;
        }
    }
}
