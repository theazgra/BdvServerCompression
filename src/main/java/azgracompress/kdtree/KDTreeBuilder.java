package azgracompress.kdtree;

import java.util.ArrayList;
import java.util.Arrays;

public class KDTreeBuilder {
    private static class DividedRecords {
        private final int[] loIndices;
        private final int[] hiIndices;

        DividedRecords(final int[] loIndices, final int[] hiIndices) {
            this.loIndices = loIndices;
            this.hiIndices = hiIndices;
        }

        public int[] getHiIndices() {
            return hiIndices;
        }

        public int[] getLoIndices() {
            return loIndices;
        }
    }

    private int[][] featureVectors;
    private final int bucketSize;
    private final int dimension;
    private int nodeCount = 0;
    private int terminalNodeCount = 0;

    /**
     * Create KDTree builder.
     *
     * @param dimension  Dimension of the feature vectors.
     * @param bucketSize Bucket size for the terminal nodes.
     */
    public KDTreeBuilder(final int dimension, final int bucketSize) {
        this.bucketSize = bucketSize;
        this.dimension = dimension;
    }

    /**
     * Construct the KDTree for provided feature vectors.
     *
     * @param featureVectors Feature vectors to build the tree with
     * @return KDTree.
     */
    public KDTree buildTree(final int[][] featureVectors) {
        nodeCount = 0;
        terminalNodeCount = 0;
        this.featureVectors = featureVectors;
        final int[] indices = new int[featureVectors.length];
        for (int i = 0; i < featureVectors.length; i++) {
            indices[i] = i;
        }

        final KDNode rootNode = buildTreeImpl(indices);
        return new KDTree(featureVectors, rootNode, bucketSize, nodeCount, terminalNodeCount);
    }

    /**
     * Build KDTree by recursion, feature vectors are split in the dimension with the greatest variance.
     *
     * @param indices Indices of feature vectors to build the tree with.
     * @return Node with its siblings.
     */
    private KDNode buildTreeImpl(final int[] indices) {
        if (indices.length <= bucketSize) {
            return makeTerminalNode(indices);
        }

        int dimensionIndex = findDimensionWithGreatestVariance(indices);
        final int median = calculateKeyMedian(indices, dimensionIndex);

        // Divide records in one method to hi and lo.
        final DividedRecords dividedRecords = divideRecords(indices, median, dimensionIndex);
        return makeNonTerminalNode(dimensionIndex, median, dividedRecords);
    }


    /**
     * Divide feature vectors into low and high subgroups.
     *
     * @param indices   Indices of feature vectors to divide into two groups.
     * @param median    Median in the dimension.
     * @param dimension Dimension index.
     * @return Divided vectors.
     */
    private DividedRecords divideRecords(final int[] indices, final int median, final int dimension) {
        ArrayList<Integer> loIndices = new ArrayList<>();
        ArrayList<Integer> hiIndices = new ArrayList<>();

        for (final int fVecIndex : indices) {
            if (featureVectors[fVecIndex][dimension] <= median) {
                loIndices.add(fVecIndex);
            } else {

                hiIndices.add(fVecIndex);
            }
        }

        return new DividedRecords(loIndices.stream().mapToInt(Integer::intValue).toArray(),
                                  hiIndices.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Create internal tree node by recursion on buildTreeImpl.
     *
     * @param dimension      Dimension to split at.
     * @param median         Median in the selected dimension.
     * @param dividedRecords Records divided by the median.
     * @return New internal node.
     */
    private KDNode makeNonTerminalNode(final int dimension, final int median, final DividedRecords dividedRecords) {
        final KDNode loSon = buildTreeImpl(dividedRecords.getLoIndices());
        final KDNode hiSon = buildTreeImpl(dividedRecords.getHiIndices());
        ++nodeCount;
        return new KDNode(dimension, median, loSon, hiSon);
    }

    /**
     * Construct terminal node with bucket of feature vectors.
     *
     * @param bucketIndices Indices of feature vectors to be stored in the leaf/terminal node.
     * @return New terminal node.
     */
    public KDNode makeTerminalNode(final int[] bucketIndices) {
        ++nodeCount;
        ++terminalNodeCount;
        return new TerminalKDNode(bucketIndices);
    }

    /**
     * Find the dimension with the greatest variance for the feature vectors.
     *
     * @param indices Indices of feature vectors.
     * @return Index of the dimension with greatest variance/spread.
     */
    private int findDimensionWithGreatestVariance(final int[] indices) {
        double maxVar = -1.0;
        int dimension = 0;
        for (int j = 0; j < this.dimension; j++) {
            // Find coordinate with greatest spread.
            final double dimVar = calculateDimensionVariance(indices, j);
            if (dimVar > maxVar) {
                maxVar = dimVar;
                dimension = j;
            }
        }
        return dimension;
    }

    /**
     * Calculate the median in selected dimension.
     *
     * @param indices   Indices of feature vectors.
     * @param dimension Dimension index.
     * @return Median of the dimension.
     */
    private int calculateKeyMedian(final int[] indices, final int dimension) {
        assert (indices.length > 1);
        final int[] sortedArray = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            sortedArray[i] = featureVectors[indices[i]][dimension];
        }
        Arrays.sort(sortedArray);

        final int midIndex = sortedArray.length / 2;
        if ((sortedArray.length % 2) == 0) {
            return (int) (((double) sortedArray[midIndex] + (double) sortedArray[(midIndex - 1)]) / 2.0);
        } else {
            return sortedArray[midIndex];
        }
    }


    /**
     * Calculate variance of the values in selected dimension.
     *
     * @param indices   Indices of feature vectors.
     * @param dimension Dimension index.
     * @return Variance in the dimension.
     */
    private double calculateDimensionVariance(final int[] indices, final int dimension) {
        double mean = 0.0;
        for (final int fVecIndex : indices) {
            mean += featureVectors[fVecIndex][dimension];
        }
        mean /= (double) indices.length;

        double var = 0.0;

        for (final int fVecIndex : indices) {
            var += Math.pow(((double) featureVectors[fVecIndex][dimension] - mean), 2);
        }
        return (var / (double) indices.length);
    }

}
