package compression;

import cli.ParsedCliOptions;

public class CompressorDecompressorBase {
    public static final String EXTENSTION = ".QCMP";

    protected final ParsedCliOptions options;

    public CompressorDecompressorBase(ParsedCliOptions options) {
        this.options = options;
    }

    protected int[] getPlaneIndicesForCompression() {
        if (options.hasPlaneIndexSet()) {
            return new int[]{options.getPlaneIndex()};
        } else if (options.hasPlaneRangeSet()) {
            final int from = options.getFromPlaneIndex();
            final int to = options.getToPlaneIndex();
            final int count = to - from;

            int[] indices = new int[count];
            for (int i = 0; i < count; i++) {
                indices[i] = from + i;
            }
            return indices;
        } else {
            return generateAllPlaneIndices(options.getImageDimension().getZ());
        }
    }

    private int[] generateAllPlaneIndices(final int planeCount) {
        int[] planeIndices = new int[planeCount];
        for (int i = 0; i < planeCount; i++) {
            planeIndices[i] = i;
        }
        return planeIndices;
    }

    protected void Log(final String message) {
        if (options.isVerbose()) {
            System.out.println(message);
        }
    }

    protected void DebugLog(final String message) {
        System.out.println(message);
    }

    protected void LogError(final String message) {
        if (options.isVerbose()) {
            System.err.println(message);
        }
    }
}
