package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.compression.exception.ImageDecompressionException;
import cz.it4i.qcmp.fileformat.IQvcFile;
import cz.it4i.qcmp.fileformat.QCMPFileHeaderV1;
import cz.it4i.qcmp.fileformat.SqQvcFile;
import cz.it4i.qcmp.huffman.HuffmanDecoder;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;
import cz.it4i.qcmp.io.InBitStream;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;
import cz.it4i.qcmp.utilities.Stopwatch;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {
    private SQCodebook cachedCodebook = null;
    private HuffmanDecoder cachedHuffmanDecoder = null;

    public SQImageDecompressor(final CompressionOptions options) {
        super(options);
    }

    private SQCodebook readSqCodebook(final DataInputStream compressedStream, final int codebookSize) throws ImageDecompressionException {
        final int[] quantizationValues = new int[codebookSize];
        final long[] symbolFrequencies = new long[codebookSize];
        try {
            for (int i = 0; i < codebookSize; i++) {
                quantizationValues[i] = compressedStream.readUnsignedShort();
            }
            // TODO(Moravec): Read frequencies or binary huffman tree based on file format version!!!
            for (int i = 0; i < codebookSize; i++) {
                symbolFrequencies[i] = compressedStream.readLong();
            }
        } catch (final IOException ioEx) {
            throw new ImageDecompressionException("Unable to read quantization values from compressed stream.", ioEx);
        }
        final HuffmanTreeBuilder builder = new HuffmanTreeBuilder(createHuffmanSymbols(codebookSize), symbolFrequencies);
        builder.buildHuffmanTree();
        return new SQCodebook(quantizationValues, builder.getRoot());
    }

    @Override
    public void decompress(final DataInputStream compressedStream,
                           final DataOutputStream decompressStream,
                           final QCMPFileHeaderV1 header) throws ImageDecompressionException {

        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();

        SQCodebook codebook = null;
        HuffmanDecoder huffmanDecoder = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            reportStatusToListeners("Loading single codebook and huffman coder.");
            codebook = readSqCodebook(compressedStream, codebookSize);
            huffmanDecoder = codebook.getHuffmanDecoder();
        }

        final Stopwatch stopwatch = new Stopwatch();
        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            stopwatch.restart();
            if (header.isCodebookPerPlane()) {
                reportStatusToListeners("Loading plane codebook...");
                codebook = readSqCodebook(compressedStream, codebookSize);
                huffmanDecoder = codebook.getHuffmanDecoder();
            }
            assert (codebook != null && huffmanDecoder != null);

            reportStatusToListeners(String.format("Decompressing plane %d...", planeIndex));
            byte[] decompressedPlaneData = null;
            final int planeDataSize = (int) header.getChunkDataSizes()[planeIndex];
            try (final InBitStream inBitStream = new InBitStream(compressedStream,
                                                                 header.getBitsPerCodebookIndex(),
                                                                 planeDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                final int[] decompressedValues = new int[planePixelCount];
                final int[] quantizationValues = codebook.getCentroids();
                for (int pixel = 0; pixel < planePixelCount; pixel++) {
                    final int decodedSymbol = huffmanDecoder.decodeSymbol(inBitStream);
                    decompressedValues[pixel] = quantizationValues[decodedSymbol];
                }

                decompressedPlaneData =
                        TypeConverter.unsignedShortArrayToByteArray(decompressedValues, false);


            } catch (final Exception ex) {
                throw new ImageDecompressionException("SQImageDecompressor::decompress() - Unable to read indices from InBitStream.", ex);
            }
            try {
                decompressStream.write(decompressedPlaneData);
            } catch (final IOException e) {
                throw new ImageDecompressionException("Unable to write decompressed data to decompress stream.", e);
            }

            stopwatch.stop();
            reportStatusToListeners(String.format("Decompressed plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString()));
        }
    }

    @Override
    public void preloadGlobalCodebook(final IQvcFile codebookCacheFile) {
        assert (codebookCacheFile instanceof SqQvcFile) : "Incorrect codebook cache file type for SQImageDecompressor";

        final SqQvcFile codebookCache = (SqQvcFile) codebookCacheFile;

        cachedCodebook = codebookCache.getCodebook();
        cachedHuffmanDecoder = cachedCodebook.getHuffmanDecoder();
    }

    @Override
    public void decompressToBuffer(final DataInputStream compressedStream,
                                   final short[][] buffer,
                                   final QCMPFileHeaderV1 header) throws ImageDecompressionException {
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();

        SQCodebook codebook = null;
        HuffmanDecoder huffmanDecoder = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            codebook = readSqCodebook(compressedStream, codebookSize);
            huffmanDecoder = codebook.getHuffmanDecoder();
        }

        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            reportProgressToListeners(planeIndex, planeCountForDecompression, "Decompressing plane %d", planeIndex);
            if (header.isCodebookPerPlane()) {
                codebook = readSqCodebook(compressedStream, codebookSize);
                huffmanDecoder = codebook.getHuffmanDecoder();
            }
            assert (codebook != null && huffmanDecoder != null);

            final int planeDataSize = (int) header.getChunkDataSizes()[planeIndex];
            try (final InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerCodebookIndex(), planeDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                final int[] decompressedValues = new int[planePixelCount];
                final int[] quantizationValues = codebook.getCentroids();
                for (int pixel = 0; pixel < planePixelCount; pixel++) {
                    final int decodedSymbol = huffmanDecoder.decodeSymbol(inBitStream);
                    decompressedValues[pixel] = quantizationValues[decodedSymbol];
                }

                buffer[planeIndex] = TypeConverter.intArrayToShortArray(decompressedValues);
            } catch (final Exception ex) {
                throw new ImageDecompressionException("SQImageDecompressor::decompressToBuffer() - Unable to read indices from " +
                                                              "InBitStream.",
                                                      ex);
            }
        }
    }

    @Override
    public short[] decompressStreamMode(final DataInputStream compressedStream,
                                        final QCMPFileHeaderV1 header) throws ImageDecompressionException {
        throw new ImageDecompressionException("Not implemented yet.");
    }
}
