package azgracompress.quantization.scalar;

import azgracompress.U16;
import azgracompress.quantization.QTrainIteration;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.Utils;

import java.util.ArrayList;


public class LloydMaxU16ScalarQuantization {
    private final int[] trainingData;
    private int codebookSize;

    private int[] centroids;
    private int[] boundaryPoints;
    private double[] pdf;

    private final int workerCount;

    private boolean verbose = false;

    public LloydMaxU16ScalarQuantization(final int[] trainData, final int codebookSize, final int workerCount) {
        trainingData = trainData;
        this.codebookSize = codebookSize;
        this.workerCount = workerCount;
    }

    public LloydMaxU16ScalarQuantization(final int[] trainData, final int codebookSize) {
        this(trainData, codebookSize, 1);
    }

    private void initialize() {
        centroids = new int[codebookSize];
        centroids[0] = 0;
        boundaryPoints = new int[codebookSize + 1];

        boundaryPoints[0] = U16.Min;
        boundaryPoints[codebookSize] = U16.Max;

        double intervalSize = (double) (U16.Max - U16.Min) / (double) codebookSize;
        for (int i = 0; i < codebookSize; i++) {
            centroids[i] = (int) Math.floor(((double) i + 0.5) * intervalSize);
        }
    }

    private void initializeProbabilityDensityFunction() {
        pdf = new double[U16.Max + 1];
        // Speedup - for now it is fast enough
        Stopwatch s = new Stopwatch();
        s.start();
        for (int i = 0; i < trainingData.length; i++) {
            pdf[trainingData[i]] += 1;
        }
        s.stop();
        if (verbose) {
            System.out.println("Init_PDF: " + s.getElapsedTimeString());
        }
    }

    private void recalculateBoundaryPoints() {
        for (int j = 1; j < codebookSize; j++) {
            boundaryPoints[j] = (centroids[j] + centroids[j - 1]) / 2;
        }
    }

    private void recalculateCentroids() {
        double numerator = 0.0;
        double denominator = 0.0;

        int lowerBound, upperBound;

        for (int j = 0; j < codebookSize; j++) {

            numerator = 0.0;
            denominator = 0.0;

            lowerBound = boundaryPoints[j];
            upperBound = boundaryPoints[j + 1];

            for (int n = lowerBound; n <= upperBound; n++) {
                numerator += (double) n * pdf[n];
                denominator += pdf[n];
            }

            if (denominator > 0) {
                centroids[j] = (int) Math.floor(numerator / denominator);
            }
        }
    }

    public int quantize(final int value) {
        for (int intervalId = 1; intervalId <= codebookSize; intervalId++) {
            if ((value >= boundaryPoints[intervalId - 1]) && (value <= boundaryPoints[intervalId])) {
                return centroids[intervalId - 1];
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }

    private double getCurrentMse() {
        double mse = 0.0;

        Stopwatch s = new Stopwatch();
        s.start();
        if (workerCount > 1) {
            final int workSize = trainingData.length / workerCount;

            RunnableLloydMseCalc[] runnables = new RunnableLloydMseCalc[workerCount];
            Thread[] workers = new Thread[workerCount];
            for (int wId = 0; wId < workerCount; wId++) {
                final int fromIndex = wId * workSize;
                final int toIndex = (wId == workerCount - 1) ? trainingData.length : (workSize + (wId * workSize));


                runnables[wId] = new RunnableLloydMseCalc(trainingData,
                                                          fromIndex,
                                                          toIndex,
                                                          centroids,
                                                          boundaryPoints,
                                                          codebookSize);
                workers[wId] = new Thread(runnables[wId]);
                workers[wId].start();
            }
            try {
                for (int wId = 0; wId < workerCount; wId++) {
                    workers[wId].join();
                    mse += runnables[wId].getMse();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            for (final int trainingDatum : trainingData) {
                int quantizedValue = quantize(trainingDatum);
                mse += Math.pow((double) trainingDatum - (double) quantizedValue, 2);
            }
        }
        s.stop();
        if (verbose) {
            System.out.println("\nLloydMax: getCurrentMse time: " + s.getElapsedTimeString());
        }

        mse /= (double) trainingData.length;

        return mse;
    }

    public QTrainIteration[] train(final boolean shouldBeVerbose) {
        this.verbose = shouldBeVerbose;
        final int RECALCULATE_N_TIMES = 10;
        if (verbose) {
            System.out.println("Training data count: " + trainingData.length);
        }

        initialize();
        initializeProbabilityDensityFunction();

        double prevMse = 1.0;
        double currentMse = 1.0;
        double psnr;

        ArrayList<QTrainIteration> solutionHistory = new ArrayList<>();

        recalculateBoundaryPoints();
        recalculateCentroids();

        currentMse = getCurrentMse();
        psnr = Utils.calculatePsnr(currentMse, U16.Max);

        if (verbose) {
            System.out.println(String.format("Initial MSE: %f", currentMse));
        }

        solutionHistory.add(new QTrainIteration(0, currentMse, currentMse, psnr, psnr));

        double dist = 1;
        int iteration = 0;
        do {
            for (int i = 0; i < RECALCULATE_N_TIMES; i++) {
                recalculateBoundaryPoints();
                recalculateCentroids();
            }
            prevMse = currentMse;
            currentMse = getCurrentMse();
            psnr = Utils.calculatePsnr(currentMse, U16.Max);
            solutionHistory.add(new QTrainIteration(++iteration, currentMse, currentMse, psnr, psnr));
            dist = (prevMse - currentMse) / currentMse;

            if (verbose) {
                System.out.println(String.format("Current MSE: %.4f PSNR: %.4f dB", currentMse, psnr));
            }

        } while (dist > 0.001); //0.0005
        if (verbose) {
            System.out.println("\nFinished training.");
        }
        return solutionHistory.toArray(new QTrainIteration[0]);
    }

    public int[] getCentroids() {
        return centroids;
    }
}

