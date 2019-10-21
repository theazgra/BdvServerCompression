package quantization;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;


public class LloydMaxU16ScalarQuantization {

    private final int Min = 0;
    private final int Max = 65535;

    private final char[] trainingData;
    private final int bitCount;
    private int intervalCount;

    private double[] centroids;
    private double[] boundaryPoints;

    private double[] pdf;

    public LloydMaxU16ScalarQuantization(final String trainDataset, final int bitCount) throws FileNotFoundException {
        trainingData = Utils.convertBytesToU16(Utils.readFileBytes(trainDataset));
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }

    public LloydMaxU16ScalarQuantization(final char[] trainData, final int bitCount) {
        trainingData = trainData;
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }

    public LloydMaxU16ScalarQuantization(final short[] trainData, final int bitCount) {
        trainingData = new char[trainData.length];
        for (int i = 0; i < trainData.length; i++) {
            trainingData[i] = (char) trainData[i];
        }
        this.bitCount = bitCount;
        this.intervalCount = (int) Math.pow(2, this.bitCount);
    }


    private void initialize() {
        centroids = new double[intervalCount];
        boundaryPoints = new double[intervalCount + 1];


//        char max = 0;
//        for (int i = 0; i < trainingData.length; i++) {
//            if (trainingData[i] > max)
//                max = trainingData[i];
//        }

        boundaryPoints[0] = Min;
        boundaryPoints[intervalCount] = Max;

        double intervalSize = (double) (Max - Min) / (double) intervalCount;
        for (int i = 0; i < intervalCount; i++) {
            centroids[i] = ((double) i + 0.5) * intervalSize;
        }
    }

    private void initializeProbabilityDensityFunction() {
        pdf = new double[Max + 1];
        for (int i = 0; i < trainingData.length; i++) {
            pdf[trainingData[i]] += 1;
        }

//        double len = (double) trainingData.length;
//        for (int i = 0; i < Max + 1; i++) {
//            pdf[i] /= len;
//        }
    }

    private void recalculateBoundaryPoints() {
        for (int j = 1; j < intervalCount; j++) {
            boundaryPoints[j] = (centroids[j] + centroids[j - 1]) / 2.0;
        }
    }

    private void recalculateCentroids() {
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

    private void recalculateCentroids2() {
        for (int j = 0; j < intervalCount; j++) {
            centroids[j] = boundaryPoints[j] + ((boundaryPoints[j + 1] - boundaryPoints[j]) / 2.0);
        }
    }

    private double quantizeChar(char v) {
        double dv = (double) v;
        for (int intervalId = 1; intervalId <= intervalCount; intervalId++) {
            if (dv >= boundaryPoints[intervalId - 1] && dv < boundaryPoints[intervalId]) {
                return centroids[intervalId - 1];
            }
        }
        throw new AssertionError("Dont get here!");
    }

    private double quantizeShort(short v) {
        return quantizeChar((char) v);
    }

    private double getCurrentMse() {
        double mse = 0.0;
        for (int i = 0; i < trainingData.length; i++) {
            mse += (Math.pow(trainingData[i] - quantizeChar(trainingData[i]), 2));
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
            sb.append(String.format("a[%d]=%.5f;", i, (Math.round(centroids[i] * 100.0) / 100.0)));
        }
        sb.append("\n");
        sb.append("Boundaries: ");
        for (int i = 0; i < boundaryPoints.length; i++) {
            sb.append(String.format("b[%d]=%.5f;", i, (Math.round(boundaryPoints[i] * 100.0) / 100.0)));
        }
        System.out.println(sb);
    }

    public short[] quantizeArray(short[] data) {
        short[] result = new short[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (short) Math.floor(quantizeShort(data[i]));
        }
        return result;
    }
}
