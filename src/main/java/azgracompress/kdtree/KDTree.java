package azgracompress.kdtree;

import java.util.Arrays;

public class KDTree {
    private final int maximumBucketSize;
    private final KDNode root;

    private final int dimension;
    private final int totalNodeCount;
    private final int terminalNodeCount;

    public static class SearchInfo {
        private double currentClosestDistance;
        private int[] currentClosestRecord = null;
        private final double[] coordinateUpperBound;
        private final double[] coordinateLowerBound;
        private final int dimension;

        public SearchInfo(final int dimension) {
            this.dimension = dimension;
            currentClosestDistance = Double.POSITIVE_INFINITY;
            coordinateUpperBound = new double[dimension];
            coordinateLowerBound = new double[dimension];
            Arrays.fill(coordinateLowerBound, Double.NEGATIVE_INFINITY);
            Arrays.fill(coordinateUpperBound, Double.POSITIVE_INFINITY);
        }

        public int getDimension() {
            return dimension;
        }

        public double getCurrentClosestDistance() {
            return currentClosestDistance;
        }

        public void setCurrentClosestDistance(double currentClosestDistance) {
            this.currentClosestDistance = currentClosestDistance;
        }

        public int[] getCurrentClosestRecord() {
            return currentClosestRecord;
        }

        public void setCurrentClosestRecord(int[] currentClosestRecord) {
            this.currentClosestRecord = currentClosestRecord;
        }

        public double[] getUpperBounds() {
            return coordinateUpperBound;
        }

        public double[] getLowerBounds() {
            return coordinateLowerBound;
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
        // TODO(Moravec): Read more about Ball Within Bounds and Bounds Overlap Ball
        SearchInfo searchInfo = new SearchInfo(dimension);
        root.findNearestNeighbor(queryRecord, searchInfo);
        return searchInfo.currentClosestRecord;
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public int getTerminalNodeCount() {
        return terminalNodeCount;
    }
}
