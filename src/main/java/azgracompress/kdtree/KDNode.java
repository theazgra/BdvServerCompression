package azgracompress.kdtree;

public class KDNode {
    private final int keyIndex;
    private final int median;

    private final KDNode loSon;
    private final KDNode hiSon;

    protected KDNode() {
        keyIndex = -1;
        median = -1;
        loSon = null;
        hiSon = null;
    }


    public KDNode(final int keyIndex, final int median, final KDNode loSon, final KDNode hiSon) {
        this.keyIndex = keyIndex;
        this.median = median;
        this.loSon = loSon;
        this.hiSon = hiSon;
    }

    public final KDNode getLoSon() {
        return loSon;
    }

    public final KDNode getHiSon() {
        return hiSon;
    }

    public final int getKeyIndex() {
        return keyIndex;
    }

    public final int getMedian() {
        return median;
    }

    public boolean isTerminal() {
        return false;
    }
}
