package azgracompress.quantization.scalar;

import azgracompress.U16;
import azgracompress.quantization.QTrainIteration;
import azgracompress.utilities.MinMaxResult;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;


public class LloydMaxU16ScalarQuantization {
    private final int[] trainingData;
    private int codebookSize;

    private int dataMin;
    private int dataMax;

    private long[] frequencies;
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
        frequencies = new long[codebookSize];
        centroids = new int[codebookSize];

        boundaryPoints = new int[codebookSize + 1];

        MinMaxResult<Integer> minMax = Utils.getMinAndMax(trainingData);
        dataMin = minMax.getMin();
        dataMax = minMax.getMax();
        final int dataSpan = dataMax - dataMin;
        centroids[0] = dataMin;

        boundaryPoints[0] = dataMin;
        boundaryPoints[codebookSize] = dataMax;
        double intervalSize = (double) (dataSpan) / (double) codebookSize;
        for (int i = 0; i < codebookSize; i++) {
            centroids[i] = (int) Math.floor(((double) i + 0.5) * intervalSize);
        }
    }

    private void initializeProbabilityDensityFunction() {
        pdf = new double[U16.Max + 1];
        // Speedup - for now it is fast enough
        Stopwatch s = new Stopwatch();
        s.start();

        for (final int trainingDatum : trainingData) {
            pdf[trainingDatum] += 1.0;
        }

        s.stop();
        if (verbose) {
            System.out.println("Init_PDF: " + s.getElapsedTimeString());
        }
    }

    private void recalculateBoundaryPoints() {
        for (int j = 1; j < codebookSize; j++) {
            boundaryPoints[j] = Math.min(dataMax,
                                         (int) Math.floor(((double) centroids[j] + (double) centroids[j - 1]) / 2.0));
        }
    }

    private void initializeCentroids() {
        int lowerBound, upperBound;
        double[] centroidPdf = new double[codebookSize];
        for (int centroidIndex = 0; centroidIndex < codebookSize; centroidIndex++) {
            lowerBound = boundaryPoints[centroidIndex];
            upperBound = boundaryPoints[centroidIndex + 1];

            for (int rangeValue = lowerBound; rangeValue <= upperBound; rangeValue++) {
                if (pdf[rangeValue] > centroidPdf[centroidIndex]) {
                    centroidPdf[centroidIndex] = pdf[rangeValue];
                    centroids[centroidIndex] = rangeValue;
                }
            }
        }
    }

    private void recalculateCentroids() {
        double numerator = 0.0;
        double denominator = 0.0;

        int lowerBound, upperBound;

        for (int centroidIndex = 0; centroidIndex < codebookSize; centroidIndex++) {

            numerator = 0.0;
            denominator = 0.0;

            lowerBound = boundaryPoints[centroidIndex];
            upperBound = boundaryPoints[centroidIndex + 1];

            for (int n = lowerBound; n <= upperBound; n++) {
                numerator += (double) n * pdf[n];
                denominator += pdf[n];
            }

            if (denominator > 0) {
                centroids[centroidIndex] = (int) Math.floor(numerator / denominator);
            }
        }
    }

    public int quantize(final int value) {
        for (int intervalId = 1; intervalId <= codebookSize; intervalId++) {
            if ((value >= boundaryPoints[intervalId - 1]) && (value <= boundaryPoints[intervalId])) {
                ++frequencies[intervalId - 1];
                return centroids[intervalId - 1];
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }

    /**
     * Reset the frequencies array to zeros.
     */
    private void resetFrequencies() {
        Arrays.fill(frequencies, 0);
    }

    private double getCurrentMse() {
        double mse = 0.0;
        resetFrequencies();

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
                    addWorkerFrequencies(runnables[wId].getFrequencies());
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

    private void addWorkerFrequencies(final long[] workerFrequencies) {
        assert (frequencies.length == workerFrequencies.length) : "Frequency array length mismatch.";
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] += workerFrequencies[i];
        }
    }

    public QTrainIteration[] train(final boolean shouldBeVerbose) {
        this.verbose = shouldBeVerbose;
        final int RECALCULATE_N_TIMES = 10;
        final int PATIENCE = 1;
        int noImprovementCounter = 0;
        if (verbose) {
            System.out.println("Training data count: " + trainingData.length);
        }

        initialize();
        initializeProbabilityDensityFunction();


        double currMAE = 1.0;
        double prevMse = 1.0;
        double currentMse = 1.0;
        double psnr;

        ArrayList<QTrainIteration> solutionHistory = new ArrayList<>();

        recalculateBoundaryPoints();
        initializeCentroids();

        currentMse = getCurrentMse();
        psnr = Utils.calculatePsnr(currentMse, U16.Max);

        if (verbose) {
            System.out.println(String.format("Initial MSE: %f", currentMse));
        }

        solutionHistory.add(new QTrainIteration(0, currentMse, currentMse, psnr, psnr));

        double mseImprovement = 1;
        int iteration = 0;
        do {
            for (int i = 0; i < RECALCULATE_N_TIMES; i++) {
                recalculateBoundaryPoints();
                recalculateCentroids();
            }

            prevMse = currentMse;
            currentMse = getCurrentMse();
            mseImprovement = prevMse - currentMse;

            psnr = Utils.calculatePsnr(currentMse, U16.Max);
            solutionHistory.add(new QTrainIteration(++iteration, currentMse, currentMse, psnr, psnr));

            if (verbose) {
                System.out.println(String.format("Current MSE: %.4f PSNR: %.4f dB",
                                                 currentMse,
                                                 psnr));
            }

            if (mseImprovement < 1.0) {
                if ((++noImprovementCounter) >= PATIENCE) {
                    break;
                }
            }


        } while (true);
        if (verbose) {
            System.out.println("\nFinished training.");
        }
        return solutionHistory.toArray(new QTrainIteration[0]);
    }

    public SQCodebook getCodebook() {
        return new SQCodebook(centroids, frequencies);
    }
}

