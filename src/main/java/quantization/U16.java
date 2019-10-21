package quantization;

public class U16 extends Number implements Comparable<U16> {

    public static final int MinValue = 0;
    public static final int MaxValue = 65535;

    public static final U16 MinU16 = new U16(0);
    public static final U16 MaxU16 = new U16(65535);

    private int m_value = 0;

    private void assertValueInBounds() throws RuntimeException {
        if (m_value < MinValue || m_value > MaxValue) {
            throw new RuntimeException("U16 number outside of Min/Max bounds.");
        }
    }

    public U16(int value) {
        m_value = value;
        assertValueInBounds();
    }

    public U16 add(final U16 other) {
        m_value += other.m_value;
        assertValueInBounds();
        return this;
    }

    public U16 sub(final U16 other) {
        m_value -= other.m_value;
        assertValueInBounds();
        return this;
    }

    public U16 mul(final U16 other) {
        m_value *= other.m_value;
        assertValueInBounds();
        return this;
    }

    public void set(int value)
    {
        m_value = value;
        assertValueInBounds();
    }

    public float mul(final float multiplier) {
        return (multiplier * (float) m_value);
    }

    public double mul(final double multiplier) {
        return (multiplier * (double) m_value);
    }

    public int mod(final U16 modulo) {
        return (m_value % modulo.m_value);
    }

    @Override
    public int intValue() {
        return m_value;
    }

    public int value() {
        return m_value;
    }

    @Override
    public long longValue() {
        return m_value;
    }

    @Override
    public float floatValue() {
        return m_value;
    }

    @Override
    public double doubleValue() {
        return m_value;
    }

    @Override
    public int hashCode() {
        return m_value;
    }

    @Override
    public int compareTo(U16 other) {
        return m_value - other.m_value;
    }
}
