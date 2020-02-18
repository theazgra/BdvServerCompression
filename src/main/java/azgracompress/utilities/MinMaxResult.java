package azgracompress.utilities;

public class MinMaxResult<T> {
    private final T min;
    private final T max;

    MinMaxResult(T min, T max) {
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