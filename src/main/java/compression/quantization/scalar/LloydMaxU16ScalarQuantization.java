package compression.quantization.scalar;

import compression.U16;
import compression.utilities.Utils;

import javax.naming.LinkLoopException;
import java.io.FileNotFoundException;
import java.util.ArrayList;


public class LloydMaxU16ScalarQuantization {
    private final int[] trainingData;
    private final int bitCount;
    private int intervalCount;

    private int[] centroids;
    private int[] boundaryPoints;
    private double[] pdf;

    public LloydMaxU16ScalarQuantization(final String trainDataset, final int bitCount) throws FileNotFoundException {
        trainingData = Utils.convertU16ByteArrayToIntArray(Utils.readFileBytes(trainDataset));
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }

    public LloydMaxU16ScalarQuantization(final short[] trainData, final int bitCount) {
        trainingData = Utils.convertShortArrayToIntArray(trainData);
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }

    public LloydMaxU16ScalarQuantization(final int[] trainData, final int bitCount) {
        trainingData = trainData;
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }


    private void initialize() {
        centroids = new int[intervalCount];
        boundaryPoints = new int[intervalCount + 1];

        boundaryPoints[0] = U16.Min;
        boundaryPoints[intervalCount] = U16.Max;

        double intervalSize = (double) (U16.Max - U16.Min) / (double) intervalCount;
        for (int i = 0; i < intervalCount; i++) {
            centroids[i] = (int) Math.floor(((double) i + 0.5) * intervalSize);
        }
    }

    private void initializeProbabilityDensityFunction() {
        pdf = new double[U16.Max + 1];
        for (int i = 0; i < trainingData.length; i++) {
            pdf[trainingData[i]] += 1;
        }
    }

    private void recalculateBoundaryPoints() {
        for (int j = 1; j < intervalCount; j++) {
            boundaryPoints[j] = (centroids[j] + centroids[j - 1]) / 2;
        }
    }

    private void recalculateCentroids() {
        // NOTE(Moravec): We cann't create floating points in here because we are trying to quantize to integer values.

        double numerator = 0.0;
        double denominator = 0.0;

        int lowerBound, upperBound;

        for (int j = 0; j < intervalCount; j++) {

            numerator = 0.0;
            denominator = 0.0;

            lowerBound = boundaryPoints[j];
            //lowerBound = (int) Math.ceil(boundaryPoints[j]);
            upperBound = boundaryPoints[j + 1];
            //upperBound = (int) ((j == (intervalCount - 1)) ? Math.ceil(boundaryPoints[j + 1]) : Math.floor(boundaryPoints[j + 1]));

            for (int n = lowerBound; n <= upperBound; n++) {
                numerator += (double) n * pdf[n];
                denominator += pdf[n];
            }

            if (denominator > 0) {
                // NOTE: Maybe try ceil instead of floor.
                centroids[j] = (int) Math.floor(numerator / denominator);
            }
        }
    }

    public int quantize(final int value) {
        for (int intervalId = 1; intervalId <= intervalCount; intervalId++) {
            if ((value >= boundaryPoints[intervalId - 1]) && (value < boundaryPoints[intervalId])) {
                return centroids[intervalId - 1];
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }

    private double getCurrentMse() {
        double mse = 0.0;
        for (int i = 0; i < trainingData.length; i++) {
            int quantizedValue = quantize(trainingData[i]);
            mse += Math.pow((double) trainingData[i] - (double) quantizedValue, 2);
        }
        mse /= (double) trainingData.length;
        return mse;
    }

    public LloydMaxIteration[] train(boolean verbose) {
        initialize();
        initializeProbabilityDensityFunction();

        double prevMse = 1.0;
        double currentMse = 1.0;

        ArrayList<LloydMaxIteration> solutionHistory = new ArrayList<LloydMaxIteration>();

        recalculateBoundaryPoints();
        recalculateCentroids();
        if (verbose) {
            printCurrentConfigration();
        }
        currentMse = getCurrentMse();
        System.out.println(String.format("Initial MSE: %f", currentMse));
        double psnr;
        int iter = 0;
        solutionHistory.add(new LloydMaxIteration(iter++, currentMse, Utils.calculatePsnr(currentMse, U16.Max), centroids));

        double dist = 1;
        do {
            recalculateBoundaryPoints();
            recalculateCentroids();

            if (verbose) {
                printCurrentConfigration();
            }

            prevMse = currentMse;
            currentMse = getCurrentMse();
            psnr = Utils.calculatePsnr(currentMse, U16.Max);
            solutionHistory.add(new LloydMaxIteration(iter++, currentMse, psnr, centroids));
            dist = (prevMse - currentMse) / currentMse;

            System.out.print(String.format("\rCurrent MSE: %.4f PSNR: %.4f dB", currentMse, psnr));

        } while (dist > 0.0005);
        System.out.println("\nFinished training.");

        printCurrentConfigration();
        return solutionHistory.toArray(new LloydMaxIteration[0]);
    }

    private void printCurrentConfigration() {

        StringBuilder sb = new StringBuilder();
        sb.append("Centroids: ");
        for (int i = 0; i < centroids.length; i++) {
            sb.append(String.format("a[%d]=%d;", i, centroids[i]));
        }
        sb.append("\n");
        sb.append("Boundaries: ");
        for (int i = 0; i < boundaryPoints.length; i++) {
            sb.append(String.format("b[%d]=%d;", i, boundaryPoints[i]));
        }
        System.out.println(sb);
    }

    public short[] quantize(short[] data) {
        // TODO(Moravec): Resolve this duplicate.
        short[] result = new short[data.length];
        for (int i = 0; i < data.length; i++) {
            final int intRepresentationOfValue = Utils.shortBitsToInt(data[i]);
            final int quantizedValue = quantize(intRepresentationOfValue);
            final short shortRepresentation = Utils.u16BitsToShort(quantizedValue);
            result[i] = shortRepresentation;
        }
        return result;
    }

    public int[] quantize(int[] data) {
        int[] result = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = quantize(data[i]);
        }
        return result;
    }

    public short[] quantizeToShortArray(int[] data) {
        short[] result = new short[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = Utils.u16BitsToShort(quantize(data[i]));
        }
        return result;
    }

    public int[] getCentroids() {
        return centroids;
    }
}

