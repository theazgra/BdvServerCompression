package azgracompress.compression;

import azgracompress.cache.QuantizationCacheManager;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.Range;
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
            reportStatusToListeners("Training vector quantizer from middle plane.");
            final int[][] refPlaneVectors = planeLoader.loadVectorsFromPlaneRange(options, Utils.singlePlaneRange(getMiddlePlaneIndex()));
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

            final int[][] planeVectors = planeLoader.loadVectorsFromPlaneRange(options, Utils.singlePlaneRange(planeIndex));

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

    @Override
    public void trainAndSaveCodebook() throws ImageCompressionException {
        reportStatusToListeners("Loading image data...");

        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create plane reader. " + e.getMessage());
        }

        int[][] trainingData;
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


        LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(trainingData,
                                                                  getCodebookSize(),
                                                                  options.getWorkerCount(),
                                                                  options.getQuantizationVector());

        reportStatusToListeners("Starting LBG optimization.");
        vqInitializer.setStatusListener(this::reportStatusToListeners);
        LBGResult lbgResult = vqInitializer.findOptimalCodebook();
        reportStatusToListeners("Learned the optimal codebook.");


        QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());
        try {
            final String cacheFilePath = cacheManager.saveCodebook(options.getInputDataInfo().getCacheFileName(), lbgResult.getCodebook());
            reportStatusToListeners("Saved cache file to %s", cacheFilePath);
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
            planeLoader.setWorkerCount(options.getWorkerCount());
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
