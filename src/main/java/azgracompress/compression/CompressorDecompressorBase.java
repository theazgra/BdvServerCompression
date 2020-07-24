package azgracompress.compression;

import azgracompress.compression.listeners.IProgressListener;
import azgracompress.compression.listeners.IStatusListener;
import azgracompress.io.InputData;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.huffman.Huffman;
import azgracompress.io.OutBitStream;

import java.io.DataOutputStream;

public abstract class CompressorDecompressorBase {
    public static final int LONG_BYTES = 8;
    public static final String EXTENSION = ".QCMP";
    public static final String RAW_EXTENSION_NO_DOT = "raw";

    protected final CompressionOptions options;
    private final int codebookSize;

    private IStatusListener statusListener = null;
    private IProgressListener progressListener = null;

    public CompressorDecompressorBase(CompressionOptions options) {
        this.options = options;
        this.codebookSize = (int) Math.pow(2, this.options.getBitsPerCodebookIndex());
        // Default status listener, which can be override by setStatusListener.
        statusListener = this::defaultLog;
    }

    public void setStatusListener(IStatusListener listener) {
        this.statusListener = listener;
    }

    public void setProgressListener(IProgressListener listener) {
        this.progressListener = listener;
    }

    protected IProgressListener getProgressListener() {
        return progressListener;
    }

    protected IStatusListener getStatusListener() {
        return statusListener;
    }

    protected void reportStatusToListeners(final String status) {
        if (this.statusListener != null) {
            this.statusListener.sendMessage(status);
        }
    }

    protected void reportStatusToListeners(final String format, final Object... args) {
        reportStatusToListeners(String.format(format, args));
    }

    protected void reportProgressListeners(final int index, final int finalIndex, final String message) {
        if (this.progressListener != null) {
            this.progressListener.sendProgress(message, index, finalIndex);
        }
    }

    protected void reportProgressListeners(final int index,
                                           final int finalIndex,
                                           final String message,
                                           final Object... args) {
        reportProgressListeners(index, finalIndex, String.format(message, args));
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

//        if (options.isVerbose()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("Huffman symbols and their probabilities:\n");
//
//            Iterator<Map.Entry<Integer, Double>> it = huffman.getSymbolProbabilityMap().entrySet().iterator();
//            while (it.hasNext()) {
//                final Map.Entry<Integer, Double> pair = (Map.Entry<Integer, Double>) it.next();
//
//                sb.append(String.format("%d: %.10f\n", pair.getKey(), pair.getValue()));
//            }
//            System.out.println(sb.toString());
//        }

        return huffman;
    }

    protected int[] getPlaneIndicesForCompression() {

        final InputData ifi = options.getInputDataInfo();
        if (ifi.isPlaneIndexSet()) {
            return new int[]{ifi.getPlaneIndex()};
        } else if (ifi.isPlaneRangeSet()) {
            final int from = ifi.getPlaneRange().getFrom();
            final int count = ifi.getPlaneRange().getInclusiveTo() - from;

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


    private void defaultLog(final String message) {
        if (options.isVerbose()) {
            System.out.println(message);
        }
    }

    /**
     * Get index of the middle plane.
     *
     * @return Index of the middle plane.
     */
    protected int getMiddlePlaneIndex() {
        return (options.getInputDataInfo().getDimensions().getZ() / 2);
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
        try (OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerCodebookIndex(), 2048)) {
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
