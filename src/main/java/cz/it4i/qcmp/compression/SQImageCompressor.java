package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.cache.ICacheFile;
import cz.it4i.qcmp.cache.QuantizationCacheManager;
import cz.it4i.qcmp.cache.SQCacheFile;
import cz.it4i.qcmp.compression.exception.ImageCompressionException;
import cz.it4i.qcmp.huffman.HuffmanEncoder;
import cz.it4i.qcmp.io.InputData;
import cz.it4i.qcmp.io.loader.IPlaneLoader;
import cz.it4i.qcmp.io.loader.PlaneLoaderFactory;
import cz.it4i.qcmp.quantization.scalar.LloydMaxU16ScalarQuantization;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;
import cz.it4i.qcmp.quantization.scalar.ScalarQuantizer;
import cz.it4i.qcmp.utilities.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;

public class SQImageCompressor extends CompressorDecompressorBase implements IImageCompressor {

    private ScalarQuantizer cachedQuantizer;
    private HuffmanEncoder cachedHuffmanEncoder;

    public SQImageCompressor(final CompressionOptions options) {
        super(options);
    }

    /**
     * Train Lloyd-Max scalar quantizer from plane data.
     *
     * @param planeData Plane data from which quantizer will be trained.
     * @return Trained scalar quantizer.
     */
    private ScalarQuantizer trainScalarQuantizerFromData(final int[] planeData) {

        final LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(planeData,
                                                                                         getCodebookSize(),
                                                                                         options.getWorkerCount());
        lloydMax.train();
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCodebook());
    }

    @Override
    public void preloadGlobalCodebook(final ICacheFile codebookCacheFile) {
        final SQCodebook cachedCodebook = ((SQCacheFile) codebookCacheFile).getCodebook();
        cachedQuantizer = new ScalarQuantizer(cachedCodebook);
        cachedHuffmanEncoder = createHuffmanEncoder(createHuffmanSymbols(cachedCodebook.getCodebookSize()),
                                                    cachedCodebook.getSymbolFrequencies());
    }

    /**
     * Writes the scalar quantizer to the compressed stream.
     *
     * @param quantizer      Quantizer used for compression of the image.
     * @param compressStream Compressed data stream.
     * @throws ImageCompressionException when writing to the stream fails.
     */
    private void writeCodebookToOutputStream(final ScalarQuantizer quantizer,
                                             final DataOutputStream compressStream) throws ImageCompressionException {
        final SQCodebook codebook = quantizer.getCodebook();
        final int[] centroids = codebook.getCentroids();
        final long[] frequencies = codebook.getSymbolFrequencies();
        try {
            for (final int quantizationValue : centroids) {
                compressStream.writeShort(quantizationValue);
            }
            for (final long symbolFrequency : frequencies) {
                compressStream.writeLong(symbolFrequency);
            }
        } catch (final IOException ioEx) {
            throw new ImageCompressionException("Unable to write codebook to compress stream.", ioEx);
        }
        if (options.isVerbose()) {
            reportStatusToListeners("Wrote quantization values to compressed stream.");
        }
    }

    /**
     * Load quantization codebook from cache file.
     *
     * @return Scalar quantizer with cached codebook.
     * @throws ImageCompressionException when fails to read cached codebook.
     */
    private ScalarQuantizer loadQuantizerFromCache() throws ImageCompressionException {
        final QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());

        if (!cacheManager.doesSQCacheExists(options.getInputDataInfo().getCacheFileName(), getCodebookSize())) {
            trainAndSaveCodebook();
        }

        final SQCodebook codebook = cacheManager.loadSQCodebook(options.getInputDataInfo().getCacheFileName(),
                                                                getCodebookSize());
        if (codebook == null) {
            throw new ImageCompressionException("Failed to read quantization values from cache file.");
        }
        return new ScalarQuantizer(codebook);
    }

    /**
     * Compress the image file specified by parsed CLI options using scalar quantization.
     *
     * @param compressStream Stream to which compressed data will be written.
     * @throws ImageCompressionException When compress process fails.
     */
    @Override
    public long[] compress(final DataOutputStream compressStream) throws ImageCompressionException {
        final InputData inputDataInfo = options.getInputDataInfo();
        final Stopwatch stopwatch = new Stopwatch();
        final boolean hasGeneralQuantizer = options.getCodebookType() != CompressionOptions.CodebookType.Individual;

        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputDataInfo);
        } catch (final Exception e) {
            throw new ImageCompressionException("Unable to create SCIFIO reader. " + e.getMessage());
        }

        ScalarQuantizer quantizer = null;
        HuffmanEncoder huffmanEncoder = null;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        if (options.getCodebookType() == CompressionOptions.CodebookType.Global) {
            reportStatusToListeners("Loading codebook from cache file.");

            quantizer = loadQuantizerFromCache();
            huffmanEncoder = createHuffmanEncoder(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());

            reportStatusToListeners("Cached quantizer with huffman coder created.");
            writeCodebookToOutputStream(quantizer, compressStream);
        } else if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
            stopwatch.restart();
            final int[] middlePlaneData;
            final int middlePlaneIndex = getMiddlePlaneIndex();
            try {
                middlePlaneData = planeLoader.loadPlaneData(0, middlePlaneIndex);
            } catch (final IOException ex) {
                throw new ImageCompressionException("Unable to load middle plane data.", ex);
            }

            reportStatusToListeners(String.format("Training scalar quantizer from middle plane %d.", middlePlaneIndex));
            quantizer = trainScalarQuantizerFromData(middlePlaneData);
            huffmanEncoder = createHuffmanEncoder(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());

            stopwatch.stop();
            writeCodebookToOutputStream(quantizer, compressStream);
            reportStatusToListeners("Middle plane codebook with huffman coder created in: " + stopwatch.getElapsedTimeString());
        }

        final int[] planeIndices = getPlaneIndicesForCompression(options.getInputDataInfo());
        final long[] planeDataSizes = new long[planeIndices.length];
        int planeCounter = 0;


        //        final int[][] preloadedPlaneData;
        //        try {
        //            preloadedPlaneData = planeLoader.loadPlanesU16DataTo2dArray(planeIndices);
        //        } catch (final IOException ex) {
        //            throw new ImageCompressionException("Unable to preload plane data.", ex);
        //        }

        //        final int index = 0;
        for (final int planeIndex : planeIndices) {
            stopwatch.restart();

            final int[] planeData;
            //            planeData = preloadedPlaneData[index++];
            try {
                planeData = planeLoader.loadPlaneData(0, planeIndex);
            } catch (final IOException ex) {
                throw new ImageCompressionException("Unable to load plane data.", ex);
            }

            if (!hasGeneralQuantizer) {
                reportStatusToListeners(String.format("Training scalar quantizer from plane %d.", planeIndex));
                quantizer = trainScalarQuantizerFromData(planeData);
                writeCodebookToOutputStream(quantizer, compressStream);

                huffmanEncoder = createHuffmanEncoder(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());
            }

            assert (quantizer != null) : "Scalar Quantizer wasn't initialized.";
            assert (huffmanEncoder != null) : "Huffman wasn't initialized.";

            final int[] indices = quantizer.quantizeIntoIndices(planeData, 1);

            planeDataSizes[planeCounter++] = writeHuffmanEncodedIndices(compressStream, huffmanEncoder, indices);

            stopwatch.stop();
            reportProgressToListeners(planeIndex, planeIndices.length,
                                      "Compressed plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString());
        }
        return planeDataSizes;
    }

    private int[] loadConfiguredPlanesData() throws ImageCompressionException, IOException {
        final InputData inputDataInfo = options.getInputDataInfo();
        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputDataInfo);
        } catch (final Exception e) {
            throw new ImageCompressionException("Unable to create SCIFIO reader. " + e.getMessage());
        }
        int[] trainData = null;

        if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
            final int middlePlaneIndex = getMiddlePlaneIndex();
            reportStatusToListeners("Loading single plane data.");
            trainData = planeLoader.loadPlaneData(0, middlePlaneIndex);
        } else if (inputDataInfo.isPlaneIndexSet()) {
            reportStatusToListeners("Loading single plane data.");
            trainData = planeLoader.loadPlaneData(0, inputDataInfo.getPlaneIndex());
        } else if (inputDataInfo.isPlaneRangeSet()) {
            reportStatusToListeners("Loading plane range data.");
            final int[] planes = getPlaneIndicesForCompression(options.getInputDataInfo());
            trainData = planeLoader.loadPlanesU16Data(0, planes);
        } else {
            reportStatusToListeners("Loading all planes data.");
            trainData = planeLoader.loadAllPlanesU16Data(0);
        }
        return trainData;
    }

    @Override
    public void trainAndSaveCodebook() throws ImageCompressionException {
        final int[] trainData;
        try {
            trainData = loadConfiguredPlanesData();
        } catch (final IOException e) {
            throw new ImageCompressionException("Failed to load configured plane data in SQImageCompressor.");
        }

        final LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(trainData,
                                                                                         getCodebookSize(),
                                                                                         options.getWorkerCount());
        reportStatusToListeners("Starting LloydMax training.");

        lloydMax.setStatusListener(this::reportStatusToListeners);
        lloydMax.train();
        final SQCodebook codebook = lloydMax.getCodebook();
        reportStatusToListeners("Finished LloydMax training.");

        final QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());
        try {
            final String cacheFilePath = cacheManager.saveCodebook(options.getInputDataInfo().getCacheFileName(), codebook);
            reportStatusToListeners(String.format("Saved cache file to %s", cacheFilePath));
        } catch (final IOException e) {
            throw new ImageCompressionException("Unable to write cache.", e);
        }
        reportStatusToListeners("Operation completed.");
    }

    @Override
    public long[] compressStreamChunk(final DataOutputStream compressStream, final InputData inputData) throws ImageCompressionException {
        throw new ImageCompressionException("Not implemented yet");
    }

    @Override
    public void trainAndSaveAllCodebooks() throws ImageCompressionException {
        throw new ImageCompressionException("Not implemented yet");
    }
}
