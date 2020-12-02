package cz.it4i.qcmp.quantization.vector;

import cz.it4i.qcmp.utilities.Utils;

public class VectorQuantizer {

    private interface QuantizeVectorMethod {
        int call(final int[] vector);
    }

    private final VectorDistanceMetric metric = VectorDistanceMetric.Euclidean;
    private final int[][] codebookVectors;
    private final int vectorSize;
    private final VQCodebook codebook;

    // private final KDTree kdTree;

    public VectorQuantizer(final VQCodebook codebook) {
        this.codebook = codebook;
        this.codebookVectors = codebook.getVectors();
        this.vectorSize = codebook.getVectors()[0].length;
        // kdTree = new KDTreeBuilder(this.vectorSize, 8).buildTree(codebook.getVectors());
    }

    public int[] quantize(final int[] dataVector) {
        assert (dataVector.length > 0 && dataVector.length % vectorSize == 0) : "Wrong vector size";
        return findClosestCodebookEntry(dataVector, metric);
    }

    public int quantizeToIndex(final int[] dataVector) {
        assert (dataVector.length > 0 && dataVector.length % vectorSize == 0) : "Wrong vector size";
        return findClosestCodebookEntryIndex(dataVector, metric);
    }

    public int[][] quantize(final int[][] dataVectors, final int workerCount) {
        assert (dataVectors.length > 0 && dataVectors[0].length % vectorSize == 0) : "Wrong vector size";
        final int[][] result = new int[dataVectors.length][vectorSize];

        if (workerCount == 1) {
            for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
                result[vectorIndex] = findClosestCodebookEntry(dataVectors[vectorIndex], metric);
            }
        } else {
            final int[] indices = quantizeIntoIndices(dataVectors, workerCount);
            for (int i = 0; i < dataVectors.length; i++) {
                result[i] = codebookVectors[indices[i]];
            }
        }

        return result;
    }

    public int[] quantizeIntoIndices(final int[][] dataVectors) {
        return quantizeIntoIndices(dataVectors, 1);
    }

    private int[] quantizeIntoIndicesImpl(final int[][] dataVectors,
                                          final int maxWorkerCount,
                                          final QuantizeVectorMethod method) {


        assert (dataVectors.length > 0 && dataVectors[0].length == vectorSize) : "Wrong vector size";
        final int[] indices = new int[dataVectors.length];

        if (maxWorkerCount == 1) {
            for (int vectorIndex = 0; vectorIndex < dataVectors.length; vectorIndex++) {
                indices[vectorIndex] = method.call(dataVectors[vectorIndex]);
            }
        } else {
            // Cap the worker count on 8
            final int workerCount = Math.min(maxWorkerCount, 8);
            final Thread[] workers = new Thread[workerCount];
            final int workSize = dataVectors.length / workerCount;

            for (int wId = 0; wId < workerCount; wId++) {
                final int fromIndex = wId * workSize;
                final int toIndex = (wId == workerCount - 1) ? dataVectors.length : (workSize + (wId * workSize));

                workers[wId] = new Thread(() -> {
                    for (int vectorIndex = fromIndex; vectorIndex < toIndex; vectorIndex++) {
                        indices[vectorIndex] = method.call(dataVectors[vectorIndex]);
                    }
                });

                workers[wId].start();
            }
            try {
                for (int wId = 0; wId < workerCount; wId++) {
                    workers[wId].join();
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        return indices;
    }

    //    public int[] quantizeIntoIndicesUsingKDTree(final int[][] dataVectors, final int maxWorkerCount) {
    //
    //        return quantizeIntoIndicesImpl(dataVectors, maxWorkerCount, (final int[] queryVector) ->
    //                kdTree.findNearestBBF(queryVector, 8));
    //    }

    public int[] quantizeIntoIndices(final int[][] dataVectors, final int maxWorkerCount) {

        return quantizeIntoIndicesImpl(dataVectors, maxWorkerCount, (final int[] queryVector) ->
                findClosestCodebookEntryIndex(queryVector, metric));
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
                return Utils.calculateEuclideanDistance(originalDataVector, codebookEntry);
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

    private int[] findClosestCodebookEntry(final int[] dataVector) {
        return findClosestCodebookEntry(dataVector, metric);
    }

    private int[] findClosestCodebookEntry(final int[] dataVector, final VectorDistanceMetric metric) {
        return codebookVectors[findClosestCodebookEntryIndex(dataVector, metric)];
    }

    private int findClosestCodebookEntryIndex(final int[] dataVector, final VectorDistanceMetric metric) {
        double minDist = Double.MAX_VALUE;
        int closestEntryIndex = 0;
        for (int entryIndex = 0; entryIndex < codebookVectors.length; entryIndex++) {


            final double dist = distanceBetweenVectors(dataVector, codebookVectors[entryIndex], metric);
            if (dist < minDist) {
                minDist = dist;
                closestEntryIndex = entryIndex;
            }
        }

        return closestEntryIndex;
    }

    public int[][] getCodebookVectors() {
        return codebookVectors;
    }

    public VQCodebook getCodebook() {
        return codebook;
    }
}

