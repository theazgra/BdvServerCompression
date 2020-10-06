package cz.it4i.qcmp.kdtree;

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

    private static double coordinateDistance(final double x, final double y) {
        return Math.pow((x - y), 2);
    }

    private static double dissimilarity(final double value) {
        return Math.sqrt(value);
    }

}
