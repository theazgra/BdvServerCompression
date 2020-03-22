package azgracompress.quantization.scalar;


/**
 * Codebook for scalar quantizer.
 */
public class SQCodebook {
    /**
     * Quantization values.
     */
    final int[] centroids;

    /**
     * Absolute frequencies of quantization values.
     */
    final long[] indexFrequencies;

    /**
     * Size of the codebook.
     */
    final int codebookSize;

    /**
     * @param centroids        Quantization values.
     * @param indexFrequencies Absolute frequencies of quantization values.
     */
    public SQCodebook(final int[] centroids, final long[] indexFrequencies) {
        assert (centroids.length == indexFrequencies.length);
        this.centroids = centroids;
        this.indexFrequencies = indexFrequencies;
        this.codebookSize = this.centroids.length;
    }

    /**
     * Get centroids (quantization values) from the codebook.
     *
     * @return Quantization values.
     */
    public int[] getCentroids() {
        return centroids;
    }

    /**
     * Get frequencies of codebook symbols at indices.
     *
     * @return Frequencies of symbols.
     */
    public long[] getSymbolFrequencies() {
        return indexFrequencies;
    }

    /**
     * Get codebook size.
     *
     * @return Codebook size.
     */
    public int getCodebookSize() {
        return codebookSize;
    }
}
