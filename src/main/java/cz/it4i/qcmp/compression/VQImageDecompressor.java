package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.compression.exception.ImageDecompressionException;
import cz.it4i.qcmp.data.*;
import cz.it4i.qcmp.fileformat.IQvcFile;
import cz.it4i.qcmp.fileformat.QCMPFileHeaderV1;
import cz.it4i.qcmp.fileformat.QuantizationType;
import cz.it4i.qcmp.fileformat.VqQvcFile;
import cz.it4i.qcmp.huffman.HuffmanDecoder;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;
import cz.it4i.qcmp.io.InBitStream;
import cz.it4i.qcmp.quantization.vector.VQCodebook;
import cz.it4i.qcmp.utilities.Stopwatch;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {

    private VQCodebook cachedCodebook = null;
    private HuffmanDecoder cachedHuffmanDecoder = null;

    private interface DecompressCallback {
        void process(final Block imageBlock, final int planeIndex) throws ImageDecompressionException;
    }

    private interface DecompressVoxelCallback {
        void process(final Voxel decompressedVoxel,
                     final int[][] decompressedVoxelData,
                     final int planeOffset) throws ImageDecompressionException;
    }

    public VQImageDecompressor(final CompressionOptions options) {
        super(options);
    }

    private long calculatePlaneVectorCount(final QCMPFileHeaderV1 header) {
        final long vectorXCount = (long) Math.ceil((double) header.getImageSizeX() / (double) header.getVectorSizeX());
        final long vectorYCount = (long) Math.ceil((double) header.getImageSizeY() / (double) header.getVectorSizeY());
        // Number of vectors per plane.
        return (vectorXCount * vectorYCount);
    }

    private VQCodebook readCodebook(final DataInputStream compressedStream,
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
            // TODO(Moravec): Read frequencies or binary huffman tree based on file format version!!!
            for (int codebookIndex = 0; codebookIndex < codebookSize; codebookIndex++) {
                frequencies[codebookIndex] = compressedStream.readLong();
            }
        } catch (final IOException ioEx) {
            throw new ImageDecompressionException("Unable to read quantization values from compressed stream.", ioEx);
        }

        final HuffmanTreeBuilder builder = new HuffmanTreeBuilder(createHuffmanSymbols(codebookSize), frequencies);
        builder.buildHuffmanTree();

        // We don't care about vector dimensions in here.
        return new VQCodebook(new V3i(0), codebookVectors, builder.getRoot());
    }

    @Override
    public void preloadGlobalCodebook(final IQvcFile codebookCacheFile) {
        assert (codebookCacheFile instanceof VqQvcFile) : "Incorrect codebook cache file type for VQImageDecompressor";
        final VqQvcFile codebookCache = (VqQvcFile) codebookCacheFile;

        cachedCodebook = codebookCache.getCodebook();
        cachedHuffmanDecoder = cachedCodebook.getHuffmanDecoder();
    }


    private Block reconstructImageFromQuantizedVectors(final int[][] vectors,
                                                       final V2i qVector,
                                                       final V3i imageDims) {

        final Block reconstructedChunk = new Block(new V2i(imageDims.getX(), imageDims.getY()));
        if (qVector.getY() > 1) {
            reconstructedChunk.reconstructFrom2DVectors(vectors, qVector);
        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk;
    }

    @Override
    public void decompress(final DataInputStream compressedStream,
                           final DataOutputStream decompressStream,
                           final QCMPFileHeaderV1 header) throws ImageDecompressionException {
        if (header.getQuantizationType() == QuantizationType.Vector3D) {
            decompressVoxels(compressedStream, decompressStream, header);
            return;
        }

        decompressImpl(compressedStream, header, (imageBlock, planeIndex) -> {
            try {
                decompressStream.write(TypeConverter.unsignedShortArrayToByteArray(imageBlock.getData(), false));
            } catch (final IOException e) {
                throw new ImageDecompressionException("Unable to write decompressed data to decompress stream.", e);
            }
        });
    }

    public void decompressImpl(final DataInputStream compressedStream,
                               final QCMPFileHeaderV1 header,
                               final DecompressCallback callback) throws ImageDecompressionException {
        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        assert (header.getVectorSizeZ() == 1);
        final int vectorSize = header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();
        final int planeCountForDecompression = header.getImageSizeZ();
        final long planeVectorCount = calculatePlaneVectorCount(header);
        final V2i qVector = new V2i(header.getVectorSizeX(), header.getVectorSizeY());

        VQCodebook codebook = null;
        HuffmanDecoder huffmanDecoder = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            codebook = readCodebook(compressedStream, codebookSize, vectorSize);
            huffmanDecoder = codebook.getHuffmanDecoder();
        }

        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            if (header.isCodebookPerPlane()) {
                codebook = readCodebook(compressedStream, codebookSize, vectorSize);
                huffmanDecoder = codebook.getHuffmanDecoder();
            }
            assert (codebook != null && huffmanDecoder != null);


            final int planeDataSize = (int) header.getPlaneDataSizes()[planeIndex];
            try (final InBitStream inBitStream = new InBitStream(compressedStream,
                                                                 header.getBitsPerCodebookIndex(),
                                                                 planeDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                final int[][] decompressedVectors = new int[(int) planeVectorCount][vectorSize];
                for (int vecIndex = 0; vecIndex < planeVectorCount; vecIndex++) {
                    final int decodedSymbol = huffmanDecoder.decodeSymbol(inBitStream);
                    System.arraycopy(codebook.getVectors()[decodedSymbol], 0, decompressedVectors[vecIndex], 0, vectorSize);
                }


                final Block decompressedPlane = reconstructImageFromQuantizedVectors(decompressedVectors,
                                                                                     qVector,
                                                                                     header.getImageDims());

                callback.process(decompressedPlane, planeIndex);
            } catch (final Exception ex) {
                throw new ImageDecompressionException("VQImageDecompressor::decompressToBuffer() - Unable to read indices from " +
                                                              "InBitStream.",
                                                      ex);
            }
            reportProgressToListeners(planeIndex, planeCountForDecompression,
                                      "Decompressed plane %d.", planeIndex);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public void decompressStreamModelImpl(final DataInputStream compressedStream,
                                          final QCMPFileHeaderV1 header,
                                          final DecompressCallback callback) throws ImageDecompressionException {

        assert (cachedCodebook != null && cachedHuffmanDecoder != null);
        assert (header.getVectorSizeZ() == 1);
        final int planeCountForDecompression = header.getImageSizeZ();
        final long planeVectorCount = calculatePlaneVectorCount(header);
        final V2i qVector = new V2i(header.getVectorSizeX(), header.getVectorSizeY());
        final int vectorSize = qVector.multiplyTogether();


        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {

            final int planeDataSize = (int) header.getPlaneDataSizes()[planeIndex];
            try (final InBitStream inBitStream = new InBitStream(compressedStream,
                                                                 header.getBitsPerCodebookIndex(),
                                                                 planeDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                final int[][] decompressedVectors = new int[(int) planeVectorCount][vectorSize];
                int huffmanIndex;
                for (int vecIndex = 0; vecIndex < planeVectorCount; vecIndex++) {
                    huffmanIndex = cachedHuffmanDecoder.decodeSymbol(inBitStream);
                    System.arraycopy(cachedCodebook.getVectors()[huffmanIndex], 0, decompressedVectors[vecIndex], 0, vectorSize);
                }


                final Block decompressedPlane = reconstructImageFromQuantizedVectors(decompressedVectors, qVector, header.getImageDims());

                callback.process(decompressedPlane, planeIndex);
            } catch (final Exception ex) {
                throw new ImageDecompressionException("VQImageDecompressor::decompressToBuffer() - Unable to read indices from " +
                                                              "InBitStream.",
                                                      ex);
            }
            reportProgressToListeners(planeIndex, planeCountForDecompression,
                                      "Decompressed plane %d.", planeIndex);
        }
    }


    @Override
    public void decompressToBuffer(final DataInputStream compressedStream,
                                   final short[][] buffer,
                                   final QCMPFileHeaderV1 header) throws ImageDecompressionException {
        if (header.getQuantizationType() == QuantizationType.Vector3D) {
            decompressVoxelsToBuffer(compressedStream, buffer, header);
            return;
        }
        decompressImpl(compressedStream, header, (imageBlock, planeIndex) -> {
            buffer[planeIndex] = TypeConverter.intArrayToShortArray(imageBlock.getData());
        });
    }


    private void decompressVoxelsImpl(final DataInputStream compressedStream,
                                      final QCMPFileHeaderV1 header,
                                      final DecompressVoxelCallback callback) throws ImageDecompressionException {

        assert (header.getQuantizationType() == QuantizationType.Vector3D);
        assert (!header.isCodebookPerPlane()); // SHOULD ALWAYS BE GLOBAL.

        final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
        final V3i voxelDims = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());
        final int vectorSize = (int) voxelDims.multiplyTogether();
        final int voxelLayerDepth = voxelDims.getZ();
        final int[] huffmanSymbols = createHuffmanSymbols(codebookSize);


        final VQCodebook codebook = readCodebook(compressedStream, codebookSize, vectorSize);
        final HuffmanDecoder huffmanDecoder = codebook.getHuffmanDecoder();

        final int voxelLayerCount = VQImageCompressor.calculateVoxelLayerCount(header.getImageSizeZ(), header.getVectorSizeZ());
        final Stopwatch stopwatch = new Stopwatch();
        for (int voxelLayerIndex = 0; voxelLayerIndex < voxelLayerCount; voxelLayerIndex++) {
            stopwatch.restart();

            final int fromZ = (voxelLayerIndex * voxelLayerDepth);
            final int toZ = (voxelLayerIndex == voxelLayerCount - 1)
                    ? header.getImageSizeZ()
                    : (voxelLayerDepth + (voxelLayerIndex * voxelLayerDepth));
            final V3i currentVoxelLayerDims = new V3i(header.getImageSizeX(), header.getImageSizeY(), toZ - fromZ);
            final int voxelLayerDataSize = (int) header.getPlaneDataSizes()[voxelLayerIndex];
            final int voxelLayerVoxelCount = Voxel.calculateRequiredVoxelCount(currentVoxelLayerDims, voxelDims);

            final int[][] decompressedVoxels = new int[voxelLayerVoxelCount][vectorSize];

            try (final InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerCodebookIndex(), voxelLayerDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                for (int voxelIndex = 0; voxelIndex < voxelLayerVoxelCount; voxelIndex++) {
                    final int huffmanSymbol = huffmanDecoder.decodeSymbol(inBitStream);
                    System.arraycopy(codebook.getVectors()[huffmanSymbol], 0, decompressedVoxels[voxelIndex], 0, vectorSize);
                }

            } catch (final Exception e) {
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

    @SuppressWarnings("DuplicatedCode")
    private void decompressVoxelsStreamModeImpl(final DataInputStream compressedStream,
                                                final QCMPFileHeaderV1 header,
                                                final DecompressVoxelCallback callback) throws ImageDecompressionException {

        assert (header.getQuantizationType() == QuantizationType.Vector3D);
        assert (!header.isCodebookPerPlane()); // SHOULD ALWAYS BE GLOBAL.


        final V3i voxelDims = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());
        final int vectorSize = (int) voxelDims.multiplyTogether();
        final int voxelLayerDepth = voxelDims.getZ();


        final int voxelLayerCount = VQImageCompressor.calculateVoxelLayerCount(header.getImageSizeZ(), header.getVectorSizeZ());
        final Stopwatch stopwatch = new Stopwatch();
        for (int voxelLayerIndex = 0; voxelLayerIndex < voxelLayerCount; voxelLayerIndex++) {
            stopwatch.restart();

            final int fromZ = (voxelLayerIndex * voxelLayerDepth);
            final int toZ = (voxelLayerIndex == voxelLayerCount - 1)
                    ? header.getImageSizeZ()
                    : (voxelLayerDepth + (voxelLayerIndex * voxelLayerDepth));
            final V3i currentVoxelLayerDims = new V3i(header.getImageSizeX(), header.getImageSizeY(), toZ - fromZ);
            final int voxelLayerDataSize = (int) header.getPlaneDataSizes()[voxelLayerIndex];
            final int voxelLayerVoxelCount = Voxel.calculateRequiredVoxelCount(currentVoxelLayerDims, voxelDims);

            final int[][] decompressedVoxels = new int[voxelLayerVoxelCount][vectorSize];

            try (final InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerCodebookIndex(), voxelLayerDataSize)) {
                inBitStream.fillEntireBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);

                for (int voxelIndex = 0; voxelIndex < voxelLayerVoxelCount; voxelIndex++) {
                    final int huffmanSymbol = cachedHuffmanDecoder.decodeSymbol(inBitStream);
                    System.arraycopy(cachedCodebook.getVectors()[huffmanSymbol], 0, decompressedVoxels[voxelIndex], 0, vectorSize);
                }

            } catch (final Exception e) {
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


    private void decompressVoxelsToBuffer(final DataInputStream compressedStream,
                                          final short[][] buffer,
                                          final QCMPFileHeaderV1 header) throws ImageDecompressionException {

        final V3i voxelDims = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());

        decompressVoxelsImpl(compressedStream, header, (decompressedVoxel, decompressedVoxelData, planeOffset) ->
                decompressedVoxel.reconstructFromVoxels(voxelDims, decompressedVoxelData, buffer, planeOffset));
    }

    private void decompressVoxels(final DataInputStream compressedStream,
                                  final DataOutputStream decompressStream,
                                  final QCMPFileHeaderV1 header) throws ImageDecompressionException {

        final V3i voxelDims = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());
        decompressVoxelsImpl(compressedStream, header, (voxel, voxelData, planeOffset) -> {

            final ImageU16Dataset currentVoxelLayer = voxel.reconstructFromVoxelsToDataset(voxelDims, voxelData);

            for (int layer = 0; layer < voxel.getDims().getZ(); layer++) {
                try {
                    decompressStream.write(TypeConverter.unsignedShortArrayToByteArray(currentVoxelLayer.getPlaneData(layer), false));
                } catch (final IOException e) {
                    throw new ImageDecompressionException("Unable to write to decompress stream.", e);
                }
            }
        });
    }

    @Override
    public short[] decompressStreamMode(final DataInputStream compressedStream,
                                        final QCMPFileHeaderV1 header) throws ImageDecompressionException {
        final short[] buffer = new short[(int) header.getImageDims().multiplyTogether()];
        if (header.getQuantizationType() == QuantizationType.Vector3D) {
            final V3i voxelDim = new V3i(header.getVectorSizeX(), header.getVectorSizeY(), header.getVectorSizeZ());

            decompressVoxelsStreamModeImpl(compressedStream, header, (voxel, voxelData, planeOffset) -> {

                final ImageU16Dataset decompressedVoxel = voxel.reconstructFromVoxelsToDataset(voxelDim, voxelData);
                assert (decompressedVoxel.getPlaneCount() == voxel.getDims().getZ());
                final int expectedVoxelPlaneSize = header.getImageSizeX() * header.getImageSizeY();

                final int baseOffset = planeOffset * expectedVoxelPlaneSize;

                for (int voxelLayerIndex = 0; voxelLayerIndex < decompressedVoxel.getPlaneCount(); voxelLayerIndex++) {
                    final short[] voxelLayerData = decompressedVoxel.getPlaneData(voxelLayerIndex);
                    assert (voxelLayerData.length == expectedVoxelPlaneSize);
                    final int bufferPos = baseOffset + (voxelLayerIndex * expectedVoxelPlaneSize);
                    System.arraycopy(voxelLayerData, 0, buffer, bufferPos, voxelLayerData.length);
                }
            });
            return buffer;
        } else {
            final int planePixelCount = header.getImageDims().toV2i().multiplyTogether();
            decompressStreamModelImpl(compressedStream, header, (imageBlock, planeIndex) -> {
                final int offset = planePixelCount * planeIndex;
                final int[] data = imageBlock.getData();
                for (int i = 0; i < planePixelCount; i++) {
                    buffer[offset + i] = (short) data[i];
                }
            });
        }
        return buffer;


    }
}
