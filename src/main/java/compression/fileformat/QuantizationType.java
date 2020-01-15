package compression.fileformat;

public enum QuantizationType {
    Scalar(0), Vector1D(1), Vector2D(2);

    private final int value;

    QuantizationType(int enumValue) {
        value = enumValue;
    }

    public int getValue() {
        return value;
    }
}
