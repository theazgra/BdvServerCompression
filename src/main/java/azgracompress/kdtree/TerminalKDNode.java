package azgracompress.kdtree;

import azgracompress.utilities.Utils;

public class TerminalKDNode extends KDNode {

    private final int[][] bucket;

    public TerminalKDNode(final int[][] records) {
        super();
        this.bucket = records;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    public int[][] getBucket() {
        return bucket;
    }

    public void findNearestNeighborInBucket(final int[] queryRecord, final KDTree.SearchInfo searchInfo) {
        double recordDistance;
        for (final int[] record : bucket) {
            recordDistance = Utils.calculateEuclideanDistance(queryRecord, record);
            if (recordDistance < searchInfo.getNearestRecordDistance()) {
                searchInfo.setNearestRecord(record, recordDistance);
            }
        }
    }
}
