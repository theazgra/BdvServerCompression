package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageDecompressionException;
import azgracompress.data.*;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.huffman.Huffman;
import azgracompress.huffman.HuffmanNode;
import azgracompress.io.InBitStream;
import azgracompress.quantization.vector.CodebookEntry;
import azgracompress.quantization.vector.VQCodebook;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

// TODO(Moravec): Handle huffman decoding.

public class VQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {
    public VQImageDecompressor(CompressionOptions options) {
        super(options);
    }

    private long calculatePlaneVectorCount(final QCMPFileHeader header) {
        final int vectorXCount = (int) Math.ceil((double) header.getImageSizeX() / (double) header.getVectorSizeX());
        final int vectorYCount = (int) Math.ceil((double) header.getImageSizeY() / (double) header.getVectorSizeY());
        // Number of vectors per plane.
        return (vectorXCount * vectorYCount);
    }

    private long calculatePlaneDataSize(final long planeVectorCount, final int bpp) {
        // Data size of single plane indices.
        return (long) Math.ceil((planeVectorCount * bpp) / 8.0);
    }

    private VQCodebook readCodebook(DataInputStream compressedStream,
                                    final int codebookSize,
                                    final int vectorSize) throws ImageDecompressionException {

        final CodebookEntry[] codebookVectors = new CodebookEntry[codebookSize];
        final long[] frequencies = new long[codebookSize];
        try {
            for (int codebookIndex = 0; codebookIndex < codebookSize; codebookIndex++) {
                final int[] vector = new int[vectorSize];
                for (int vecIndex = 0; vecIndex < vectorSize; vecIndex++) {
                    vector[vecIndex] = compressedStream.readUnsignedShort();
                }
                codebookVectors[codebookIndex] = new CodebookEntry(vector);
            }
            for (int codebookIndex = 0; codebookIndex < codebookSize; codebookIndex++) {
                frequencies[codebookIndex] = compressedStream.readLong();
            }
        } catch (IOException ioEx) {
            throw new ImageDecompressionException("Unable to read quantization values from compressed stream.", ioEx);
        }

        // We don't care about vector dimensions in here.
        return new VQCodebook(new V3i(0), codebookVectors, frequencies);
    }


    private ImageU16 reconstructImageFromQuantizedVectors(final int[][] vectors,
                                                          final V2i qVector,
                                                          final V3i imageDims) {

        Chunk2D reconstructedChunk = new Chunk2D(new V2i(imageDims.getX(), imageDims.getY()), new V2l(0, 0));
        if (qVector.getY() > 1) {
            reconstructedChunk.reconstructFrom2DVectors(vectors, qVector);
        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk.asImageU16();
    }

    @Override
    public long getExpectedDataSize(QCMPFileHeader header) {
        // Vector count in codebook
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());

        // Single vector size in bytes.
        assert (header.getVectorSizeZ() == 1);
        final int vectorDataSize = 2 * header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();

        // Total codebook size in bytes.
        final long codebookDataSize = ((codebookSize * vectorDataSize) + (codebookSize * LONG_BYTES)) *
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
        assert (header.getVectorSizeZ() == 1);
        final int vectorSize = header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();
        final int planeCountForDecompression = header.getImageSizeZ();
        final long planeVectorCount = calculatePlaneVectorCount(header);
        //final long planeDataSize = calculatePlaneDataSize(planeVectorCount, header.getBitsPerPixel());
        final V2i qVector = new V2i(header.getVectorSizeX(), header.getVectorSizeY());
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);


