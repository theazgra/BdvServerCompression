package azgracompress.compression;

import azgracompress.compression.exception.ImageDecompressionException;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.huffman.Huffman;
import azgracompress.huffman.HuffmanNode;
import azgracompress.io.InBitStream;
import azgracompress.quantization.scalar.SQCodebook;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {
    public SQImageDecompressor(CompressionOptions options) {
        super(options);
    }

    private SQCodebook readScalarQuantizationValues(DataInputStream compressedStream,
                                                    final int codebookSize) throws ImageDecompressionException {
        int[] quantizationValues = new int[codebookSize];
        long[] symbolFrequencies = new long[codebookSize];
        try {
            for (int i = 0; i < codebookSize; i++) {
                quantizationValues[i] = compressedStream.readUnsignedShort();
            }
            for (int i = 0; i < codebookSize; i++) {
                symbolFrequencies[i] = compressedStream.readLong();
            }
        } catch (IOException ioEx) {
            throw new ImageDecompressionException("Unable to read quantization values from compressed stream.", ioEx);
        }
        return new SQCodebook(quantizationValues, symbolFrequencies);
    }

    @Override
    public long getExpectedDataSize(QCMPFileHeader header) {
        // Quantization value count.
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());

        // Total codebook size in bytes. Also symbol frequencies for Huffman.
        long codebookDataSize = ((2 * codebookSize) + (LONG_BYTES * codebookSize)) *
                (header.isCodebookPerPlane() ? header.getImageSizeZ() : 1);

        // Indices are encoded using huffman. Plane data size is written in the header.
        long[] planeDataSizes = header.getPlaneDataSizes();
        long totalPlaneDataSize = 0;
        for (final long planeDataSize : planeDataSizes) {
            totalPlaneDataSize += planeDataSize;
        }

        return (codebookDataSize + totalPlaneDataSize);
    }

    @Override
    public void decompress(DataInputStream compressedStream,
                           DataOutputStream decompressStream,
                           QCMPFileHeader header) throws ImageDecompressionException {

        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();

        SQCodebook codebook = null;
        Huffman huffman = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            reportStatusToListeners("Loading single codebook and huffman coder.");
            codebook = readScalarQuantizationValues(compressedStream, codebookSize);
            huffman = createHuffmanCoder(huffmanSymbols, codebook.getSymbolFrequencies());
        }

        Stopwatch stopwatch = new Stopwatch();
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
            try (InBitStream inBitStream = new InBitStream(compressedStream,
                    header.getBitsPerCodebookIndex(),
                    planeDataSize)) {
                inBitStream.readToBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                int[] decompressedValues = new int[planePixelCount];
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


            } catch (Exception ex) {
                throw new ImageDecompressionException("Unable to read indices from InBitStream.", ex);
            }
            try {
                decompressStream.write(decompressedPlaneData);
            } catch (IOException e) {
                throw new ImageDecompressionException("Unable to write decompressed data to decompress stream.", e);
            }

            stopwatch.stop();
            reportStatusToListeners(String.format("Decompressed plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString()));
        }
    }

    @Override
    public void decompressToBuffer(DataInputStream compressedStream, short[][] buffer, QCMPFileHeader header) throws ImageDecompressionException {
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();

        SQCodebook codebook = null;
        Huffman huffman = null;
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
            try (InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerCodebookIndex(), planeDataSize)) {
                inBitStream.readToBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                int[] decompressedValues = new int[planePixelCount];
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
            } catch (Exception ex) {
                throw new ImageDecompressionException("Unable to read indices from InBitStream.", ex);
            }
        }
    }
}
