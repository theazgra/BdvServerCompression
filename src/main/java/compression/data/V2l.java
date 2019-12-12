package compression.data;

public class V2l {
    private final long x;
    private final long y;

    public V2l(final long x, final long y) {
        this.x = x;
        this.y = y;
    }

    public V2l(final long universalValue) {
        this(universalValue, universalValue);
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }


    public V2l add(final V2l other) {
        return new V2l(x + other.x, y + other.y);
    }

    public V2l add(final V3i other) {
        return new V2l(x + other.getX(), y + other.getY());
    }

    public V2l sub(final V2l other) {
        return new V2l(x - other.x, y - other.y);
    }

    @Override
    public String toString() {
        return String.format("[%d;%d]", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof V2l) {
            final V2l other = (V2l) obj;
            return ((x == other.x) && (y == other.y));
        } else {
            return super.equals(obj);
        }
    }
}
