package azgracompress.kdtree;

import azgracompress.utilities.Utils;

public class TerminalKDNode extends KDNode {

    private final int[] bucketIndices;

    public TerminalKDNode(final int[] bucketIndices) {
        super();
        this.bucketIndices = bucketIndices;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    public int[] getBucketIndices() {
        return bucketIndices;
    }

    public void findNearestNeighborInBucket(final int[] queryRecord, final KDTree.BBFSearchInfo searchInfo) {
        double recordDistance;
        for (final int index : bucketIndices) {
            recordDistance = Utils.calculateEuclideanDistance(queryRecord, searchInfo.getFeatureVectors()[index]);
            if (recordDistance < searchInfo.getNearestVectorDistance()) {
                searchInfo.setNearestRecord(index, recordDistance);
            }
        }
    }
}
