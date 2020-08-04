package azgracompress.data;

public final class V2i {
    private final int x;
    private final int y;

    public V2i(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public V2i(final int universalValue) {
        this(universalValue, universalValue);
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }


    public final V2i add(final V2i other) {
        return new V2i(x + other.x, y + other.y);
    }

    public final V2i sub(final V2i other) {
        return new V2i(x - other.x, y - other.y);
    }

    @Override
    public String toString() {
        return String.format("[%d;%d]", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof V2i) {
            final V2i other = (V2i) obj;
            return ((x == other.x) && (y == other.y));
        } else {
            return super.equals(obj);
        }

    }

    public final V3i toV3i() {
        return new V3i(x, y, 1);
    }
}
