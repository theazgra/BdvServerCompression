package compression.data;

public class V3l {
    private final long x;
    private final long y;
    private final long z;

    public V3l(final long x, final long y, final long z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public V3l(final long universalValue) {
        this(universalValue, universalValue, universalValue);
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }

    public long getZ() {
        return z;
    }

    public V3l add(final V3l other) {
        return new V3l(x + other.x, y + other.y, z + other.z);
    }

    public V3l sub(final V3l other) {
        return new V3l(x - other.x, y - other.y, z - other.z);
    }

    @Override
    public String toString() {
        return String.format("[%d;%d;%d]", x, y, z);
    }
}
