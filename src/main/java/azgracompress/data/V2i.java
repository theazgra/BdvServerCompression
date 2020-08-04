package azgracompress.data;

public final class V2i extends V2<Integer> {
    public V2i(final int x, final int y) {
        super(x, y);
    }

    public V2i(final int universalValue) {
        this(universalValue, universalValue);
    }

    public final V2i add(final V2i other) {
        return new V2i(getX() + other.getX(), getY() + other.getY());
    }

    public final V2i sub(final V2i other) {
        return new V2i(getX() - other.getX(), getY() - other.getY());
    }

    public final V3i toV3i() {
        return new V3i(getX(), getY(), 1);
    }
}
