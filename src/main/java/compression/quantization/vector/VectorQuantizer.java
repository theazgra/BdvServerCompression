package compression.quantization.vector;

import compression.utilities.Utils;
import org.apache.commons.math3.stat.descriptive.moment.VectorialCovariance;

public class VectorQuantizer {

    private final CodebookEntry[] codebook;
    private final int quantizedVectorSize;

    public VectorQuantizer(final CodebookEntry[] codebook) {
        this.codebook = codebook;
        quantizedVectorSize = codebook[0].getVector().length;
    }

    public short[] quantize(short[] data) {
        short[] result = new short[data.length];
        int[] originalDataBuffer = new int[quantizedVectorSize];

        for (int i = 0; i < data.length; i++) {

            for (int j = 0; j < quantizedVectorSize; j++) {
                originalDataBuffer[j] = data[((i * quantizedVectorSize) + j)];
            }
            // Find the closest codebook entry
            final CodebookEntry closestEntry = findClosestCodebookEntry(originalDataBuffer, VectorDistanceMetric.Euclidean);

            for (int j = 0; j < quantizedVectorSize; j++) {
                result[((i * quantizedVectorSize) + j)] = Utils.u16BitsToShort(closestEntry.getVector()[j]);
            }
        }
        return result;
    }

    private double distanceBetweenVectors(final int[] originalDataVector, final int[] codebookEntry, final VectorDistanceMetric metric) {
        assert (originalDataVector.length == codebookEntry.length);
        switch (metric) {
            case Manhattan: {
                double sum = 0.0;
                for (int i = 0; i < originalDataVector.length; i++) {
                    sum += Math.abs((double) originalDataVector[i] - (double) codebookEntry[i]);
                }
                return sum;
            }
            case Euclidean: {
                double sum = 0.0;
                for (int i = 0; i < originalDataVector.length; i++) {
                    sum += Math.pow(((double) originalDataVector[i] - (double) codebookEntry[i]), 2);
                }
                return sum;
            }
            case MaxDiff: {
                double maxDiff = Double.MIN_VALUE;
                for (int i = 0; i < originalDataVector.length; i++) {
                    final double diff = Math.abs((double) originalDataVector[i] - (double) codebookEntry[i]);
                    if (diff > maxDiff) {
                        maxDiff = diff;
                    }
                }
                return maxDiff;
            }
        }
        assert (false) : "Unreachable code reached";
        return 0.0;
    }

    private CodebookEntry findClosestCodebookEntry(final int[] dataVector) {
        return findClosestCodebookEntry(dataVector, VectorDistanceMetric.Euclidean);
    }

    private CodebookEntry findClosestCodebookEntry(final int[] dataVector, final VectorDistanceMetric metric) {
        double minDist = Double.MAX_VALUE;
        CodebookEntry closestEntry = null;
        for (CodebookEntry codebookEntry : codebook) {
            final double dist = distanceBetweenVectors(dataVector, codebookEntry.getVector(), metric);
            if (dist < minDist) {
                minDist = dist;
                closestEntry = codebookEntry;
            }
        }
        assert (closestEntry != null) : "Closest entry wasn't found";
        return closestEntry;
    }
}

