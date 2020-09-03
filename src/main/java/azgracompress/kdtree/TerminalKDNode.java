package azgracompress.kdtree;

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
}
