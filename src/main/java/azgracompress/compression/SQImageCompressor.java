package azgracompress.compression;

import azgracompress.U16;
import azgracompress.cache.ICacheFile;
import azgracompress.cache.QuantizationCacheManager;
import azgracompress.cache.SQCacheFile;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.V3i;
import azgracompress.huffman.Huffman;
import azgracompress.io.InputData;
import azgracompress.io.loader.IPlaneLoader;
import azgracompress.io.loader.PlaneLoaderFactory;
import azgracompress.quantization.scalar.LloydMaxU16ScalarQuantization;
import azgracompress.quantization.scalar.SQCodebook;
import azgracompress.quantization.scalar.ScalarQuantizer;
import azgracompress.utilities.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;

public class SQImageCompressor extends CompressorDecompressorBase implements IImageCompressor {

    private ScalarQuantizer cachedQuantizer;
    private Huffman cachedHuffman;

    public SQImageCompressor(CompressionOptions options) {
        super(options);
    }

    /**
     * Train Lloyd-Max scalar quantizer from plane data.
     *
     * @param planeData Plane data from which quantizer will be trained.
     * @return Trained scalar quantizer.
     */
    private ScalarQuantizer trainScalarQuantizerFromData(final int[] planeData) {

        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(planeData,
                                                                                   getCodebookSize(),
                                                                                   options.getWorkerCount());
        lloydMax.train();
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCodebook());
    }

    @Override
    public void preloadGlobalCodebook(ICacheFile codebookCacheFile) {
        final SQCodebook cachedCodebook = ((SQCacheFile) codebookCacheFile).getCodebook();
        cachedQuantizer = new ScalarQuantizer(cachedCodebook);
        cachedHuffman = createHuffmanCoder(createHuffmanSymbols(cachedCodebook.getCodebookSize()), cachedCodebook.getSymbolFrequencies());
    }

    /**
     * Writes the scalar quantizer to the compressed stream.
     *
     * @param quantizer      Quantizer used for compression of the image.
     * @param compressStream Compressed data stream.
     * @throws ImageCompressionException when writing to the stream fails.
     */
    private void writeCodebookToOutputStream(final ScalarQuantizer quantizer,
                                             DataOutputStream compressStream) throws ImageCompressionException {
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
        } catch (IOException ioEx) {
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
        QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());

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
    public long[] compress(DataOutputStream compressStream) throws ImageCompressionException {
        final InputData inputDataInfo = options.getInputDataInfo();
        Stopwatch stopwatch = new Stopwatch();
        final boolean hasGeneralQuantizer = options.getCodebookType() != CompressionOptions.CodebookType.Individual;

        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputDataInfo);
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create SCIFIO reader. " + e.getMessage());
        }

        ScalarQuantizer quantizer = null;
        Huffman huffman = null;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        if (options.getCodebookType() == CompressionOptions.CodebookType.Global) {
            reportStatusToListeners("Loading codebook from cache file.");

            quantizer = loadQuantizerFromCache();
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());

            reportStatusToListeners("Cached quantizer with huffman coder created.");
            writeCodebookToOutputStream(quantizer, compressStream);
        } else if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
            stopwatch.restart();
            int[] middlePlaneData;
            final int middlePlaneIndex = getMiddlePlaneIndex();
            try {
                middlePlaneData = planeLoader.loadPlaneData(middlePlaneIndex);
            } catch (IOException ex) {
                throw new ImageCompressionException("Unable to load middle plane data.", ex);
            }

            reportStatusToListeners(String.format("Training scalar quantizer from middle plane %d.", middlePlaneIndex));
            quantizer = trainScalarQuantizerFromData(middlePlaneData);
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());

            stopwatch.stop();
            writeCodebookToOutputStream(quantizer, compressStream);
            reportStatusToListeners("Middle plane codebook with huffman coder created in: " + stopwatch.getElapsedTimeString());
        }

        final int[] planeIndices = getPlaneIndicesForCompression(options.getInputDataInfo());
        long[] planeDataSizes = new long[planeIndices.length];
        int planeCounter = 0;
        for (final int planeIndex : planeIndices) {
            stopwatch.restart();

            int[] planeData;
            try {
                planeData = planeLoader.loadPlaneData(planeIndex);
            } catch (IOException ex) {
                throw new ImageCompressionException("Unable to load plane data.", ex);
            }

            if (!hasGeneralQuantizer) {
                reportStatusToListeners(String.format("Training scalar quantizer from plane %d.", planeIndex));
                quantizer = trainScalarQuantizerFromData(planeData);
                writeCodebookToOutputStream(quantizer, compressStream);

                huffman = new Huffman(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());
                huffman.buildHuffmanTree();
            }

            assert (quantizer != null) : "Scalar Quantizer wasn't initialized.";
            assert (huffman != null) : "Huffman wasn't initialized.";

            final int[] indices = quantizer.quantizeIntoIndices(planeData, 1);

            planeDataSizes[planeCounter++] = writeHuffmanEncodedIndices(compressStream, huffman, indices);

            stopwatch.stop();
            reportProgressToListeners(planeIndex, planeIndices.length,
                                      "Compressed plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString());
        }
        return planeDataSizes;
    }

    private int[] loadConfiguredPlanesData() throws ImageCompressionException {
        final InputData inputDataInfo = options.getInputDataInfo();
        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputDataInfo);
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create SCIFIO reader. " + e.getMessage());
        }
        int[] trainData = null;

        if (inputDataInfo.isPlaneIndexSet()) {
            try {
                reportStatusToListeners("Loading single plane data.");
                trainData = planeLoader.loadPlaneData(inputDataInfo.getPlaneIndex());
            } catch (IOException e) {
                throw new ImageCompressionException("Failed to load plane data.", e);
            }
        } else if (inputDataInfo.isPlaneRangeSet()) {
            reportStatusToListeners("Loading plane range data.");
            final int[] planes = getPlaneIndicesForCompression(options.getInputDataInfo());
            try {
                trainData = planeLoader.loadPlanesU16Data(planes);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ImageCompressionException("Failed to load plane range data.", e);
            }
        } else {
            reportStatusToListeners("Loading all planes data.");
            try {
                trainData = planeLoader.loadAllPlanesU16Data();
            } catch (IOException e) {
                throw new ImageCompressionException("Failed to load all planes data.", e);
            }
        }
        return trainData;
    }

    @Override
    public void trainAndSaveCodebook() throws ImageCompressionException {
        int[] trainData = loadConfiguredPlanesData();

        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(trainData,
                                                                                   getCodebookSize(),
                                                                                   options.getWorkerCount());
        reportStatusToListeners("Starting LloydMax training.");

        lloydMax.setStatusListener(this::reportStatusToListeners);
        lloydMax.train();
        final SQCodebook codebook = lloydMax.getCodebook();
        reportStatusToListeners("Finished LloydMax training.");

        QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getCodebookCacheFolder());
        try {
            final String cacheFilePath = cacheManager.saveCodebook(options.getInputDataInfo().getCacheFileName(), codebook);
            reportStatusToListeners(String.format("Saved cache file to %s", cacheFilePath));
        } catch (IOException e) {
            throw new ImageCompressionException("Unable to write cache.", e);
        }
        reportStatusToListeners("Operation completed.");
    }

    @Override
    public long[] compressStreamChunk(DataOutputStream compressStream, InputData inputData) throws ImageCompressionException {
        throw new ImageCompressionException("Not implemented yet");
    }
}
