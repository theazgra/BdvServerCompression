package azgracompress.compression;

import azgracompress.U16;
import azgracompress.cache.QuantizationCacheManager;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.ImageU16;
import azgracompress.huffman.Huffman;
import azgracompress.io.RawDataIO;
import azgracompress.quantization.scalar.LloydMaxU16ScalarQuantization;
import azgracompress.quantization.scalar.SQCodebook;
import azgracompress.quantization.scalar.ScalarQuantizer;
import azgracompress.utilities.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;

public class SQImageCompressor extends CompressorDecompressorBase implements IImageCompressor {

    public SQImageCompressor(ParsedCliOptions options) {
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
        lloydMax.train(false);
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCodebook());
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
            Log("Wrote quantization values to compressed stream.");
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

        final SQCodebook codebook = cacheManager.loadSQCodebook(options.getInputFile(), getCodebookSize());
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
    public long[] compress(DataOutputStream compressStream) throws ImageCompressionException {
        Stopwatch stopwatch = new Stopwatch();
        final boolean hasGeneralQuantizer = options.hasCodebookCacheFolder() || options.shouldUseMiddlePlane();
        ScalarQuantizer quantizer = null;
        Huffman huffman = null;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        if (options.hasCodebookCacheFolder()) {
            Log("Loading codebook from cache file.");

            quantizer = loadQuantizerFromCache();
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());

            Log("Cached quantizer with huffman coder created.");
            writeCodebookToOutputStream(quantizer, compressStream);
        } else if (options.shouldUseMiddlePlane()) {
            stopwatch.restart();
            ImageU16 middlePlane = null;
            final int middlePlaneIndex = getMiddlePlaneIndex();
            try {
                middlePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                     options.getImageDimension(),
                                                     getMiddlePlaneIndex());
            } catch (Exception ex) {
                throw new ImageCompressionException("Unable to load plane data.", ex);
            }

            Log(String.format("Training scalar quantizer from middle plane %d.", middlePlaneIndex));
            quantizer = trainScalarQuantizerFromData(middlePlane.getData());
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());

            stopwatch.stop();
            writeCodebookToOutputStream(quantizer, compressStream);
            Log("Middle plane codebook with huffman coder created in: " + stopwatch.getElapsedTimeString());
        }

        final int[] planeIndices = getPlaneIndicesForCompression();
        long[] planeDataSizes = new long[planeIndices.length];
        int planeCounter = 0;
        for (final int planeIndex : planeIndices) {
            stopwatch.restart();
            Log(String.format("Loading plane %d.", planeIndex));

            ImageU16 plane = null;

            try {
                plane = RawDataIO.loadImageU16(options.getInputFile(),
                                               options.getImageDimension(),
                                               planeIndex);
            } catch (Exception ex) {
                throw new ImageCompressionException("Unable to load plane data.", ex);
            }

            if (!hasGeneralQuantizer) {
                Log(String.format("Training scalar quantizer from plane %d.", planeIndex));
                quantizer = trainScalarQuantizerFromData(plane.getData());
                writeCodebookToOutputStream(quantizer, compressStream);

                huffman = new Huffman(huffmanSymbols, quantizer.getCodebook().getSymbolFrequencies());
                huffman.buildHuffmanTree();
            }

            assert (quantizer != null) : "Scalar Quantizer wasn't initialized.";
            assert (huffman != null) : "Huffman wasn't initialized.";

            Log("Compressing plane...");
            final int[] indices = quantizer.quantizeIntoIndices(plane.getData(), 1);

            planeDataSizes[planeCounter++] = writeHuffmanEncodedIndices(compressStream,huffman, indices);

            stopwatch.stop();
            Log("Plane time: " + stopwatch.getElapsedTimeString());
            Log(String.format("Finished processing of plane %d", planeIndex));
        }
        return planeDataSizes;
    }

    private int[] loadConfiguredPlanesData() throws ImageCompressionException {
        int[] trainData = null;
        if (options.hasPlaneIndexSet()) {
            try {
                Log("Loading single plane data.");
                trainData = RawDataIO.loadImageU16(options.getInputFile(),
                                                   options.getImageDimension(),
                                                   options.getPlaneIndex()).getData();
            } catch (IOException e) {
                throw new ImageCompressionException("Failed to load plane data.", e);
            }
        } else if (options.hasPlaneRangeSet()) {
            Log("Loading plane range data.");
            final int[] planes = getPlaneIndicesForCompression();
            try {
                trainData = RawDataIO.loadPlanesData(options.getInputFile(), options.getImageDimension(), planes);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ImageCompressionException("Failed to load plane range data.", e);
            }
        } else {
            Log("Loading all planes data.");
            try {
                trainData = RawDataIO.loadAllPlanesData(options.getInputFile(), options.getImageDimension());
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
        Log("Starting LloydMax training.");
        lloydMax.train(options.isVerbose());
        final SQCodebook codebook = lloydMax.getCodebook();
        final int[] qValues = codebook.getCentroids();
        Log("Finished LloydMax training.");

        Log(String.format("Saving cache file to %s", options.getOutputFile()));
        QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getOutputFile());
        try {
            cacheManager.saveCodebook(options.getInputFile(), codebook);
        } catch (IOException e) {
            throw new ImageCompressionException("Unable to write cache.", e);
        }
        Log("Operation completed.");
    }
}
