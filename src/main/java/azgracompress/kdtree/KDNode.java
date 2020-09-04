package azgracompress.kdtree;

public class KDNode {
    private final int discriminator;
    private final int partition;

    private final KDNode loSon;
    private final KDNode hiSon;

    protected KDNode() {
        discriminator = -1;
        partition = -1;
        loSon = null;
        hiSon = null;
    }


    public KDNode(final int keyIndex, final int median, final KDNode loSon, final KDNode hiSon) {
        this.discriminator = keyIndex;
        this.partition = median;
        this.loSon = loSon;
        this.hiSon = hiSon;
    }

    public final KDNode getLoSon() {
        return loSon;
    }

    public final KDNode getHiSon() {
        return hiSon;
    }

    public final int getDiscriminator() {
        return discriminator;
    }

    public final int getPartition() {
        return partition;
    }

    public boolean isTerminal() {
        return false;
    }

    public void findNearestNeighbor(final int[] queryRecord, final KDTree.SearchInfo searchInfo) {

        if (searchInfo.stopSearching())
            return;

        if (isTerminal()) {
            ((TerminalKDNode) this).findNearestNeighborInBucket(queryRecord, searchInfo);

            if (ballWithinBounds(queryRecord, searchInfo)) {
                searchInfo.setContinueSearching(false);
            }
            return;
        }

        assert (loSon != null && hiSon != null);
        if (queryRecord[discriminator] <= partition) {
            double tmp = searchInfo.getUpperBounds()[discriminator];
            searchInfo.getUpperBounds()[discriminator] = partition;
            loSon.findNearestNeighbor(queryRecord, searchInfo);
            searchInfo.getUpperBounds()[discriminator] = tmp;

        } else {
            double tmp = searchInfo.getLowerBounds()[discriminator];
            searchInfo.getLowerBounds()[discriminator] = partition;
            hiSon.findNearestNeighbor(queryRecord, searchInfo);
            searchInfo.getLowerBounds()[discriminator] = tmp;
        }
        if (searchInfo.stopSearching())
            return;


        if (queryRecord[discriminator] <= partition) {
            double tmp = searchInfo.getLowerBounds()[discriminator];
            searchInfo.getLowerBounds()[discriminator] = partition;
            if (boundsOverlapBall(queryRecord, searchInfo)) {
                hiSon.findNearestNeighbor(queryRecord, searchInfo);
            }
            searchInfo.getLowerBounds()[discriminator] = tmp;
        } else {
            double tmp = searchInfo.getUpperBounds()[discriminator];
            searchInfo.getUpperBounds()[discriminator] = partition;
            if (boundsOverlapBall(queryRecord, searchInfo)) {
                loSon.findNearestNeighbor(queryRecord, searchInfo);
            }
            searchInfo.getUpperBounds()[discriminator] = tmp;
        }
        if (searchInfo.stopSearching())
            return;

        if (ballWithinBounds(queryRecord, searchInfo)) {
            searchInfo.setContinueSearching(false);
        }
    }

    private static double coordinateDistance(final double x, final double y) {
        return Math.pow((x - y), 2);
    }

    private static double dissimilarity(final double value) {
        return Math.sqrt(value);
    }

    private boolean ballWithinBounds(final int[] queryRecord, final KDTree.SearchInfo searchInfo) {
        double lbDist, ubDist;
        for (int d = 0; d < searchInfo.getDimension(); d++) {
            lbDist = coordinateDistance(searchInfo.getLowerBounds()[d], queryRecord[d]);
            ubDist = coordinateDistance(searchInfo.getUpperBounds()[d], queryRecord[d]);
            if ((lbDist <= searchInfo.getNearestRecordDistance()) || (ubDist <= searchInfo.getNearestRecordDistance())) {
                return false;
            }
        }
        return true;
    }

    private boolean boundsOverlapBall(final int[] queryRecord, final KDTree.SearchInfo searchInfo) {
        double sum = 0.0;
        for (int d = 0; d < searchInfo.getDimension(); d++) {
            if (queryRecord[d] < searchInfo.getLowerBounds()[d]) {
                sum += coordinateDistance(queryRecord[d], searchInfo.getLowerBounds()[d]);
                if (dissimilarity(sum) > searchInfo.getNearestRecordDistance()) {
                    return true;
                }
            } else if (queryRecord[d] > searchInfo.getUpperBounds()[d]) {
                sum += coordinateDistance(queryRecord[d], searchInfo.getUpperBounds()[d]);
                if (dissimilarity(sum) > searchInfo.getNearestRecordDistance()) {
                    return true;
                }
            }
        }
        return false;
    }
}
