package azgracompress.quantization.vector;

public class LearningCodebookEntry extends CodebookEntry {

    private int vectorCount = -1;
    private double averageDistortion = -1.0f;
    private double[] perturbationVector;

    public LearningCodebookEntry(int[] codebook) {
        super(codebook);
    }

    /**
     * Set codebook entry properties from helper object.
     *
     * @param info Helper object with property informations.
     */
    public void setInfo(final EntryInfo info) {
        this.vectorCount = info.vectorCount;
        this.averageDistortion = info.calculateAverageDistortion();

        final int[] newCentroid = info.calculateCentroid();
        assert (newCentroid.length == vector.length);
        System.arraycopy(newCentroid, 0, this.vector, 0, newCentroid.length);

        this.perturbationVector = info.calculatePRTVector();
    }

    /**
     * Get perturbation vector for splitting this entry.
     *
     * @return Array of doubles.
     */
    public double[] getPerturbationVector() {
        return perturbationVector;
    }

    /**
     * Get average distortion of this codebook entry.
     *
     * @return Double value.
     */
    public double getAverageDistortion() {
        return averageDistortion;
    }

    /**
     * Get number of vectors which are closer to this codebook entry that to every other entry.
     *
     * @return Number of associated vectors.
     */
    public int getVectorCount() {
        return vectorCount;
    }


}
