package compression;

public class U16 {
    public static final int Min = 0x0;
    public static final int Max = 0xffff;

    public static boolean isInRange(final int value) {
        return ((value >= Min) && (value <= Max));
    }

    public static boolean isInRange(final long value) {
        return ((value >= Min) && (value <= Max));
    }
}
