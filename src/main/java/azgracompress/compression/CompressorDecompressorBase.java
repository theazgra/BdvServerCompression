package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.io.RawDataIO;

import java.io.IOException;

public abstract class CompressorDecompressorBase {
    public static final String EXTENSTION = ".QCMP";

    protected final ParsedCliOptions options;
    protected final int codebookSize;

    public CompressorDecompressorBase(ParsedCliOptions options) {
        this.options = options;
        this.codebookSize = (int) Math.pow(2, this.options.getBitsPerPixel());
    }

    protected int[] getPlaneIndicesForCompression() {
        if (options.hasPlaneIndexSet()) {
            return new int[]{options.getPlaneIndex()};
        } else if (options.hasPlaneRangeSet()) {
            final int from = options.getFromPlaneIndex();
            final int to = options.getToPlaneIndex();
            final int count = to - from;

            int[] indices = new int[count + 1];
            for (int i = 0; i <= count; i++) {
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

    protected int[] loadConfiguredPlanesData() throws ImageCompressionException {
        int[] trainData = null;
        if (options.hasPlaneIndexSet()) {
            try {
                Log("Loading single plane data.");
                trainData = RawDataIO.loadImageU16(options.getInputFile(),
                                                   options.getImageDimension(),
                                                   options.getPlaneIndex()).getData();
            } catch (IOException e) {
                throw new ImageCompressionException("Failed to load reference image data.", e);
            }
        } else if (options.hasPlaneRangeSet()) {
            Log("Loading plane range data.");
            final int[] planes = getPlaneIndicesForCompression();
            try {
                trainData = RawDataIO.loadPlanesData(options.getInputFile(), options.getImageDimension(), planes);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ImageCompressionException("Failed to load plane range data.", e);
            }
        } else {
            Log("Loading all planes data.");
            try {
                trainData = RawDataIO.loadAllPlanesData(options.getInputFile(), options.getImageDimension());
            } catch (IOException e) {
                throw new ImageCompressionException("Failed to load all planes data.", e);
            }
        }
        return trainData;
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
