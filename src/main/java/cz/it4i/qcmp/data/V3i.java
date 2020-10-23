package cz.it4i.qcmp.data;

public final class V3i extends V3<Integer> {

    public V3i(final int x, final int y, final int z) {
        super(x, y, z);
    }

    public V3i(final int x, final int y) {
        this(x, y, 1);
    }

    public V3i(final int universalValue) {
        this(universalValue, universalValue, universalValue);
    }

    public V3i add(final V3i other) {
        return new V3i(getX() + other.getX(), getY() + other.getY(), getZ() + other.getZ());
    }

    public V3i sub(final V3i other) {
        return new V3i(getX() - other.getX(), getY() - other.getY(), getZ() - other.getZ());
    }


    /**
     * Convert this vector to V2i by dropping the Z value.
     *
     * @return V2i vector with X and Y values.
     */
    public final V2i toV2i() {
        return new V2i(getX(), getY());
    }

    public final int multiplyTogether() {
        return Math.multiplyExact(getZ(), Math.multiplyExact(getX(), getY()));
    }
}
