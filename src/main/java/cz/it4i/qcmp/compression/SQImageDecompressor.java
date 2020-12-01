package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.cache.ICacheFile;
import cz.it4i.qcmp.cache.SQCacheFile;
import cz.it4i.qcmp.compression.exception.ImageDecompressionException;
import cz.it4i.qcmp.fileformat.QCMPFileHeader;
import cz.it4i.qcmp.huffman.HuffmanNode;
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
    private HuffmanTreeBuilder cachedHuffman = null;

    public SQImageDecompressor(final CompressionOptions options) {
        super(options);
    }

    private SQCodebook readScalarQuantizationValues(final DataInputStream compressedStream,
                                                    final int codebookSize) throws ImageDecompressionException {
        final int[] quantizationValues = new int[codebookSize];
        final long[] symbolFrequencies = new long[codebookSize];
        try {
            for (int i = 0; i < codebookSize; i++) {
                quantizationValues[i] = compressedStream.readUnsignedShort();
            }
            for (int i = 0; i < codebookSize; i++) {
                symbolFrequencies[i] = compressedStream.readLong();
            }
        } catch (final IOException ioEx) {
            throw new ImageDecompressionException("Unable to read quantization values from compressed stream.", ioEx);
        }
        return new SQCodebook(quantizationValues, symbolFrequencies);
    }

    @Override
    public void decompress(final DataInputStream compressedStream,
                           final DataOutputStream decompressStream,
                           final QCMPFileHeader header) throws ImageDecompressionException {

        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();

        SQCodebook codebook = null;
        HuffmanTreeBuilder huffman = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            reportStatusToListeners("Loading single codebook and huffman coder.");
            codebook = readScalarQuantizationValues(compressedStream, codebookSize);
            huffman = createHuffmanCoder(huffmanSymbols, codebook.getSymbolFrequencies());
        }

        final Stopwatch stopwatch = new Stopwatch();
        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            stopwatch.restart();
            if (header.isCodebookPerPlane()) {
                reportStatusToListeners("Loading plane codebook...");
                codebook = readScalarQuantizationValues(compressedStream, codebookSize);
                huffman = createHuffmanCoder(huffmanSymbols, codebook.getSymbolFrequencies());
            }
            assert (codebook != null && huffman != null);

            reportStatusToListeners(String.format("Decompressing plane %d...", planeIndex));
            byte[] decompressedPlaneData = null;
            final int planeDataSize = (int) header.getPlaneDataSizes()[planeIndex];
            try (final InBitStream inBitStream = new InBitStream(compressedStream,
                                                                 header.getBitsPerCodebookIndex(),
                                                                 planeDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                final int[] decompressedValues = new int[planePixelCount];
                final int[] quantizationValues = codebook.getCentroids();
                for (int pixel = 0; pixel < planePixelCount; pixel++) {
                    HuffmanNode currentHuffmanNode = huffman.getRoot();
                    boolean bit;
                    while (!currentHuffmanNode.isLeaf()) {
                        bit = inBitStream.readBit();
                        currentHuffmanNode = currentHuffmanNode.traverse(bit);
                    }
                    decompressedValues[pixel] = quantizationValues[currentHuffmanNode.getSymbol()];
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
    public void preloadGlobalCodebook(final ICacheFile codebookCacheFile) {
        assert (codebookCacheFile instanceof SQCacheFile) : "Incorrect codebook cache file type for SQImageDecompressor";

        final SQCacheFile codebookCache = (SQCacheFile) codebookCacheFile;

        cachedCodebook = codebookCache.getCodebook();
        cachedHuffman = createHuffmanCoder(createHuffmanSymbols(cachedCodebook.getCodebookSize()), cachedCodebook.getSymbolFrequencies());
    }

    @Override
    public void decompressToBuffer(final DataInputStream compressedStream,
                                   final short[][] buffer,
                                   final QCMPFileHeader header) throws ImageDecompressionException {
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();

        SQCodebook codebook = null;
        HuffmanTreeBuilder huffman = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            codebook = readScalarQuantizationValues(compressedStream, codebookSize);
            huffman = createHuffmanCoder(huffmanSymbols, codebook.getSymbolFrequencies());
        }

        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            reportProgressToListeners(planeIndex, planeCountForDecompression, "Decompressing plane %d", planeIndex);
            if (header.isCodebookPerPlane()) {
                codebook = readScalarQuantizationValues(compressedStream, codebookSize);
                huffman = createHuffmanCoder(huffmanSymbols, codebook.getSymbolFrequencies());
            }
            assert (codebook != null && huffman != null);

            final int planeDataSize = (int) header.getPlaneDataSizes()[planeIndex];
            try (final InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerCodebookIndex(), planeDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                final int[] decompressedValues = new int[planePixelCount];
                final int[] quantizationValues = codebook.getCentroids();
                for (int pixel = 0; pixel < planePixelCount; pixel++) {
                    HuffmanNode currentHuffmanNode = huffman.getRoot();
                    boolean bit;
                    while (!currentHuffmanNode.isLeaf()) {
                        bit = inBitStream.readBit();
                        currentHuffmanNode = currentHuffmanNode.traverse(bit);
                    }
                    decompressedValues[pixel] = quantizationValues[currentHuffmanNode.getSymbol()];
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
                                        final QCMPFileHeader header) throws ImageDecompressionException {
        throw new ImageDecompressionException("Not implemented yet.");
    }
}
