package azgracompress.compression;

import azgracompress.cache.ICacheFile;
import azgracompress.cache.QuantizationCacheManager;
import azgracompress.cache.VQCacheFile;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.Range;
import azgracompress.data.V3i;
import azgracompress.fileformat.QuantizationType;
import azgracompress.huffman.Huffman;
import azgracompress.io.InputData;
import azgracompress.io.loader.IPlaneLoader;
import azgracompress.io.loader.PlaneLoaderFactory;
import azgracompress.quantization.vector.*;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class VQImageCompressor extends CompressorDecompressorBase implements IImageCompressor {

    private VectorQuantizer cachedQuantizer = null;
    private Huffman cachedHuffman = null;

    public VQImageCompressor(final CompressionOptions options) {
        super(options);
    }

    @Override
    public void preloadGlobalCodebook(final ICacheFile codebookCacheFile) {
        final VQCodebook cachedCodebook = ((VQCacheFile) codebookCacheFile).getCodebook();
        cachedQuantizer = new VectorQuantizer(cachedCodebook);
        cachedHuffman = createHuffmanCoder(createHuffmanSymbols(cachedCodebook.getCodebookSize()), cachedCodebook.getVectorFrequencies());
    }

    /**
     * Train vector quantizer from plane vectors.
     *
     * @param planeVectors Image vectors.
     * @return Trained vector quantizer with codebook of set size.
     */
    private VectorQuantizer trainVectorQuantizerFromPlaneVectors(final int[][] planeVectors) {

        final LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeVectors,
                                                                        getCodebookSize(),
                                                                        options.getWorkerCount(),
                                                                        options.getQuantizationVector());
        final LBGResult vqResult = vqInitializer.findOptimalCodebook();
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
                                                final DataOutputStream compressStream) throws ImageCompressionException {
        final int[][] codebook = quantizer.getCodebookVectors();
        try {
            for (final int[] entry : codebook) {
                for (final int vecVal : entry) {
                    compressStream.writeShort(vecVal);
                }
            }
            final long[] frequencies = quantizer.getFrequencies();
            for (final long symbolFrequency : frequencies) {
                compressStream.writeLong(symbolFrequency);
            }
        } catch (final IOException ioEx) {
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
        final QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());

        if (!cacheManager.doesVQCacheExists(options.getInputDataInfo().getCacheFileName(),
                                            getCodebookSize(),
                                            options.getQuantizationVector())) {
            reportStatusToListeners("Quantization file doesn't exist");
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
    public long[] compress(final DataOutputStream compressStream) throws ImageCompressionException {
        if (options.getQuantizationType() == QuantizationType.Vector3D) {
            return compressVoxels(compressStream, false, options.getInputDataInfo());
        }
        assert (options.getQuantizationVector().getZ() == 1);
        return compress1D2DVectors(compressStream, false, options.getInputDataInfo());
    }

    @Override
    public long[] compressStreamChunk(final DataOutputStream compressStream, final InputData inputData) throws ImageCompressionException {
        if (options.getQuantizationType() == QuantizationType.Vector3D) {
            return compressVoxels(compressStream, true, inputData);
        }
        assert (options.getQuantizationVector().getZ() == 1);
        return compress1D2DVectors(compressStream, true, inputData);
    }

    @NotNull
    private long[] compress1D2DVectors(final DataOutputStream compressStream,
                                       final boolean streamMode,
                                       final InputData inputData) throws ImageCompressionException {

        final Stopwatch stopwatch = new Stopwatch();
        final boolean hasGeneralQuantizer = options.getCodebookType() != CompressionOptions.CodebookType.Individual;
        final IPlaneLoader planeLoader;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputData);
        } catch (final Exception e) {
            throw new ImageCompressionException("Unable to create plane reader. " + e.getMessage());
        }

        VectorQuantizer quantizer = cachedQuantizer;
        Huffman huffman = cachedHuffman;
        assert (!streamMode || ((quantizer != null) && (huffman != null)));

        if (!streamMode) {
            if (options.getCodebookType() == CompressionOptions.CodebookType.Global) {
                reportStatusToListeners("Loading codebook from cache file.");
                quantizer = loadQuantizerFromCache();
                huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
                reportStatusToListeners("Cached quantizer with huffman coder created.");
            } else if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
                stopwatch.restart();
                reportStatusToListeners("Training vector quantizer from middle plane.");
                final int[][] refPlaneVectors = planeLoader.loadVectorsFromPlaneRange(options,
                                                                                      Utils.singlePlaneRange(getMiddlePlaneIndex()));
                quantizer = trainVectorQuantizerFromPlaneVectors(refPlaneVectors);
                huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
                stopwatch.stop();
                reportStatusToListeners("Middle plane codebook created in: " + stopwatch.getElapsedTimeString());
            }
            writeQuantizerToCompressStream(quantizer, compressStream);
        }

        final int[] planeIndices = getPlaneIndicesForCompression(inputData);
        if (streamMode) {
            try {
                // Image dimensions
                compressStream.writeShort(inputData.getDimensions().getX());
                compressStream.writeShort(inputData.getDimensions().getY());
                compressStream.writeShort(inputData.getDimensions().getZ());

                // Write voxel layer in stream mode.
                compressStream.writeShort(planeIndices.length);
            } catch (final IOException e) {
                throw new ImageCompressionException("Failed to write short value to compression stream.", e);
            }
        }
        final long[] planeDataSizes = new long[planeIndices.length];
        int planeCounter = 0;

        for (final int planeIndex : planeIndices) {
            stopwatch.restart();


            final int[][] planeVectors = planeLoader.loadVectorsFromPlaneRange(options, Utils.singlePlaneRange(planeIndex));


            if (!streamMode && !hasGeneralQuantizer) {
                reportStatusToListeners(String.format("Training vector quantizer from plane %d.", planeIndex));
                quantizer = trainVectorQuantizerFromPlaneVectors(planeVectors);
                huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
                writeQuantizerToCompressStream(quantizer, compressStream);
            }

            // Use BestBinFirst KDTree for codebook lookup.
            // final int[] indices = quantizer.quantizeIntoIndicesUsingKDTree(planeVectors, options.getWorkerCount());
            // Use BruteForce for codebook lookup.
            final int[] indices = quantizer.quantizeIntoIndices(planeVectors, options.getWorkerCount());

            planeDataSizes[planeCounter++] = writeHuffmanEncodedIndices(compressStream, huffman, indices);

            stopwatch.stop();
            if (options.isConsoleApplication()) {
                System.out.println("Is console app, whyy");
                reportStatusToListeners("Finished compression of plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString());
            } else {
                reportProgressToListeners(planeIndex, planeIndices.length,
                                          "Finished compression of plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString());
            }
        }
        return planeDataSizes;
    }

    @Override
    public void trainAndSaveCodebook() throws ImageCompressionException {
        reportStatusToListeners("Loading image data...");

        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (final Exception e) {
            throw new ImageCompressionException("Unable to create plane reader. " + e.getMessage());
        }

        final int[][] trainingData;
        if (options.getInputDataInfo().isPlaneIndexSet()) {
            reportStatusToListeners("VQ: Loading single plane data.");
            final int planeIndex = options.getInputDataInfo().getPlaneIndex();
            trainingData = planeLoader.loadVectorsFromPlaneRange(options, new Range<>(planeIndex, planeIndex + 1));
        } else if (options.getInputDataInfo().isPlaneRangeSet()) {
            reportStatusToListeners("VQ: Loading plane range data.");
            trainingData = planeLoader.loadVectorsFromPlaneRange(options, options.getInputDataInfo().getPlaneRange());
        } else {
            reportStatusToListeners("VQ: Loading all planes data.");
            trainingData = planeLoader.loadVectorsFromPlaneRange(options,
                                                                 new Range<>(0, options.getInputDataInfo().getDimensions().getZ()));
        }


        final LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(trainingData,
                                                                        getCodebookSize(),
                                                                        options.getWorkerCount(),
                                                                        options.getQuantizationVector());

        reportStatusToListeners("Starting LBG optimization.");
        vqInitializer.setStatusListener(this::reportStatusToListeners);
        final LBGResult lbgResult = vqInitializer.findOptimalCodebook();
        reportStatusToListeners("Learned the optimal codebook.");


        final QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());
        try {
            final String cacheFilePath = cacheManager.saveCodebook(options.getInputDataInfo().getCacheFileName(), lbgResult.getCodebook());
            reportStatusToListeners("Saved cache file to %s", cacheFilePath);
        } catch (final IOException e) {
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

    public long[] compressVoxels(final DataOutputStream compressStream,
                                 final boolean streamMode,
                                 final InputData inputData) throws ImageCompressionException {
        assert (options.getCodebookType() == CompressionOptions.CodebookType.Global);
        final IPlaneLoader planeLoader;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputData);
            planeLoader.setWorkerCount(options.getWorkerCount());
        } catch (final Exception e) {
            throw new ImageCompressionException("Unable to create plane reader. " + e.getMessage());
        }

        final int voxelLayerDepth = options.getQuantizationVector().getZ();
        final int voxelLayerCount = calculateVoxelLayerCount(inputData.getDimensions().getZ(), voxelLayerDepth);
        if (streamMode) {
            try {
                // Image dimensions
                compressStream.writeShort(inputData.getDimensions().getX());
                compressStream.writeShort(inputData.getDimensions().getY());
                compressStream.writeShort(inputData.getDimensions().getZ());

                // Write voxel layer in stream mode.
                compressStream.writeShort(voxelLayerCount);
            } catch (final IOException e) {
                throw new ImageCompressionException("Failed to write short value to compression stream.", e);
            }
        }
        final long[] voxelLayersSizes = new long[voxelLayerCount];

        final VectorQuantizer quantizer = (cachedQuantizer != null) ? cachedQuantizer : loadQuantizerFromCache();
        final Huffman huffman = (cachedHuffman != null) ? cachedHuffman : createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
        if (!streamMode)
            writeQuantizerToCompressStream(quantizer, compressStream);

        int[][] voxelData;
        final Stopwatch stopwatch = new Stopwatch();
        for (int voxelLayerIndex = 0; voxelLayerIndex < voxelLayerCount; voxelLayerIndex++) {
            stopwatch.restart();
            final int fromZ = (voxelLayerIndex * voxelLayerDepth);

            final int toZ = (voxelLayerIndex == voxelLayerCount - 1)
                    ? inputData.getDimensions().getZ()
                    : (voxelLayerDepth + (voxelLayerIndex * voxelLayerDepth));
            assert (toZ >= fromZ);

            final Range<Integer> voxelLayerRange = new Range<>(fromZ, toZ);

            try {
                voxelData = planeLoader.loadVoxels(options.getQuantizationVector(), voxelLayerRange);
            } catch (final IOException e) {
                throw new ImageCompressionException("Unable to load voxels from voxel layer " + voxelLayerRange, e);
            }

            final int[] indices = quantizer.quantizeIntoIndices(voxelData, options.getWorkerCount());
//            final int[] indices = quantizer.quantizeIntoIndicesUsingKDTree(voxelData, options.getWorkerCount());
            voxelLayersSizes[voxelLayerIndex] = writeHuffmanEncodedIndices(compressStream, huffman, indices);
            stopwatch.stop();
            if (options.isConsoleApplication()) {
                reportStatusToListeners("%d/%d Finished voxel layer %s compression pass in %s",
                                        voxelLayerIndex, voxelLayerCount, voxelLayerRange.toString(), stopwatch.getElapsedTimeString());
            } else {
                reportProgressToListeners(voxelLayerIndex, voxelLayerCount,
                                          "%d/%d Finished voxel layer %s compression pass in %s",
                                          voxelLayerIndex, voxelLayerCount, voxelLayerRange.toString(), stopwatch.getElapsedTimeString());
            }
        }

        return voxelLayersSizes;
    }

}
