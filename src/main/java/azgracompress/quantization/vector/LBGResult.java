package azgracompress.quantization.vector;

import azgracompress.quantization.QTrainIteration;

public class LBGResult {

    private final CodebookEntry[] codebook;
    private final double averageMse;
    private final double psnr;

    public LBGResult(CodebookEntry[] codebook, double averageMse, double psnr) {
        this.codebook = codebook;
        this.averageMse = averageMse;
        this.psnr = psnr;
    }

    public CodebookEntry[] getCodebook() {
        return codebook;
    }

    public double getAverageMse() {
        return averageMse;
    }

    public double getPsnr() {
        return psnr;
    }

    public int getCodebookSize() {
        return codebook.length;
    }
}
