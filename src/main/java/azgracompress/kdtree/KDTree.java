package azgracompress.kdtree;

import java.util.Arrays;

// TODO(Moravec):   One more time read the paper and check the implementation!
//                  Fix the spreadest function (max-min) may be used.
//                  https://dl.acm.org/doi/pdf/10.1145/355744.355745

public class KDTree {
    private final int maximumBucketSize;
    private final KDNode root;

    private final int dimension;
    private final int totalNodeCount;
    private final int terminalNodeCount;

    public static class SearchInfo {
        private boolean continueSearching = true;
        private double nearestRecordDistance;
        private int[] nearestRecord = null;
        private final double[] coordinateUpperBound;
        private final double[] coordinateLowerBound;
        private final int dimension;

        public SearchInfo(final int dimension) {
            this.dimension = dimension;
            nearestRecordDistance = Double.POSITIVE_INFINITY;
            coordinateUpperBound = new double[dimension];
            coordinateLowerBound = new double[dimension];
            Arrays.fill(coordinateLowerBound, Double.NEGATIVE_INFINITY);
            Arrays.fill(coordinateUpperBound, Double.POSITIVE_INFINITY);
        }

        public int getDimension() {
            return dimension;
        }

        public double getNearestRecordDistance() {
            return nearestRecordDistance;
        }

        public int[] getNearestRecord() {
            return nearestRecord;
        }

        public double[] getUpperBounds() {
            return coordinateUpperBound;
        }

        public double[] getLowerBounds() {
            return coordinateLowerBound;
        }

        public boolean stopSearching() {
            return !continueSearching;
        }

        public void setContinueSearching(boolean continueSearching) {
            this.continueSearching = continueSearching;
        }

        public void setNearestRecord(final int[] record, final double recordDistance) {
            this.nearestRecord = record;
            this.nearestRecordDistance = recordDistance;
        }
    }

    public KDTree(final KDNode root,
                  final int dimension,
                  final int maximumBucketSize,
                  final int totalNodeCount,
                  final int terminalNodeCount) {
        this.root = root;
        this.dimension = dimension;
        this.maximumBucketSize = maximumBucketSize;
        this.totalNodeCount = totalNodeCount;
        this.terminalNodeCount = terminalNodeCount;
    }

    public int[] findNearestNeighbor(final int[] queryRecord) {
        SearchInfo searchInfo = new SearchInfo(dimension);
        root.findNearestNeighbor(queryRecord, searchInfo);
        return searchInfo.nearestRecord;
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public int getTerminalNodeCount() {
        return terminalNodeCount;
    }
}
