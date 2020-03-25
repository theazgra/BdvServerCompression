package azgracompress.quantization.vector;

public class VectorQuantizer {

    private final VectorDistanceMetric metric = VectorDistanceMetric.Euclidean;
    private final VQCodebook codebook;
    private final CodebookEntry[] codebookVectors;
    private final int vectorSize;
    private final long[] frequencies;

    public VectorQuantizer(final VQCodebook codebook) {
        this.codebook = codebook;
        this.codebookVectors = codebook.getVectors();
        vectorSize = codebookVectors[0].getVector().length;
        this.frequencies = codebook.getVectorFrequencies();
    }

    public int[] quantize(final int[] dataVector) {
        assert (dataVector.length > 0 && dataVector.length % vectorSize == 0) : "Wrong vector size";
        final CodebookEntry closestEntry = findClosestCodebookEntry(dataVector, metric);
        return closestEntry.getVector();
    }

    public int quantizeToIndex(final int[] dataVector) {
        assert (dataVector.length > 0 && dataVector.length % vectorSize == 0) : "Wrong vector size";
        return findClosestCodebookEntryIndex(dataVector, metric);
    }

    public int[][] quantize(final int[][] dataVectors, final int workerCount) {
        assert (dataVectors.length > 0 && dataVectors[0].length % vectorSize == 0) : "Wrong vector size";
        int[][] result = new int[dataVectors.length][vectorSize];

        if (workerCount == 1) {
            for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
                final CodebookEntry closestEntry = findClosestCodebookEntry(dataVectors[vectorIndex], metric);
                result[vectorIndex] = closestEntry.getVector();
            }
        } else {
            final int[] indices = quantizeIntoIndices(dataVectors, workerCount);
            for (int i = 0; i < dataVectors.length; i++) {
                result[i] = codebookVectors[indices[i]].getVector();
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
            int closestIndex;
            for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
                closestIndex = findClosestCodebookEntryIndex(dataVectors[vectorIndex], metric);
                indices[vectorIndex] = closestIndex;
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
                    int closestIndex;
                    for (int vectorIndex = fromIndex; vectorIndex < toIndex; vectorIndex++) {
                        closestIndex = findClosestCodebookEntryIndex(dataVectors[vectorIndex], metric);
                        indices[vectorIndex] = closestIndex;
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
        return findClosestCodebookEntry(dataVector, metric);
    }

    private CodebookEntry findClosestCodebookEntry(final int[] dataVector, final VectorDistanceMetric metric) {
        return codebookVectors[findClosestCodebookEntryIndex(dataVector, metric)];
    }

    private int findClosestCodebookEntryIndex(final int[] dataVector, final VectorDistanceMetric metric) {
        double minDist = Double.MAX_VALUE;
        int closestEntryIndex = 0;
        for (int entryIndex = 0; entryIndex < codebookVectors.length; entryIndex++) {


            final double dist = distanceBetweenVectors(dataVector, codebookVectors[entryIndex].getVector(), metric);
            if (dist < minDist) {
                minDist = dist;
                closestEntryIndex = entryIndex;
            }
        }

        return closestEntryIndex;
    }

    public CodebookEntry[] getCodebookVectors() {
        return codebookVectors;
    }

    public long[] getFrequencies() {
        return frequencies;
    }
}

