package azgracompress.compression;

import azgracompress.cli.InputFileInfo;
import azgracompress.cli.ParsedCliOptions;

public abstract class CompressorDecompressorBase {
    public static final String EXTENSION = ".QCMP";

    protected final ParsedCliOptions options;
    protected final int codebookSize;

    public CompressorDecompressorBase(ParsedCliOptions options) {
        this.options = options;
        this.codebookSize = (int) Math.pow(2, this.options.getBitsPerPixel());
    }

    protected int[] getPlaneIndicesForCompression() {
        final InputFileInfo ifi = options.getInputFileInfo();
        if (ifi.isPlaneIndexSet()) {
            return new int[]{ifi.getPlaneIndex()};
        } else if (ifi.isPlaneRangeSet()) {
            final int from = ifi.getPlaneRange().getX();
            final int to = ifi.getPlaneRange().getY();
            final int count = to - from;

            int[] indices = new int[count + 1];
            for (int i = 0; i <= count; i++) {
                indices[i] = from + i;
            }
            return indices;
        } else {
            return generateAllPlaneIndices(ifi.getDimensions().getZ());
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

    protected void Log(final String format, final Object... args) {
        if (options.isVerbose()) {
            System.out.println(String.format(format, args));
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
