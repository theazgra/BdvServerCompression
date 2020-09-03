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

    public KDTreeBuilder(final int dimension, final int bucketSize) {
        this.bucketSize = bucketSize;
        this.dimension = dimension;
    }


    public KDNode buildTree(final int[][] records) {
        if (records.length <= bucketSize) {
            return makeTerminalNode(records);
        }

        double maxSpread = -1.0;
        int keyIndex = 0;

        for (int j = 0; j < dimension; j++) {
            // Find coordinate with greatest spread.
            final double greatestSpread = calculateKeySpread(records, j);
            if (greatestSpread > maxSpread) {
                maxSpread = greatestSpread;
                keyIndex = j;
            }
        }
        final int median = calculateKeyMedian(records, keyIndex);


        // Divide records in one method to hi and lo.
        final DividedRecords dividedRecords = divideRecords(records, median, keyIndex);
        return makeNonTerminalNode(keyIndex, median, dividedRecords);
    }


    private DividedRecords divideRecords(final int[][] records, final int median, final int keyIndex) {
        ArrayList<int[]> loRecords = new ArrayList<>();
        ArrayList<int[]> hiRecords = new ArrayList<>();
        for (final int[] record : records) {
            if (record[keyIndex] <= median) {
                loRecords.add(record);
            } else {
                hiRecords.add(record);
            }
        }
        return new DividedRecords(loRecords.toArray(new int[0][]), hiRecords.toArray(new int[0][]));
    }

    private KDNode makeNonTerminalNode(final int keyIndex, final int median, final DividedRecords dividedRecords) {
        final KDNode loSon = buildTree(dividedRecords.getLoRecords());
        final KDNode hiSon = buildTree(dividedRecords.getHiRecords());
        ++nodeCount;
        return new KDNode(keyIndex, median, loSon, hiSon);
    }

    public KDNode makeTerminalNode(final int[][] records) {
        ++nodeCount;
        ++terminalNodeCount;
        return new TerminalKDNode(records);
    }

    private int calculateKeyMedian(final int[][] records, final int keyIndex) {
        assert (records.length > 1);
        final int[] sortedArray = new int[records.length];
        for (int i = 0; i < records.length; i++) {
            sortedArray[i] = records[i][keyIndex];
        }
        Arrays.sort(sortedArray);

        final int midIndex = sortedArray.length / 2;
        if ((sortedArray.length % 2) == 0) {
            return (int) (((double) sortedArray[midIndex] + (double) sortedArray[(midIndex - 1)]) / 2.0);
        } else {
            return sortedArray[midIndex];
        }
    }


    private double calculateKeySpread(final int[][] records, final int keyIndex) {
        double center = 0.0;
        for (final int[] record : records) {
            center += record[keyIndex];
        }
        center /= (double) records.length;

        double spread = 0.0;

        for (final int[] record : records) {
            spread += Math.pow(((double) center - (double) record[keyIndex]), 2);
        }

        return Math.sqrt(spread);
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getTerminalNodeCount() {
        return terminalNodeCount;
    }
}
