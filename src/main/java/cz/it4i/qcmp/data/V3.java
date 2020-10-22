package cz.it4i.qcmp.data;

/**
 * Template for vector with three values.
 *
 * @param <T> Vector types.
 */
public class V3<T> extends V2<T> {
    /**
     * Third vector value.
     */
    private final T z;

    /**
     * Construct vector with three values.
     *
     * @param x First value.
     * @param y Second value.
     * @param z Third value.
     */
    public V3(final T x, final T y, final T z) {
        super(x, y);
        this.z = z;
    }

    /**
     * Get Z component (third value) from this vector.
     *
     * @return Z component value.
     */
    public final T getZ() {
        return z;
    }

    /**
     * Default pretty print.
     *
     * @return Pretty form for this vector.
     */
    @Override
    public String toString() {
        return '[' + getX().toString() + ";" + getY().toString() + ";" + z.toString() + ']';
    }

    /**
     * If other object is also V2, then check component equality.
     *
     * @param obj Other object.
     * @return True if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof V3<?>) {
            final V3<?> other = (V3<?>) obj;
            return (getX().equals(other.getX()) && getY().equals(other.getY()) && z.equals(other.z));
        } else {
            return super.equals(obj);
        }
    }
}
