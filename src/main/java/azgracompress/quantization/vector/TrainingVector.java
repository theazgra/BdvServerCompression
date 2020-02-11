package azgracompress.quantization.vector;

/**
 * Training image vector.
 */
public class TrainingVector {

    final int[] vector;
    private int entryIndex = -1;
    private double entryDistance = Double.POSITIVE_INFINITY;

    public TrainingVector(int[] vector) {
        this.vector = vector;
    }

    /**
     * Set the closest codebook entry index and its distance.
     *
     * @param closestEntryIndex Closest codebook entry index.
     * @param minDist           Distance to the closest codebook entry.
     */
    public void setEntryInfo(final int closestEntryIndex, final double minDist) {
        this.entryIndex = closestEntryIndex;
        this.entryDistance = minDist;
    }

    /**
     * Get the training vector data.
     *
     * @return Int array.
     */
    public int[] getVector() {
        return vector;
    }

    /**
     * Get the index of the closest codebook entry.
     *
     * @return Index of the codebook entry
     */
    public int getEntryIndex() {
        return entryIndex;
    }

    /**
     * Get the distance to the closest codebook entry.
     *
     * @return Distance to the codebook entry.
     */
    public double getEntryDistance() {
        return entryDistance;
    }
}
