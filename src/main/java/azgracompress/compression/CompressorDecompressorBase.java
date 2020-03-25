package azgracompress.compression;

import azgracompress.cli.InputFileInfo;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.huffman.Huffman;
import azgracompress.io.OutBitStream;

import java.io.DataOutputStream;

public abstract class CompressorDecompressorBase {
    public static final int LONG_BYTES = 8;
    public static final String EXTENSION = ".QCMP";
    public static final String RAW_EXTENSION_NO_DOT = "raw";

    protected final ParsedCliOptions options;
    private final int codebookSize;

    public CompressorDecompressorBase(ParsedCliOptions options) {
        this.options = options;
        this.codebookSize = (int) Math.pow(2, this.options.getBitsPerPixel());
    }

    protected int[] createHuffmanSymbols(final int codebookSize) {
        int[] symbols = new int[codebookSize];
        for (int i = 0; i < codebookSize; i++) {
            symbols[i] = i;
        }
        return symbols;
    }

    protected Huffman createHuffmanCoder(final int[] symbols, final long[] frequencies) {
        Huffman huffman = new Huffman(symbols, frequencies);
        huffman.buildHuffmanTree();
        return huffman;
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

    /**
     * Get index of the middle plane.
     *
     * @return Index of the middle plane.
     */
    protected int getMiddlePlaneIndex() {
            return (options.getInputFileInfo().getDimensions().getZ() / 2);
    }

    /**
     * Write huffman encoded indices to the compress stream.
     *
     * @param compressStream Compress stream.
     * @param huffman        Huffman encoder.
     * @param indices        Indices to write.
     * @return Number of bytes written.
     * @throws ImageCompressionException when fails to write to compress stream.
     */
    protected long writeHuffmanEncodedIndices(DataOutputStream compressStream,
                                              final Huffman huffman,
                                              final int[] indices) throws ImageCompressionException {
        try (OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerPixel(), 2048)) {
            for (final int index : indices) {
                outBitStream.write(huffman.getCode(index));
            }
            return outBitStream.getBytesWritten();
        } catch (Exception ex) {
            throw new ImageCompressionException("Unable to write indices to OutBitStream.", ex);
        }
    }

    protected int getCodebookSize() {
        return codebookSize;
    }
}
