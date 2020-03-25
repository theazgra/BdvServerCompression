package azgracompress.quantization.vector;

import azgracompress.data.V3i;

public class LBGResult {

    private final CodebookEntry[] codebookVectors;
    private final long[] frequencies;
    private final double averageMse;
    private final double psnr;
    private final V3i vectorDims;

    public LBGResult(final V3i vectorDims,
                     final CodebookEntry[] codebook,
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
        return new VQCodebook(vectorDims, codebookVectors, frequencies);
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
