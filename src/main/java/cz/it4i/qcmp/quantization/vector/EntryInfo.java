package cz.it4i.qcmp.quantization.vector;

import cz.it4i.qcmp.U16;

import java.util.Arrays;

/**
 * This object works as C struct and help in calculation of Codebook entry properties.
 */
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

    /**
     * Calculate the average distortion as average distance of all vectors from centroid.
     *
     * @return Average distance of vectors.
     */
    public double calculateAverageDistortion() {
        return (distanceSum / (double) vectorCount);
    }

    /**
     * Calculate the centroid from dimension sums.
     *
     * @return Centroid of the vectors.
     */
    public int[] calculateCentroid() {
        final int[] centroid = new int[vectorSize];
        for (int dim = 0; dim < vectorSize; dim++) {
            centroid[dim] = (int) Math.round(dimensionSum[dim] / (double) vectorCount);
        }
        return centroid;
    }

    /**
     * Calculate the perturbation vector from mins and maxes.
     *
     * @return Perturbation vector of the vectors.
     */
    public double[] calculatePRTVector() {
        final double[] prtV = new double[vectorSize];
        for (int dim = 0; dim < vectorSize; dim++) {
            prtV[dim] = ((double) max[dim] - (double) min[dim]) / LBGVectorQuantizer.PRT_VECTOR_DIVIDER;
        }
        return prtV;
    }
}
