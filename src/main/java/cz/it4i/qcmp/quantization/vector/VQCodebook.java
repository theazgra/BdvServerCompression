package cz.it4i.qcmp.quantization.vector;

import cz.it4i.qcmp.data.V3i;

/**
 * Codebook for vector quantizer.
 */
public class VQCodebook {
    /**
     * Quantization vectors.
     */
    private final int[][] vectors;

    /**
     * Absolute frequencies of quantization vectors.
     */
    private final long[] vectorFrequencies;

    /**
     * Size of the codebook.
     */
    private final int codebookSize;

    /**
     * Vector dimensions.
     */
    private final V3i vectorDims;

    public VQCodebook(final V3i vectorDims, final int[][] vectors, final long[] vectorFrequencies) {
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
    public int[][] getVectors() {
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
