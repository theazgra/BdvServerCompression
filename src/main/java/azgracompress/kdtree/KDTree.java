package azgracompress.kdtree;

import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;

// TODO(Moravec):   One more time read the paper and check the implementation!
//                  Fix the spreadest function (max-min) may be used.
//                  https://dl.acm.org/doi/pdf/10.1145/355744.355745
//                  Actually get rid of this kdTree and look into BBF (Best Bin First)
//                  https://www.cs.ubc.ca/~lowe/papers/cvpr97.pdf
//                  https://github.com/iwyoo/kd_tree/blob/master/kd_tree.cxx

public class KDTree {
    private final int maximumBucketSize;
    private final KDNode root;

    private final int dimension;
    private final int totalNodeCount;
    private final int terminalNodeCount;

    public static class BBFSearchInfo {
        private double nearestRecordDistance;
        private int[] nearestRecord;

        public BBFSearchInfo() {
            nearestRecord = null;
            nearestRecordDistance = Double.POSITIVE_INFINITY;
        }

        public double getNearestRecordDistance() {
            return nearestRecordDistance;
        }

        public int[] getNearestRecord() {
            return nearestRecord;
        }

        public void setNearestRecord(final int[] record, final double recordDistance) {
            this.nearestRecord = record;
            this.nearestRecordDistance = recordDistance;
        }
    }

    private static class NodeWithDistance implements Comparable<NodeWithDistance> {
        private final KDNode node;
        private final double distance;

        private NodeWithDistance(KDNode node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        public KDNode getNode() {
            return node;
        }

        public double getDistance() {
            return distance;
        }

        @Override
        public int compareTo(@NotNull KDTree.NodeWithDistance o) {
            return Double.compare(distance, o.distance);
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

    public int[] findNearestBBF(final int[] queryVector, final int maxE) {

        PriorityQueue<NodeWithDistance> priorityQueue = new PriorityQueue<>();
        priorityQueue.add(new NodeWithDistance(root, 0.0));

        BBFSearchInfo searchInfo = new BBFSearchInfo();
        int tryIndex = 0;
        int partition, discriminator;
        while (!priorityQueue.isEmpty() && tryIndex < maxE) {
            NodeWithDistance current = priorityQueue.remove();
            if (current.getNode().isTerminal()) {
                ((TerminalKDNode) current.getNode()).findNearestNeighborInBucket(queryVector, searchInfo);
                ++tryIndex;
            } else {
                discriminator = current.getNode().getDiscriminator();
                partition = current.getNode().getPartition();
                if (queryVector[discriminator] < partition) {
                    priorityQueue.add(new NodeWithDistance(current.getNode().getLoSon(),
                                                           0.0));
                    priorityQueue.add(new NodeWithDistance(current.getNode().getHiSon(),
                                                           (double) partition - (double) queryVector[discriminator]));
                } else {
                    priorityQueue.add(new NodeWithDistance(current.getNode().getHiSon(),
                                                           0.0));
                    priorityQueue.add(new NodeWithDistance(current.getNode().getLoSon(),
                                                           (double) queryVector[discriminator] - (double) partition));
                }
            }
        }
        return searchInfo.getNearestRecord();
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public int getTerminalNodeCount() {
        return terminalNodeCount;
    }
}
