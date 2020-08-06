package azgracompress.compression;

import azgracompress.cache.QuantizationCacheManager;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.Chunk2D;
import azgracompress.data.ImageU16;
import azgracompress.data.Range;
import azgracompress.fileformat.QuantizationType;
import azgracompress.huffman.Huffman;
import azgracompress.io.InputData;
import azgracompress.io.loader.IPlaneLoader;
import azgracompress.io.loader.PlaneLoaderFactory;
import azgracompress.quantization.vector.*;
import azgracompress.utilities.Stopwatch;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class VQImageCompressor extends CompressorDecompressorBase implements IImageCompressor {

    public VQImageCompressor(CompressionOptions options) {
        super(options);
    }

    /**
     * Train vector quantizer from plane vectors.
     *
     * @param planeVectors Image vectors.
     * @return Trained vector quantizer with codebook of set size.
     */
    private VectorQuantizer trainVectorQuantizerFromPlaneVectors(final int[][] planeVectors) {

        LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeVectors,
                                                                  getCodebookSize(),
                                                                  options.getWorkerCount(),
                                                                  options.getQuantizationVector());
        LBGResult vqResult = vqInitializer.findOptimalCodebook();
        return new VectorQuantizer(vqResult.getCodebook());
    }

    /**
     * Write the vector codebook to the compress stream.
     *
     * @param quantizer      Quantizer with the codebook.
     * @param compressStream Stream with compressed data.
     * @throws ImageCompressionException When unable to write quantizer.
     */
    private void writeQuantizerToCompressStream(final VectorQuantizer quantizer,
                                                DataOutputStream compressStream) throws ImageCompressionException {
        final CodebookEntry[] codebook = quantizer.getCodebookVectors();
        try {
            for (final CodebookEntry entry : codebook) {
                final int[] entryVector = entry.getVector();
                for (final int vecVal : entryVector) {
                    compressStream.writeShort(vecVal);
                }
            }
            final long[] frequencies = quantizer.getFrequencies();
            for (final long symbolFrequency : frequencies) {
                compressStream.writeLong(symbolFrequency);
            }
        } catch (IOException ioEx) {
            throw new ImageCompressionException("Unable to write codebook to compress stream.", ioEx);
        }
        if (options.isVerbose()) {
            reportStatusToListeners("Wrote quantization vectors to compressed stream.");
        }
    }

    /**
     * Load quantizer from cached codebook.
     *
     * @return Vector quantizer with cached codebook.
     * @throws ImageCompressionException when fails to read cached codebook.
     */
    private VectorQuantizer loadQuantizerFromCache() throws ImageCompressionException {

        QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());

        if (!cacheManager.doesVQCacheExists(options.getInputDataInfo().getCacheFileName(),
                                            getCodebookSize(),
                                            options.getQuantizationVector())) {
            trainAndSaveCodebook();
        }

        final VQCodebook codebook = cacheManager.loadVQCodebook(options.getInputDataInfo().getCacheFileName(),
                                                                getCodebookSize(),
                                                                options.getQuantizationVector());

        if (codebook == null) {
            throw new ImageCompressionException("Failed to read quantization vectors from cache.");
        }
        return new VectorQuantizer(codebook);
    }

    /**
     * Compress the image file specified by parsed CLI options using vector quantization.
     *
     * @param compressStream Stream to which compressed data will be written.
     * @throws ImageCompressionException When compress process fails.
     */
    @Override
    public long[] compress(DataOutputStream compressStream) throws ImageCompressionException {
        if (options.getQuantizationType() == QuantizationType.Vector3D) {
            return compressVoxels(compressStream);
        }
        assert (options.getQuantizationVector().getZ() == 1);
        return compress1D2DVectors(compressStream);
    }

    @NotNull
    private long[] compress1D2DVectors(DataOutputStream compressStream) throws ImageCompressionException {
        final InputData inputDataInfo = options.getInputDataInfo();
        Stopwatch stopwatch = new Stopwatch();
        final boolean hasGeneralQuantizer = options.getCodebookType() != CompressionOptions.CodebookType.Individual;
        final IPlaneLoader planeLoader;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputDataInfo);
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create plane reader. " + e.getMessage());
        }
        VectorQuantizer quantizer = null;
        Huffman huffman = null;

        if (options.getCodebookType() == CompressionOptions.CodebookType.Global) {
            reportStatusToListeners("Loading codebook from cache file.");
            quantizer = loadQuantizerFromCache();
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
            reportStatusToListeners("Cached quantizer with huffman coder created.");
            writeQuantizerToCompressStream(quantizer, compressStream);
        } else if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
            stopwatch.restart();

            final int middlePlaneIndex = getMiddlePlaneIndex();
            ImageU16 middlePlane = null;
            try {
                middlePlane = new ImageU16(options.getInputDataInfo().getDimensions().toV2i(), planeLoader.loadPlaneData(middlePlaneIndex));
            } catch (IOException ex) {
                throw new ImageCompressionException("Unable to load reference plane data.", ex);
            }

            reportStatusToListeners(String.format("Training vector quantizer from middle plane %d.", middlePlaneIndex));
            final int[][] refPlaneVectors = middlePlane.toQuantizationVectors(options.getQuantizationVector().toV2i());
            quantizer = trainVectorQuantizerFromPlaneVectors(refPlaneVectors);
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
            writeQuantizerToCompressStream(quantizer, compressStream);
            stopwatch.stop();
            reportStatusToListeners("Middle plane codebook created in: " + stopwatch.getElapsedTimeString());
        }

        final int[] planeIndices = getPlaneIndicesForCompression();
        long[] planeDataSizes = new long[planeIndices.length];
        int planeCounter = 0;

        for (final int planeIndex : planeIndices) {
            stopwatch.restart();
            reportStatusToListeners(String.format("Loading plane %d.", planeIndex));

            ImageU16 plane = null;
            try {
                plane = new ImageU16(options.getInputDataInfo().getDimensions().toV2i(), planeLoader.loadPlaneData(planeIndex));
            } catch (IOException ex) {
                throw new ImageCompressionException("Unable to load plane data.", ex);
            }

            final int[][] planeVectors = plane.toQuantizationVectors(options.getQuantizationVector().toV2i());

            if (!hasGeneralQuantizer) {
                reportStatusToListeners(String.format("Training vector quantizer from plane %d.", planeIndex));
                quantizer = trainVectorQuantizerFromPlaneVectors(planeVectors);
                huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
                writeQuantizerToCompressStream(quantizer, compressStream);
            }

            assert (quantizer != null);

            final int[] indices = quantizer.quantizeIntoIndices(planeVectors, options.getWorkerCount());

            planeDataSizes[planeCounter++] = writeHuffmanEncodedIndices(compressStream, huffman, indices);

            stopwatch.stop();
            reportProgressToListeners(planeIndex, planeIndices.length,
                                      "Finished compression of plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString());
        }
        return planeDataSizes;
    }


    /**
     * Load plane and convert the plane into quantization vectors.
     *
     * @param planeIndex Zero based plane index.
     * @return Quantization vectors of configured quantization.
     * @throws IOException When reading fails.
     */

    private int[][] loadPlaneQuantizationVectors(final IPlaneLoader planeLoader,
                                                 final int planeIndex) throws IOException {
        ImageU16 refPlane = new ImageU16(options.getInputDataInfo().getDimensions().toV2i(), planeLoader.loadPlaneData(planeIndex));
        return refPlane.toQuantizationVectors(options.getQuantizationVector().toV2i());
    }

    private int[][] loadConfiguredPlanesData() throws ImageCompressionException {

        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create reader. " + e.getMessage());
        }
        if (options.getQuantizationType().isOneOf(QuantizationType.Vector1D, QuantizationType.Vector2D)) {
            // TODO: Chunk2D operations should eventually be moved to loaders.
            //  Same as voxel loading, so that we wouldn't have to copy data twice.
            final int vectorSize = options.getQuantizationVector().toV2i().multiplyTogether();

            int[][] trainData;
            Stopwatch s = Stopwatch.startNew();
            if (options.getInputDataInfo().isPlaneIndexSet()) {
                reportStatusToListeners("VQ: Loading single plane data.");
                try {
                    trainData = loadPlaneQuantizationVectors(planeLoader, options.getInputDataInfo().getPlaneIndex());
                } catch (IOException e) {
                    throw new ImageCompressionException("Failed to load plane data.", e);
                }
            } else {
                reportStatusToListeners(options.getInputDataInfo().isPlaneRangeSet() ?
                                                "VQ: Loading plane range data." : "VQ: Loading all planes data.");

                final int[] planeIndices = getPlaneIndicesForCompression();
                final int chunkCountPerPlane = Chunk2D.calculateRequiredChunkCount(options.getInputDataInfo().getDimensions().toV2i(),
                                                                                   options.getQuantizationVector().toV2i());
                final int totalChunkCount = chunkCountPerPlane * planeIndices.length;
                trainData = new int[totalChunkCount][vectorSize];

                int[][] planeVectors;
                int planeCounter = 0;
                for (final int planeIndex : planeIndices) {
                    try {
                        planeVectors = loadPlaneQuantizationVectors(planeLoader, planeIndex);
                        assert (planeVectors.length == chunkCountPerPlane) : "Wrong chunk count per plane";
                    } catch (IOException e) {
                        throw new ImageCompressionException(String.format("Failed to load plane %d image data.", planeIndex), e);
                    }

                    System.arraycopy(planeVectors, 0, trainData, (planeCounter * chunkCountPerPlane), chunkCountPerPlane);
                    ++planeCounter;
                }
            }
            s.stop();
            reportStatusToListeners("Quantization vector load took: " + s.getElapsedTimeString());
            return trainData;
        } else {
            if (options.getQuantizationType() != QuantizationType.Vector3D) {
                throw new ImageCompressionException("Invalid QuantizationType, expected: `QuantizationType.Vector3D`, but got: " +
                                                            options.getQuantizationType().toString());
            }

            try {
                return planeLoader.loadVoxels(options.getQuantizationVector());
            } catch (IOException e) {
                throw new ImageCompressionException("Unable to load voxels.", e);
            }
        }
    }

    @Override
    public void trainAndSaveCodebook() throws ImageCompressionException {
        reportStatusToListeners("Loading image data...");
        final int[][] trainingData = loadConfiguredPlanesData();

        LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(trainingData,
                                                                  getCodebookSize(),
                                                                  options.getWorkerCount(),
                                                                  options.getQuantizationVector());

        reportStatusToListeners("Starting LBG optimization.");
        vqInitializer.setStatusListener(this::reportStatusToListeners);
        LBGResult lbgResult = vqInitializer.findOptimalCodebook();
        reportStatusToListeners("Learned the optimal codebook.");


        reportStatusToListeners("Saving cache file to %s", options.getOutputFilePath());
        QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());
        try {
            cacheManager.saveCodebook(options.getInputDataInfo().getCacheFileName(), lbgResult.getCodebook());
        } catch (IOException e) {
            throw new ImageCompressionException("Unable to write VQ cache.", e);
        }
        reportStatusToListeners("Operation completed.");
    }

    /**
     * Calculate the number of voxel layers needed for dataset of plane count.
     *
     * @param datasetPlaneCount Dataset plane count
     * @param voxelDepth        Z dimension of voxel.
     * @return Number of voxel layers.
     */
    public static int calculateVoxelLayerCount(final int datasetPlaneCount, final int voxelDepth) {
        return (datasetPlaneCount / voxelDepth);
    }

    public long[] compressVoxels(DataOutputStream compressStream) throws ImageCompressionException {
        assert (options.getCodebookType() == CompressionOptions.CodebookType.Global);
        final IPlaneLoader planeLoader;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create plane reader. " + e.getMessage());
        }

        final int voxelLayerDepth = options.getQuantizationVector().getZ();
        final int voxelLayerCount = calculateVoxelLayerCount(options.getInputDataInfo().getDimensions().getZ(), voxelLayerDepth);
        long[] voxelLayersSizes = new long[voxelLayerCount];

        final VectorQuantizer quantizer = loadQuantizerFromCache();
        final Huffman huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
        writeQuantizerToCompressStream(quantizer, compressStream);

        int[][] voxelData;
        Stopwatch stopwatch = new Stopwatch();
        for (int voxelLayerIndex = 0; voxelLayerIndex < voxelLayerCount; voxelLayerIndex++) {
            stopwatch.restart();
            final int fromZ = (voxelLayerIndex * voxelLayerDepth);

            // TODO(Moravec):   There is a problem!
            //                  If dataset.Z is not divisible by voxel.Z we end up creating a lot stupid voxels.
            //                  Those stupid voxels have only one or two layers of actual data and the rest are zeros.
            //                  This ends up increasing the file size because they have quite long Huffman codes.
            final int toZ = (voxelLayerIndex == voxelLayerCount - 1)
                    ? options.getInputDataInfo().getDimensions().getZ()
                    : (voxelLayerDepth + (voxelLayerIndex * voxelLayerDepth));

            final Range<Integer> voxelLayerRange = new Range<>(fromZ, toZ);

            try {
                voxelData = planeLoader.loadVoxels(options.getQuantizationVector(), voxelLayerRange);
                System.out.println("voxelData.length=" + voxelData.length);
            } catch (IOException e) {
                throw new ImageCompressionException("Unable to load voxels from voxel layer " + voxelLayerRange, e);
            }

            final int[] indices = quantizer.quantizeIntoIndices(voxelData, options.getWorkerCount());
            voxelLayersSizes[voxelLayerIndex] = writeHuffmanEncodedIndices(compressStream, huffman, indices);
            stopwatch.stop();
            reportProgressToListeners(voxelLayerIndex, voxelLayerCount,
                                      "%d/%d Finished voxel layer %s compression pass in %s",
                                      voxelLayerIndex, voxelLayerCount, voxelLayerRange.toString(), stopwatch.getElapsedTimeString());
        }

        return voxelLayersSizes;
    }

}
