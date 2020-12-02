package cz.it4i.qcmp.quantization.vector;

import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.huffman.HuffmanNode;
import cz.it4i.qcmp.quantization.Codebook;

/**
 * Codebook for vector quantizer.
 */
public class VQCodebook extends Codebook {
    /**
     * Quantization vectors.
     */
    private final int[][] vectors;


    /**
     * Size of the codebook.
     */
    private final int codebookSize;

    /**
     * Vector dimensions.
     */
    private final V3i vectorDims;

    /**
     * Create vector quantization codebook from quantization vectors and huffman coder.
     *
     * @param vectorDims  Quantization vector dimensions.
     * @param vectors     Quantization vectors.
     * @param huffmanRoot Root of the huffman tree.
     */
    public VQCodebook(final V3i vectorDims, final int[][] vectors, final HuffmanNode huffmanRoot) {
        this.vectorDims = vectorDims;
        this.vectors = vectors;
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
