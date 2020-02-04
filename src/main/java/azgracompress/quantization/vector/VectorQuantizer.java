package azgracompress.quantization.vector;

public class VectorQuantizer {

    private final CodebookEntry[] codebook;
    private final int vectorSize;

    public VectorQuantizer(final CodebookEntry[] codebook) {
        this.codebook = codebook;
        vectorSize = codebook[0].getVector().length;
    }

    public int[] quantize(final int[] dataVector) {
        assert (dataVector.length > 0 && dataVector.length % vectorSize == 0) : "Wrong vector size";
        final CodebookEntry closestEntry = findClosestCodebookEntry(dataVector, VectorDistanceMetric.Euclidean);
        return closestEntry.getVector();
    }

    public int[][] quantize(final int[][] dataVectors, final int workerCount) {
        assert (dataVectors.length > 0 && dataVectors[0].length % vectorSize == 0) : "Wrong vector size";
        int[][] result = new int[dataVectors.length][vectorSize];

        if (workerCount == 1) {
            for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
                final CodebookEntry closestEntry = findClosestCodebookEntry(dataVectors[vectorIndex],
                                                                            VectorDistanceMetric.Euclidean);
                result[vectorIndex] = closestEntry.getVector();
            }
        } else {
            final int[] indices = quantizeIntoIndices(dataVectors, workerCount);
            for (int i = 0; i < dataVectors.length; i++) {
                result[i] = codebook[indices[i]].getVector();
            }
        }

        return result;
    }

    public int[] quantizeIntoIndices(final int[][] dataVectors) {
        return quantizeIntoIndices(dataVectors, 1);
    }


    public int[] quantizeIntoIndices(final int[][] dataVectors, final int maxWorkerCount) {

        assert (dataVectors.length > 0 && dataVectors[0].length % vectorSize == 0) : "Wrong vector size";
        int[] indices = new int[dataVectors.length];

        if (maxWorkerCount == 1) {
            for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
                indices[vectorIndex] = findClosestCodebookEntryIndex(dataVectors[vectorIndex],
                                                                     VectorDistanceMetric.Euclidean);
            }
        } else {
            // Cap the worker count on 8
            final int workerCount = Math.min(maxWorkerCount, 8);
            Thread[] workers = new Thread[workerCount];
            final int workSize = dataVectors.length / workerCount;

            for (int wId = 0; wId < workerCount; wId++) {
                final int fromIndex = wId * workSize;
                final int toIndex = (wId == workerCount - 1) ? dataVectors.length : (workSize + (wId * workSize));

                workers[wId] = new Thread(() -> {
                    for (int vectorIndex = fromIndex; vectorIndex < toIndex; vectorIndex++) {
                        indices[vectorIndex] = findClosestCodebookEntryIndex(dataVectors[vectorIndex],
                                                                             VectorDistanceMetric.Euclidean);
                    }
                });

                workers[wId].start();
            }
            try {
                for (int wId = 0; wId < workerCount; wId++) {
                    workers[wId].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        return indices;
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
        return codebook[findClosestCodebookEntryIndex(dataVector, metric)];
    }

    private int findClosestCodebookEntryIndex(final int[] dataVector, final VectorDistanceMetric metric) {
        double minDist = Double.MAX_VALUE;
        int closestEntryIndex = 0;
        final int codebookSize = codebook.length;
        for (int i = 0; i < codebookSize; i++) {
            final double dist = distanceBetweenVectors(dataVector, codebook[i].getVector(), metric);
            if (dist < minDist) {
                minDist = dist;
                closestEntryIndex = i;
            }
        }
        return closestEntryIndex;
    }

    public CodebookEntry[] getCodebook() {
        return codebook;
    }
}

