package azgracompress.quantization.scalar;


public class ScalarQuantizationCodebook {
    /**
     * Quantization values.
     */
    final int[] centroids;

    /**
     * Absolute frequencies of quantization values.
     */
    final long[] indexFrequencies;

    final int codebookSize;

    /**
     * @param centroids        Quantization values.
     * @param indexFrequencies Absolute frequencies of quantization values.
     */
    public ScalarQuantizationCodebook(final int[] centroids, final long[] indexFrequencies) {
        this.centroids = centroids;
        this.indexFrequencies = indexFrequencies;
        this.codebookSize = this.centroids.length;
    }

    public int[] getCentroids() {
        return centroids;
    }

    public long[] getIndicesFrequency() {
        return indexFrequencies;
    }

    public int getCodebookSize() {
        return codebookSize;
    }
}
