package azgracompress.quantization.vector;

import azgracompress.data.V3i;

/**
 * Codebook for vector quantizer.
 */
public class VQCodebook {
    /**
     * Quantization vectors.
     */
    private final CodebookEntry[] vectors;

    /**
     * Absolute frequencies of quantization vectors.
     */
    private long[] vectorFrequencies;

    /**
     * Size of the codebook.
     */
    private final int codebookSize;

    /**
     * Vector dimensions.
     */
    private final V3i vectorDims;

    public VQCodebook(final V3i vectorDims, final CodebookEntry[] vectors, final long[] vectorFrequencies) {
        //assert (vectors.length == vectorFrequencies.length);
        this.vectorDims = vectorDims;
        this.vectors = vectors;
        this.vectorFrequencies = vectorFrequencies;
        this.codebookSize = vectors.length;
    }

    /**
     * Get vectors (quantization vectors) from the codebook.
     *
     * @return Quantization vectors.
     */
    public CodebookEntry[] getVectors() {
        return vectors;
    }

    /**
     * Get frequencies of codebook vectors at indices.
     *
     * @return Frequencies of vectors.
     */
    public long[] getVectorFrequencies() {
        return vectorFrequencies;
    }

    /**
     * Get codebook size.
     *
     * @return Codebook size.
     */
    public int getCodebookSize() {
        return codebookSize;
    }

    /**
     * Get vector dimensions.
     *
     * @return Vector dimensions.
     */
    public V3i getVectorDims() {
        return vectorDims;
    }
}
