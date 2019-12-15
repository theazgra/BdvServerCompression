package compression.quantization.vector;

import compression.utilities.TypeConverter;

public class VectorQuantizer {

    private final CodebookEntry[] codebook;
    private final int vectorSize;

    public VectorQuantizer(final CodebookEntry[] codebook) {
        this.codebook = codebook;
        vectorSize = codebook[0].getVector().length;
    }

    public int[][] quantize(final int[][] dataVectors) {
        assert (dataVectors.length > 0 && dataVectors[0].length % vectorSize == 0) : "Wrong vector size";
        int[][] result = new int[dataVectors.length][vectorSize];

        for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
            final CodebookEntry closestEntry = findClosestCodebookEntry(dataVectors[vectorIndex],
                                                                        VectorDistanceMetric.Euclidean);
            result[vectorIndex] = closestEntry.getVector();
        }

        return result;
    }

    public short[][] quantize(short[][] dataVectors) {
        assert (dataVectors.length > 0 && dataVectors[0].length % vectorSize == 0) : "Wrong vector size";
        short[][] result = new short[dataVectors.length][vectorSize];

        for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
            final CodebookEntry closestEntry =
                    findClosestCodebookEntry(TypeConverter.shortArrayToIntArray(dataVectors[vectorIndex]),
                                                                        VectorDistanceMetric.Euclidean);

            result[vectorIndex] = TypeConverter.intArrayToShortArray(closestEntry.getVector());
        }

        return result;
    }

    public static double distanceBetweenVectors(final int[] originalDataVector,
                                                final int[] codebookEntry,
                                                final VectorDistanceMetric metric) {
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
                return Math.sqrt(sum);
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

