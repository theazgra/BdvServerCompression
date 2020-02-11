package azgracompress.quantization;

public class QTrainIteration {
    private final int iteration;
    private final double averageMSE;
    private final double bestMSE;
    private final double averagePSNR;
    private final double bestPSNR;


    public QTrainIteration(int iteration, double averageMSE, double bestMSE, double averagePSNR, double bestPSNR) {
        this.iteration = iteration;
        this.averageMSE = averageMSE;
        this.bestMSE = bestMSE;
        this.averagePSNR = averagePSNR;
        this.bestPSNR = bestPSNR;
    }

    public int getIteration() {
        return iteration;
    }

    public double getAverageMSE() {
        return averageMSE;
    }

    public double getBestMSE() {
        return bestMSE;
    }

    public double getAveragePSNR() {
        return averagePSNR;
    }

    public double getBestPSNR() {
        return bestPSNR;
    }
}
