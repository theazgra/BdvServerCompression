package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.compression.exception.ImageCompressionException;
import cz.it4i.qcmp.compression.listeners.IProgressListener;
import cz.it4i.qcmp.compression.listeners.IStatusListener;
import cz.it4i.qcmp.huffman.HuffmanDecoder;
import cz.it4i.qcmp.huffman.HuffmanEncoder;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;
import cz.it4i.qcmp.io.InputData;
import cz.it4i.qcmp.io.OutBitStream;

import java.io.DataOutputStream;
import java.util.ArrayList;

public abstract class CompressorDecompressorBase {
    public static final int LONG_BYTES = 8;
    public static final String EXTENSION = ".QCMP";
    public static final String RAW_EXTENSION_NO_DOT = "raw";

    protected final CompressionOptions options;
    private final int codebookSize;

    private ArrayList<IStatusListener> statusListeners;
    private ArrayList<IProgressListener> progressListeners;

    public CompressorDecompressorBase(final CompressionOptions options) {
        this.options = options;
        this.codebookSize = (int) Math.pow(2, this.options.getBitsPerCodebookIndex());
    }

    public int getBitsPerCodebookIndex() {
        return this.options.getBitsPerCodebookIndex();
    }

    public void addStatusListener(final IStatusListener listener) {
        if (statusListeners == null) {
            statusListeners = new ArrayList<>(1);
        }
        statusListeners.add(listener);
    }

    public void addProgressListener(final IProgressListener listener) {
        if (this.progressListeners == null) {
            this.progressListeners = new ArrayList<>(1);
        }
        this.progressListeners.add(listener);
    }

    public void clearAllListeners() {
        this.statusListeners.clear();
        this.progressListeners.clear();
    }

    protected void duplicateAllListeners(final IListenable other) {
        if (other == this)
            return;

        if (this.statusListeners != null) {
            for (final IStatusListener statusListener : this.statusListeners) {
                other.addStatusListener(statusListener);
            }
        }
        if (this.progressListeners != null) {
            for (final IProgressListener progressListener : this.progressListeners) {
                other.addProgressListener(progressListener);
            }
        }
    }

    protected void reportStatusToListeners(final String status) {
        if (this.statusListeners != null) {
            for (final IStatusListener listener : this.statusListeners) {
                listener.sendMessage(status);
            }
        }
    }

    protected void reportStatusToListeners(final String format, final Object... args) {
        reportStatusToListeners(String.format(format, args));
    }

    protected void reportProgressToListeners(final int index, final int finalIndex, final String message) {
        if (this.progressListeners != null) {
            for (final IProgressListener listener : this.progressListeners) {
                listener.sendProgress(message, index, finalIndex);
            }
        }
    }

    protected void reportProgressToListeners(final int index,
                                             final int finalIndex,
                                             final String message,
                                             final Object... args) {
        reportProgressToListeners(index, finalIndex, String.format(message, args));
    }

    protected int[] createHuffmanSymbols(final int codebookSize) {
        final int[] symbols = new int[codebookSize];
        for (int i = 0; i < codebookSize; i++) {
            symbols[i] = i;
        }
        return symbols;
    }

    protected HuffmanEncoder createHuffmanEncoder(final int[] symbols, final long[] frequencies) {
        final HuffmanTreeBuilder huffman = new HuffmanTreeBuilder(symbols, frequencies);
        huffman.buildHuffmanTree();
        return huffman.createEncoder();
    }

    protected HuffmanDecoder createHuffmanDecoder(final int[] symbols, final long[] frequencies) {
        final HuffmanTreeBuilder huffman = new HuffmanTreeBuilder(symbols, frequencies);
        huffman.buildHuffmanTree();
        return huffman.createDecoder();
    }

    protected int[] getPlaneIndicesForCompression(final InputData inputData) {
        if (inputData.isPlaneIndexSet()) {
            return new int[]{inputData.getPlaneIndex()};
        } else if (inputData.isPlaneRangeSet()) {
            final int from = inputData.getPlaneRange().getFrom();
            final int count = inputData.getPlaneRange().getTo() - from;

            final int[] indices = new int[count + 1];
            for (int i = 0; i <= count; i++) {
                indices[i] = from + i;
            }
            return indices;
        } else {
            return generateAllPlaneIndices(inputData.getDimensions().getPlaneCount());
        }
    }

    private int[] generateAllPlaneIndices(final int planeCount) {
        final int[] planeIndices = new int[planeCount];
        for (int i = 0; i < planeCount; i++) {
            planeIndices[i] = i;
        }
        return planeIndices;
    }


    public void defaultLog(final String message) {
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
        return (options.getInputDataInfo().getDimensions().getPlaneCount() / 2);
    }

    /**
     * Write huffman encoded indices to the compress stream.
     *
     * @param compressStream Compress stream.
     * @param huffmanEncoder Huffman encoder.
     * @param indices        Indices to write.
     * @return Number of bytes written.
     * @throws ImageCompressionException when fails to write to compress stream.
     */
    protected long writeHuffmanEncodedIndices(final DataOutputStream compressStream,
                                              final HuffmanEncoder huffmanEncoder,
                                              final int[] indices) throws ImageCompressionException {
        try (final OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerCodebookIndex(), 2048)) {
            for (final int index : indices) {
                outBitStream.write(huffmanEncoder.getSymbolCode(index));
            }
            return outBitStream.getBytesWritten();
        } catch (final Exception ex) {
            throw new ImageCompressionException("Unable to write indices to OutBitStream.", ex);
        }
    }

    protected int getCodebookSize() {
        return codebookSize;
    }
}