        VQCodebook codebook = null;
        Huffman huffman = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            reportStatusToListeners("Loading codebook from cache...");
            codebook = readCodebook(compressedStream, codebookSize, vectorSize);
            huffman = createHuffmanCoder(huffmanSymbols, codebook.getVectorFrequencies());
        }

        Stopwatch stopwatch = new Stopwatch();
        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            stopwatch.restart();
            if (header.isCodebookPerPlane()) {
                reportStatusToListeners("Loading plane codebook...");
                codebook = readCodebook(compressedStream, codebookSize, vectorSize);
                huffman = createHuffmanCoder(huffmanSymbols, codebook.getVectorFrequencies());
            }
            assert (codebook != null && huffman != null);

            byte[] decompressedPlaneData = null;

            final int planeDataSize = (int) header.getPlaneDataSizes()[planeIndex];
            try (InBitStream inBitStream = new InBitStream(compressedStream,
                    header.getBitsPerCodebookIndex(),
                    planeDataSize)) {
                inBitStream.readToBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                int[][] decompressedVectors = new int[(int) planeVectorCount][vectorSize];
                for (int vecIndex = 0; vecIndex < planeVectorCount; vecIndex++) {
                    HuffmanNode currentHuffmanNode = huffman.getRoot();
                    boolean bit;
                    while (!currentHuffmanNode.isLeaf()) {
                        bit = inBitStream.readBit();
                        currentHuffmanNode = currentHuffmanNode.traverse(bit);
                    }
                    System.arraycopy(codebook.getVectors()[currentHuffmanNode.getSymbol()].getVector(),
                            0,
                            decompressedVectors[vecIndex],
                            0,
                            vectorSize);
                }


                final ImageU16 decompressedPlane = reconstructImageFromQuantizedVectors(decompressedVectors,
                        qVector,
                        header.getImageDims());
                decompressedPlaneData =
                        TypeConverter.unsignedShortArrayToByteArray(decompressedPlane.getData(), false);
            } catch (Exception ex) {
                throw new ImageDecompressionException("Unable to read indices from InBitStream.", ex);
            }


            try {
                decompressStream.write(decompressedPlaneData);
            } catch (IOException e) {
                throw new ImageDecompressionException("Unable to write decompressed data to decompress stream.", e);
            }

            stopwatch.stop();
            reportProgressListeners(planeIndex, planeCountForDecompression,
                    "Decompressed plane %d in %s", planeIndex, stopwatch.getElapsedTimeString());
        }
    }

    @Override
    public void decompressToBuffer(DataInputStream compressedStream, short[][] buffer, QCMPFileHeader header) throws ImageDecompressionException {
        // TODO: Think how to remove the duplicate code.
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        assert (header.getVectorSizeZ() == 1);
        final int vectorSize = header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();
        final int planeCountForDecompression = header.getImageSizeZ();
        final long planeVectorCount = calculatePlaneVectorCount(header);
        final V2i qVector = new V2i(header.getVectorSizeX(), header.getVectorSizeY());
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);


        VQCodebook codebook = null;
        Huffman huffman = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            codebook = readCodebook(compressedStream, codebookSize, vectorSize);
            huffman = createHuffmanCoder(huffmanSymbols, codebook.getVectorFrequencies());
        }

        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            if (header.isCodebookPerPlane()) {
                codebook = readCodebook(compressedStream, codebookSize, vectorSize);
                huffman = createHuffmanCoder(huffmanSymbols, codebook.getVectorFrequencies());
            }
            assert (codebook != null && huffman != null);



            final int planeDataSize = (int) header.getPlaneDataSizes()[planeIndex];
            try (InBitStream inBitStream = new InBitStream(compressedStream,
                    header.getBitsPerCodebookIndex(),
                    planeDataSize)) {
                inBitStream.readToBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                int[][] decompressedVectors = new int[(int) planeVectorCount][vectorSize];
                for (int vecIndex = 0; vecIndex < planeVectorCount; vecIndex++) {
                    HuffmanNode currentHuffmanNode = huffman.getRoot();
                    boolean bit;
                    while (!currentHuffmanNode.isLeaf()) {
                        bit = inBitStream.readBit();
                        currentHuffmanNode = currentHuffmanNode.traverse(bit);
                    }
                    System.arraycopy(codebook.getVectors()[currentHuffmanNode.getSymbol()].getVector(),
                            0,
                            decompressedVectors[vecIndex],
                            0,
                            vectorSize);
                }


                final ImageU16 decompressedPlane = reconstructImageFromQuantizedVectors(decompressedVectors,
                        qVector,
                        header.getImageDims());

                buffer[planeIndex] = TypeConverter.intArrayToShortArray(decompressedPlane.getData());
            } catch (Exception ex) {
                throw new ImageDecompressionException("Unable to read indices from InBitStream.", ex);
            }
            reportProgressListeners(planeIndex, planeCountForDecompression,
                    "Decompressed plane %d.", planeIndex);
        }
    }
}
