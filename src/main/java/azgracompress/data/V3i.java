package azgracompress.data;

public class V3i {
    private final int x;
    private final int y;
    private final int z;

    public V3i(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public V3i(final int x, final int y) {
        this(x,y,1);
    }

    public V3i(final int universalValue) {
        this(universalValue, universalValue, universalValue);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public V3i add(final V3i other) {
        return new V3i(x + other.x, y + other.y, z + other.z);
    }

    public V3i sub(final V3i other) {
        return new V3i(x - other.x, y - other.y, z - other.z);
    }

    @Override
    public String toString() {
        return String.format("[%d;%d;%d]", x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof V3i) {
            final V3i other = (V3i) obj;
            return ((x == other.x) && (y == other.y) && (z == other.z));
        } else {
            return super.equals(obj);
        }

    }

    public V3l toV3l() {
        return new V3l(x, y, z);
    }

    /**
     * Convert this vector to V2i by dropping the Z value.
     *
     * @return V2i vector with X and Y values.
     */
    public V2i toV2i() {
        return new V2i(x, y);
    }
}
