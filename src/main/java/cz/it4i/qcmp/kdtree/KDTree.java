package cz.it4i.qcmp.kdtree;

import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;

// TODO(Moravec):   One more time read the paper and check the implementation!
//                  Fix the spreadest function (max-min) may be used.
//                  https://dl.acm.org/doi/pdf/10.1145/355744.355745
//                  Actually get rid of this kdTree and look into BBF (Best Bin First)
//                  https://www.cs.ubc.ca/~lowe/papers/cvpr97.pdf
//                  https://github.com/iwyoo/kd_tree/blob/master/kd_tree.cxx

public class KDTree {
    private final int[][] featureVectors;
    private final int maximumBucketSize;
    private final KDNode root;

    private final int dimension;
    private final int totalNodeCount;
    private final int terminalNodeCount;

    public static class BBFSearchInfo {
        private final int[][] featureVectors;
        private double nearestVectorDistance;
        private int nearestVectorIndex;

        public BBFSearchInfo(final int[][] featureVectors) {
            this.featureVectors = featureVectors;
            nearestVectorIndex = -1;
            nearestVectorDistance = Double.POSITIVE_INFINITY;
        }

        public int[][] getFeatureVectors() {
            return featureVectors;
        }

        public double getNearestVectorDistance() {
            return nearestVectorDistance;
        }

        public int getNearestVectorIndex() {
            return nearestVectorIndex;
        }

        public void setNearestRecord(final int vectorIndex, final double recordDistance) {
            this.nearestVectorIndex = vectorIndex;
            this.nearestVectorDistance = recordDistance;
        }
    }

    private static class NodeWithDistance implements Comparable<NodeWithDistance> {
        private final KDNode node;
        private final double distance;

        private NodeWithDistance(final KDNode node, final double distance) {
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
        public int compareTo(@NotNull final KDTree.NodeWithDistance o) {
            return Double.compare(distance, o.distance);
        }
    }

    public KDTree(final int[][] featureVectors,
                  final KDNode root,
                  final int maximumBucketSize,
                  final int totalNodeCount,
                  final int terminalNodeCount) {
        this.featureVectors = featureVectors;
        this.root = root;
        this.dimension = featureVectors[0].length;
        this.maximumBucketSize = maximumBucketSize;
        this.totalNodeCount = totalNodeCount;
        this.terminalNodeCount = terminalNodeCount;
    }

    public int findNearestBBF(final int[] queryVector, final int maxE) {

        final PriorityQueue<NodeWithDistance> priorityQueue = new PriorityQueue<>();
        priorityQueue.add(new NodeWithDistance(root, 0.0));

        final BBFSearchInfo searchInfo = new BBFSearchInfo(featureVectors);
        int tryIndex = 0;
        int partition, discriminator;
        while (!priorityQueue.isEmpty() && tryIndex < maxE) {
            final NodeWithDistance current = priorityQueue.remove();
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
        return searchInfo.getNearestVectorIndex();
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public int getTerminalNodeCount() {
        return terminalNodeCount;
    }
}
