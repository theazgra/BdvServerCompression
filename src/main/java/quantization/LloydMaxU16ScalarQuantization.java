package quantization;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;


public class LloydMaxU16ScalarQuantization {

    private final U16 Min = U16.MinU16;
    private final U16 Max = U16.MaxU16

    private final U16[] trainingData;
    private final int bitCount;
    private int intervalCount;

    private U16[] centroids;
    private U16[] boundaryPoints;
    private double[] pdf;

    private static U16[] convertToU16(final short[] src) {
        U16[] result = new U16[src.length];
        for (int i = 0; i < src.length; i++) {
            result[i] = new U16(src[i]);
        }
        return result;
    }

    private static U16[] convertToU16(final char[] src) {
        U16[] result = new U16[src.length];
        for (int i = 0; i < src.length; i++) {
            result[i] = new U16(src[i]);
        }
        return result;
    }

    public LloydMaxU16ScalarQuantization(final String trainDataset, final int bitCount) throws FileNotFoundException {
        trainingData = convertToU16(Utils.convertBytesToU16(Utils.readFileBytes(trainDataset)));
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }

    public LloydMaxU16ScalarQuantization(final char[] trainData, final int bitCount) {
        trainingData = convertToU16(trainData);
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }

    public LloydMaxU16ScalarQuantization(final short[] trainData, final int bitCount) {
        trainingData = convertToU16(trainData);
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }


    private void initialize() {
        centroids = new U16[intervalCount];
        boundaryPoints = new U16[intervalCount + 1];

        boundaryPoints[0] = U16.MinU16;
        boundaryPoints[intervalCount] = U16.MaxU16;

        double intervalSize = (double) (U16.MaxValue - U16.MinValue) / (double) intervalCount;
        for (int i = 0; i < intervalCount; i++) {
            centroids[i] = new U16((int) Math.floor(((double) i + 0.5) * intervalSize));
        }
    }

    private void initializeProbabilityDensityFunction() {
        pdf = new double[U16.MaxValue + 1];
        for (int i = 0; i < trainingData.length; i++) {
            pdf[trainingData[i].value()] += 1;
        }
    }

    private void recalculateBoundaryPoints() {
        for (int j = 1; j < intervalCount; j++) {
            if (j == intervalCount - 1) {
                boundaryPoints[j].set((int) Math.ceil((centroids[j].add(centroids[j - 1]).doubleValue()) / 2.0));
            }
        }
    }

    private void recalculateCentroids() {
        // TODO(Moravec): We cann't create floating points in here because we are trying to quantize to integer values.
        double numerator = 0.0;
        double denominator = 0.0;
        for (int j = 0; j < intervalCount; j++) {
            int from = (int) Math.floor(boundaryPoints[j]);
            int to = (int) Math.ceil(boundaryPoints[j + 1]);
            for (int n = from; n <= to; n++) {
                numerator += (double) n * pdf[n];
                denominator += pdf[n];
            }
            centroids[j] = (numerator / denominator);
        }
    }

    public <T extends Number> U16 quantize(final T value) {
        double doubleValue = value.doubleValue();
        for (int intervalId = 1; intervalId <= intervalCount; intervalId++) {
            if ((doubleValue >= boundaryPoints[intervalId - 1]) && (doubleValue < boundaryPoints[intervalId])) {
                return centroids[intervalId - 1];
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }

    private double getCurrentMse() {
        double mse = 0.0;
        for (int i = 0; i < trainingData.length; i++) {
            U16 quantizedValue = quantize(trainingData[i]);
            mse += Math.pow((trainingData[i].sub(quantizedValue).doubleValue()),2);
        }
        mse /= (double) trainingData.length;
        return mse;
    }

    public void train() {
        initialize();
        initializeProbabilityDensityFunction();

        double prevMse = 1.0;
        double currentMse = 1.0;

        recalculateBoundaryPoints();
        recalculateCentroids();

        printCurrentConfigration();

        currentMse = getCurrentMse();
        System.out.println(String.format("Current MSE: %f", currentMse));

        double dist = 1;
        do {
            recalculateBoundaryPoints();
            recalculateCentroids();

            printCurrentConfigration();

            prevMse = currentMse;
            currentMse = getCurrentMse();
            dist = (prevMse - currentMse) / currentMse;
            System.out.println(String.format("Current MSE: %f", currentMse));

        } while (dist > 0.001);

        //recalculateCentroids2();
    }

    private void printCurrentConfigration() {

        StringBuilder sb = new StringBuilder();
        sb.append("Centroids: ");
        for (int i = 0; i < centroids.length; i++) {
            sb.append(String.format("a[%d]=%d;", i, centroids[i].value()));
        }
        sb.append("\n");
        sb.append("Boundaries: ");
        for (int i = 0; i < boundaryPoints.length; i++) {
            sb.append(String.format("b[%d]=%d;", i, boundaryPoints[i].value()));
        }
        System.out.println(sb);
    }

    public <T extends Number> U16[] quantize(T[] data) {
        U16[] result = new U16[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = quantize(data[i]);
        }
        return result;
    }
}
