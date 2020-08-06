package azgracompress.data;

public final class Range<T extends Comparable<T>> {
    /**
     * Start of the interval.
     */
    private final T from;

    /**
     * End of the interval.
     */
    private final T to;

    /**
     * Construct interval with inclusive end.
     *
     * @param from Start of the interval,
     * @param to   Inclusive end of the interval.
     */
    public Range(T from, T to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Get start of the interval.
     *
     * @return Start of the interval.
     */
    public final T getFrom() {
        return from;
    }

    /**
     * Get inclusive end of the interval.
     *
     * @return Inclusive end of the interval.
     */
    public final T getInclusiveTo() {
        return to;
    }
}
