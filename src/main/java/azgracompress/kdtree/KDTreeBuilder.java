package azgracompress.kdtree;

import java.util.ArrayList;
import java.util.Arrays;

public class KDTreeBuilder {
    private static class DividedRecords {
        private final int[][] hiRecords;
        private final int[][] loRecords;

        DividedRecords(int[][] hiRecords, int[][] loRecords) {
            this.hiRecords = hiRecords;
            this.loRecords = loRecords;
        }

        public int[][] getHiRecords() {
            return hiRecords;
        }

        public int[][] getLoRecords() {
            return loRecords;
        }
    }

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
        final KDNode rootNode = buildTreeImpl(featureVectors);
        return new KDTree(rootNode, dimension, bucketSize, nodeCount, terminalNodeCount);
    }

    /**
     * Build KDTree by recursion, feature vectors are split in the dimension with the greatest variance.
     *
     * @param featureVectors Feature vectors to build the tree with.
     * @return Node with its siblings.
     */
    public KDNode buildTreeImpl(final int[][] featureVectors) {
        if (featureVectors.length <= bucketSize) {
            return makeTerminalNode(featureVectors);
        }

        int keyIndexMSE = findDimensionWithGreatestVariance(featureVectors);
        final int median = calculateKeyMedian(featureVectors, keyIndexMSE);

        // Divide records in one method to hi and lo.
        final DividedRecords dividedRecords = divideRecords(featureVectors, median, keyIndexMSE);
        return makeNonTerminalNode(keyIndexMSE, median, dividedRecords);
    }


    /**
     * Divide feature vectors into low and high subgroups.
     *
     * @param featureVectors Feature vectors to divide.
     * @param median         Median in the dimension.
     * @param dimension      Dimension index.
     * @return Divided vectors.
     */
    private DividedRecords divideRecords(final int[][] featureVectors, final int median, final int dimension) {
        ArrayList<int[]> loRecords = new ArrayList<>();
        ArrayList<int[]> hiRecords = new ArrayList<>();
        for (final int[] record : featureVectors) {
            if (record[dimension] <= median) {
                loRecords.add(record);
            } else {
                hiRecords.add(record);
            }
        }
        return new DividedRecords(loRecords.toArray(new int[0][]), hiRecords.toArray(new int[0][]));
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
        final KDNode loSon = buildTreeImpl(dividedRecords.getLoRecords());
        final KDNode hiSon = buildTreeImpl(dividedRecords.getHiRecords());
        ++nodeCount;
        return new KDNode(dimension, median, loSon, hiSon);
    }

    /**
     * Construct terminal node with bucket of feature vectors.
     *
     * @param featureVectors Feature vectors.
     * @return New terminal node.
     */
    public KDNode makeTerminalNode(final int[][] featureVectors) {
        ++nodeCount;
        ++terminalNodeCount;
        System.out.printf("Terminal node bucket size: %d\n", featureVectors.length);
        return new TerminalKDNode(featureVectors);
    }

    /**
     * Find the dimension with the greatest variance for the feature vectors.
     *
     * @param featureVectors Feature vectors.
     * @return Index of the dimension with greatest variance/spread.
     */
    private int findDimensionWithGreatestVariance(final int[][] featureVectors) {
        double maxVar = -1.0;
        int dimension = 0;
        for (int j = 0; j < this.dimension; j++) {
            // Find coordinate with greatest spread.
            final double dimVar = calculateDimensionVariance(featureVectors, j);
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
     * @param featureVectors Feature vectors.
     * @param dimension      Dimension index.
     * @return Median of the dimension.
     */
    private int calculateKeyMedian(final int[][] featureVectors, final int dimension) {
        assert (featureVectors.length > 1);
        final int[] sortedArray = new int[featureVectors.length];
        for (int i = 0; i < featureVectors.length; i++) {
            sortedArray[i] = featureVectors[i][dimension];
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
     * @param featureVectors Feature vectors.
     * @param dimension      Dimension index.
     * @return Variance in the dimension.
     */
    private double calculateDimensionVariance(final int[][] featureVectors, final int dimension) {
        double mean = 0.0;
        for (final int[] record : featureVectors) {
            mean += record[dimension];
        }
        mean /= (double) featureVectors.length;

        double var = 0.0;

        for (final int[] record : featureVectors) {
            var += Math.pow(((double) record[dimension] - mean), 2);
        }
        return (var / (double) featureVectors.length);
    }

}
