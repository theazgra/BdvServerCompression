package cz.it4i.qcmp.quantization;

public class QTrainIteration {
    private final int iteration;
    private final double mse;
    private final double PSNR;


    public QTrainIteration(final int iteration, final double mse, final double psnr) {
        this.iteration = iteration;
        this.mse = mse;
        this.PSNR = psnr;
    }

    public int getIteration() {
        return iteration;
    }

    public double getMse() {
        return mse;
    }

    public double getPSNR() {
        return PSNR;
    }
}
