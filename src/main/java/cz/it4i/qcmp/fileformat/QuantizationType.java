package cz.it4i.qcmp.fileformat;

public enum QuantizationType {
    Scalar(0),
    Vector1D(1),
    Vector2D(2),
    Vector3D(3),
    Invalid(255);

    private final int value;

    QuantizationType(final int enumValue) {
        value = enumValue;
    }

    public int getValue() {
        return value;
    }

    public static QuantizationType fromByte(final byte b) {
        if (b == 0)
            return Scalar;
        if (b == 1)
            return Vector1D;
        if (b == 2)
            return Vector2D;
        if (b == 3)
            return Vector3D;
        else
            return Invalid;
    }

    public boolean isOneOf(final QuantizationType... types) {
        for (final QuantizationType type : types) {
            if (type == this) {
                return true;
            }
        }
        return false;
    }
}
