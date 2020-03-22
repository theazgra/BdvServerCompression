package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageDecompressionException;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.huffman.Huffman;
import azgracompress.huffman.HuffmanNode;
import azgracompress.io.InBitStream;
import azgracompress.quantization.scalar.ScalarQuantizationCodebook;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {
    public SQImageDecompressor(ParsedCliOptions options) {
        super(options);
    }

    private ScalarQuantizationCodebook readScalarQuantizationValues(DataInputStream compressedStream) throws ImageDecompressionException {
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
        return new ScalarQuantizationCodebook(quantizationValues, symbolFrequencies);
    }

    @Override
    public long getExpectedDataSize(QCMPFileHeader header) {
        // Quantization value count.
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());

        // Total codebook size in bytes. Also symbol frequencies for Huffman.
        long codebookDataSize = ((2 * codebookSize) + (LONG_BYTES * codebookSize)) *
                (header.isCodebookPerPlane() ? header.getImageSizeZ() : 1);

        // Indices are encoded using huffman. Plane data size is written in the header.
        long[] planeDataSizes = header.getPlaneDataSizes();
        long totalPlaneDataSize = 0;
        for (final long planeDataSize : planeDataSizes) {
            totalPlaneDataSize += planeDataSize;
        }

        //        // Data size of single plane indices.
        //        final long planeIndicesDataSize =
        //                (long) Math.ceil(((header.getImageSizeX() * header.getImageSizeY()) * header
        //                .getBitsPerPixel()) / 8.0);
        //
        //        // All planes data size.
        //        final long allPlaneIndicesDataSize = planeIndicesDataSize * header.getImageSizeZ();

        return (codebookDataSize + totalPlaneDataSize);
    }

    @Override
    public void decompress(DataInputStream compressedStream,
                           DataOutputStream decompressStream,
                           QCMPFileHeader header) throws ImageDecompressionException {

        final int[] huffmanSymbols = createHuffmanSymbols();
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();
        final int planeIndicesDataSize = (int) Math.ceil((planePixelCount * header.getBitsPerPixel()) / 8.0);

        int[] quantizationValues = null;
        Huffman huffman = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            huffman = null;
            // TODO(Moravec): Handle loading of Huffman.
            Log("Loading codebook from cache...");
            //quantizationValues = readScalarQuantizationValues(compressedStream, codebookSize);
        }

        Stopwatch stopwatch = new Stopwatch();
        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            stopwatch.restart();
            if (header.isCodebookPerPlane()) {
                Log("Loading plane codebook...");
                ScalarQuantizationCodebook codebook = readScalarQuantizationValues(compressedStream);
                quantizationValues = codebook.getCentroids();
                huffman = new Huffman(huffmanSymbols, codebook.getSymbolFrequencies());
                huffman.buildHuffmanTree();
            }
            assert (quantizationValues != null);
            assert (huffman != null);

            Log(String.format("Decompressing plane %d...", planeIndex));
            byte[] decompressedPlaneData = null;
            final int planeDataSize = (int) header.getPlaneDataSizes()[planeIndex];
            try (InBitStream inBitStream = new InBitStream(compressedStream,
                                                           header.getBitsPerPixel(),
                                                           planeDataSize)) {
                inBitStream.readToBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                int[] decompressedValues = new int[planePixelCount];
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
            Log(String.format("Decompressed plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString()));
        }

    }
}
