package azgracompress.compression;

import azgracompress.compression.exception.ImageDecompressionException;
import azgracompress.data.*;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.fileformat.QuantizationType;
import azgracompress.huffman.Huffman;
import azgracompress.huffman.HuffmanNode;
import azgracompress.io.InBitStream;
import azgracompress.quantization.vector.VQCodebook;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {

    private interface DecompressCallback {
        void process(final Block imageBlock, final int planeIndex) throws ImageDecompressionException;
    }

    private interface DecompressVoxelCallback {
        void process(final Voxel decompressedVoxel,
                     final int[][] decompressedVoxelData,
                     final int planeOffset) throws ImageDecompressionException;
    }

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

        final int[][] codebookVectors = new int[codebookSize][vectorSize];
        final long[] frequencies = new long[codebookSize];
        try {
            for (int codebookIndex = 0; codebookIndex < codebookSize; codebookIndex++) {
                //                final int[] vector = new int[vectorSize];
                for (int vecIndex = 0; vecIndex < vectorSize; vecIndex++) {
                    codebookVectors[codebookIndex][vecIndex] = compressedStream.readUnsignedShort();
                }
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


    private Block reconstructImageFromQuantizedVectors(final int[][] vectors,
                                                       final V2i qVector,
                                                       final V3i imageDims) {

        Block reconstructedChunk = new Block(new V2i(imageDims.getX(), imageDims.getY()));
        if (qVector.getY() > 1) {
            reconstructedChunk.reconstructFrom2DVectors(vectors, qVector);
        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk;
    }

    @Override
    public long getExpectedDataSize(QCMPFileHeader header) {
        // Vector count in codebook
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());

        // Single vector size in bytes.
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
        if (header.getQuantizationType() == QuantizationType.Vector3D) {
            decompressVoxels(compressedStream, decompressStream, header);
            return;
        }

        decompressImpl(compressedStream, header, (imageBlock, planeIndex) -> {
            try {
                decompressStream.write(TypeConverter.unsignedShortArrayToByteArray(imageBlock.getData(), false));
            } catch (IOException e) {
                throw new ImageDecompressionException("Unable to write decompressed data to decompress stream.", e);
            }
        });
    }

    public void decompressImpl(DataInputStream compressedStream,
                               QCMPFileHeader header,
                               DecompressCallback callback) throws ImageDecompressionException {
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
                    System.arraycopy(codebook.getVectors()[currentHuffmanNode.getSymbol()],
                                     0, decompressedVectors[vecIndex], 0, vectorSize);
                }


                final Block decompressedPlane = reconstructImageFromQuantizedVectors(decompressedVectors,
                                                                                     qVector,
                                                                                     header.getImageDims());

                callback.process(decompressedPlane, planeIndex);
            } catch (Exception ex) {
                throw new ImageDecompressionException("VQImageDecompressor::decompressToBuffer() - Unable to read indices from " +
                                                              "InBitStream.",
                                                      ex);
            }
            reportProgressToListeners(planeIndex, planeCountForDecompression,
                                      "Decompressed plane %d.", planeIndex);
        }
    }


    @Override
    public void decompressToBuffer(DataInputStream compressedStream,
                                   short[][] buffer,
                                   QCMPFileHeader header) throws ImageDecompressionException {
        if (header.getQuantizationType() == QuantizationType.Vector3D) {
            decompressVoxelsToBuffer(compressedStream, buffer, header);
            return;
        }
        decompressImpl(compressedStream, header, (imageBlock, planeIndex) -> {
            buffer[planeIndex] = TypeConverter.intArrayToShortArray(imageBlock.getData());
        });
    }


    private void decompressVoxelsImpl(DataInputStream compressedStream,
                                      QCMPFileHeader header,
                                      DecompressVoxelCallback callback) throws ImageDecompressionException {

        assert (header.getQuantizationType() == QuantizationType.Vector3D);
        assert (!header.isCodebookPerPlane()); // SHOULD ALWAYS BE GLOBAL.

        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final V3i voxelDims = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());
        final int vectorSize = (int) voxelDims.multiplyTogether();
        final int voxelLayerDepth = voxelDims.getZ();
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);


        final VQCodebook codebook = readCodebook(compressedStream, codebookSize, vectorSize);
        final Huffman huffman = createHuffmanCoder(huffmanSymbols, codebook.getVectorFrequencies());

        final int voxelLayerCount = VQImageCompressor.calculateVoxelLayerCount(header.getImageSizeZ(), header.getVectorSizeZ());
        Stopwatch stopwatch = new Stopwatch();
        for (int voxelLayerIndex = 0; voxelLayerIndex < voxelLayerCount; voxelLayerIndex++) {
            stopwatch.restart();

            final int fromZ = (voxelLayerIndex * voxelLayerDepth);
            final int toZ = (voxelLayerIndex == voxelLayerCount - 1)
                    ? header.getImageSizeZ()
                    : (voxelLayerDepth + (voxelLayerIndex * voxelLayerDepth));
            final V3i currentVoxelLayerDims = new V3i(header.getImageSizeX(), header.getImageSizeY(), toZ - fromZ);
            final int voxelLayerDataSize = (int) header.getPlaneDataSizes()[voxelLayerIndex];
            final int voxelLayerVoxelCount = Voxel.calculateRequiredVoxelCount(currentVoxelLayerDims, voxelDims);

            int[][] decompressedVoxels = new int[voxelLayerVoxelCount][vectorSize];

            try (InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerCodebookIndex(), voxelLayerDataSize)) {
                inBitStream.readToBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                for (int voxelIndex = 0; voxelIndex < voxelLayerVoxelCount; voxelIndex++) {
                    final int huffmanSymbol = decodeHuffmanSymbol(huffman, inBitStream);
                    System.arraycopy(codebook.getVectors()[huffmanSymbol], 0, decompressedVoxels[voxelIndex], 0, vectorSize);
                }

            } catch (Exception e) {
                throw new ImageDecompressionException("VQImageDecompressor::decompressVoxels() - Unable to read indices from InBitStream.",
                                                      e);
            }

            final Voxel currentVoxel = new Voxel(currentVoxelLayerDims);
            callback.process(currentVoxel, decompressedVoxels, (voxelLayerIndex * voxelLayerDepth));

            stopwatch.stop();
            if (options.isConsoleApplication()) {
                reportStatusToListeners("Decompressed voxel layer %d/%d in %s",
                                        voxelLayerIndex, voxelLayerCount, stopwatch.getElapsedTimeString());
            } else {
                reportProgressToListeners(voxelLayerIndex, voxelLayerCount,
                                          "Decompressed voxel layer %d/%d in %s",
                                          voxelLayerIndex, voxelLayerCount, stopwatch.getElapsedTimeString());
            }
        }
    }


    private void decompressVoxelsToBuffer(DataInputStream compressedStream,
                                          short[][] buffer,
                                          QCMPFileHeader header) throws ImageDecompressionException {

        final V3i voxelDims = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());

        decompressVoxelsImpl(compressedStream, header, (decompressedVoxel, decompressedVoxelData, planeOffset) ->
                decompressedVoxel.reconstructFromVoxels(voxelDims, decompressedVoxelData, buffer, planeOffset));
    }

    private void decompressVoxels(DataInputStream compressedStream,
                                  DataOutputStream decompressStream,
                                  QCMPFileHeader header) throws ImageDecompressionException {

        final V3i voxelDims = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());
        decompressVoxelsImpl(compressedStream, header, (voxel, voxelData, planeOffset) -> {

            ImageU16Dataset currentVoxelLayer = voxel.reconstructFromVoxelsToDataset(voxelDims, voxelData);

            for (int layer = 0; layer < voxel.getDims().getZ(); layer++) {
                try {
                    decompressStream.write(TypeConverter.unsignedShortArrayToByteArray(currentVoxelLayer.getPlaneData(layer), false));
                } catch (IOException e) {
                    throw new ImageDecompressionException("Unable to write to decompress stream.", e);
                }
            }
        });
    }

    private int decodeHuffmanSymbol(Huffman huffman, InBitStream inBitStream) throws IOException {
        HuffmanNode currentHuffmanNode = huffman.getRoot();
        while (!currentHuffmanNode.isLeaf()) {
            currentHuffmanNode = currentHuffmanNode.traverse(inBitStream.readBit());
        }
        return currentHuffmanNode.getSymbol();
    }
}
