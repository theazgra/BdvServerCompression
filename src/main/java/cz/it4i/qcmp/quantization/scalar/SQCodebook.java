package cz.it4i.qcmp.quantization.scalar;


import cz.it4i.qcmp.huffman.HuffmanNode;
import cz.it4i.qcmp.quantization.Codebook;

/**
 * Codebook for scalar quantizer.
 */
public class SQCodebook extends Codebook {
    /**
     * Quantization values.
     */
    final int[] centroids;

    /**
     * Size of the codebook.
     */
    final int codebookSize;

    /**
     * Create SQ codebook from quantization values and huffman coder.
     *
     * @param centroids   Quantization values.
     * @param huffmanRoot Root of the huffman tree.
     */
    public SQCodebook(final int[] centroids, final HuffmanNode huffmanRoot) {
        super(huffmanRoot);
        this.centroids = centroids;
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
     * Get codebook size.
     *
     * @return Codebook size.
     */
    public int getCodebookSize() {
        return codebookSize;
    }
}
