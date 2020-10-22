package cz.it4i.qcmp.data;

/**
 * Template for vector with two values.
 *
 * @param <T> Vector types.
 */
public class V2<T> {
    /**
     * First vector value.
     */
    private final T x;

    /**
     * Second vector value.
     */
    private final T y;

    /**
     * Construct vector with two values.
     *
     * @param x First value.
     * @param y Second value.
     */
    public V2(final T x, final T y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Construct two value vector with single number for both values.
     *
     * @param universalValue Value.
     */
    public V2(final T universalValue) {
        this(universalValue, universalValue);
    }

    /**
     * Get X component (first value) from this vector.
     *
     * @return X component value.
     */
    public final T getX() {
        return x;
    }

    /**
     * Get Y component (second value) from this vector.
     *
     * @return Y component value.
     */
    public final T getY() {
        return y;
    }

    /**
     * Default pretty print.
     *
     * @return Pretty form for this vector.
     */
    @Override
    public String toString() {
        return '[' + x.toString() + ";" + y.toString() + ']';
    }

    /**
     * If other object is also V2, then check component equality.
     *
     * @param obj Other object.
     * @return True if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof V2<?>) {
            final V2<?> other = (V2<?>) obj;
            return (x.equals(other.x) && y.equals(other.y));
        } else {
            return super.equals(obj);
        }
    }
}
