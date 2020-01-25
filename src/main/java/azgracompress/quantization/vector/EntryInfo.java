package azgracompress.quantization.vector;

import azgracompress.U16;

import java.util.Arrays;

public class EntryInfo {
    public int vectorCount;
    public int[] min;
    public int[] max;
    public double distanceSum;
    public double[] dimensionSum;
    private final int vectorSize;

    public EntryInfo(final int vectorSize) {
        this.vectorSize = vectorSize;
        vectorCount = 0;
        max = new int[vectorSize];
        min = new int[vectorSize];
        Arrays.fill(min, U16.Max);
        distanceSum = 0;
        dimensionSum = new double[vectorSize];
    }

    public double calculateAverageDistortion() {
        return (distanceSum / (double) vectorCount);
    }

    public int[] calculateCentroid() {
        int[] centroid = new int[vectorSize];
        for (int dim = 0; dim < vectorSize; dim++) {
            centroid[dim] = (int) Math.round(dimensionSum[dim] / (double) vectorCount);
        }
        return centroid;
    }

    public double[] calculatePRTVector() {
        double[] prtV = new double[vectorSize];
        for (int dim = 0; dim < vectorSize; dim++) {
            prtV[dim] = (((double) max[dim] - (double) min[dim]) / LBGVectorQuantizer.PRT_VECTOR_DIVIDER);
        }
        return prtV;
    }
}
