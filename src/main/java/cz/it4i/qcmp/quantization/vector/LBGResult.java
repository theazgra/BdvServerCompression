package cz.it4i.qcmp.quantization.vector;

import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;

public class LBGResult {

    private final int[][] codebookVectors;
    private final long[] frequencies;
    private final double averageMse;
    private final double psnr;
    private final V3i vectorDims;

    public LBGResult(final V3i vectorDims,
                     final int[][] codebook,
                     final long[] frequencies,
                     final double averageMse,
                     final double psnr) {
        this.vectorDims = vectorDims;
        this.codebookVectors = codebook;
        this.frequencies = frequencies;
        this.averageMse = averageMse;
        this.psnr = psnr;
    }

    public VQCodebook getCodebook() {
        final HuffmanTreeBuilder builder = new HuffmanTreeBuilder(codebookVectors.length, frequencies);
        builder.buildHuffmanTree();
        return new VQCodebook(vectorDims, codebookVectors, builder.getRoot());
    }

    public double getAverageMse() {
        return averageMse;
    }

    public double getPsnr() {
        return psnr;
    }

    public int getCodebookSize() {
        return codebookVectors.length;
    }
}
