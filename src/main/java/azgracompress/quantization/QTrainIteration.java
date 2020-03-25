package azgracompress.quantization;

public class QTrainIteration {
    private final int iteration;
    private final double mse;
    private final double PSNR;


    public QTrainIteration(int iteration, double mse, double psnr) {
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
