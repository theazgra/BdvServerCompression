package cz.it4i.qcmp.utilities;

public class MinMaxResult<T> {
    private final T min;
    private final T max;

    MinMaxResult(final T min, final T max) {
        this.min = min;
        this.max = max;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }
}