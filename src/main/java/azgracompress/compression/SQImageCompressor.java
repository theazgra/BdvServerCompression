package azgracompress.compression;

import azgracompress.U16;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.ImageU16;
import azgracompress.huffman.Huffman;
import azgracompress.io.OutBitStream;
import azgracompress.io.RawDataIO;
import azgracompress.quantization.QuantizationValueCache;
import azgracompress.quantization.scalar.LloydMaxU16ScalarQuantization;
import azgracompress.quantization.scalar.ScalarQuantizationCodebook;
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
                                                                                   codebookSize,
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
        final ScalarQuantizationCodebook codebook = quantizer.getCodebook();
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
        QuantizationValueCache cache = new QuantizationValueCache(options.getCodebookCacheFolder());
        try {

            final int[] quantizationValues = cache.readCachedValues(options.getInputFile(),
                                                                    codebookSize);
            // TODO(Moravec): FIXME the null value.
            return new ScalarQuantizer(U16.Min, U16.Max, null);
        } catch (IOException e) {
            throw new ImageCompressionException("Failed to read quantization values from cache file.", e);
        }
    }

    /**
     * Compress the image file specified by parsed CLI options using scalar quantization.
     *
     * @param compressStream Stream to which compressed data will be written.
     * @throws ImageCompressionException When compress process fails.
     */
    public long[] compress(DataOutputStream compressStream) throws ImageCompressionException {
        Stopwatch stopwatch = new Stopwatch();

        final boolean hasGeneralQuantizer = options.hasCodebookCacheFolder() || options.hasReferencePlaneIndex();

        ScalarQuantizer quantizer = null;
        Huffman huffman = null;
        final int[] huffmanSymbols = createHuffmanSymbols();
        if (options.hasCodebookCacheFolder()) {
            // TODO(Moravec): Create huffman.
            Log("Loading codebook from cache file.");
            quantizer = loadQuantizerFromCache();
            Log("Cached quantizer created.");
            writeCodebookToOutputStream(quantizer, compressStream);
        } else if (options.hasReferencePlaneIndex()) {
            // TODO(Moravec): Reference plane will be deprecated in favor of 'middle' plane.
            stopwatch.restart();
            ImageU16 referencePlane = null;
            try {
                referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                        options.getImageDimension(),
                                                        options.getReferencePlaneIndex());
                // TODO(Moravec): Create huffman.
            } catch (Exception ex) {
                throw new ImageCompressionException("Unable to load reference plane data.", ex);
            }


            Log(String.format("Training scalar quantizer from reference plane %d.", options.getReferencePlaneIndex()));
            quantizer = trainScalarQuantizerFromData(referencePlane.getData());
            stopwatch.stop();

            writeCodebookToOutputStream(quantizer, compressStream);

            Log("Reference codebook created in: " + stopwatch.getElapsedTimeString());
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

//            ////////////////////////
//            for (int i = 0; i < indices.length; i++) {
//                final boolean[] huffmanCode = huffman.getCode(indices[i]);
//                HuffmanNode currentHuffmanNode = huffman.getRoot();
//                boolean bit;
//                int index = 0;
//                while (!currentHuffmanNode.isLeaf()) {
//                    bit = huffmanCode[index++];
//                    currentHuffmanNode = currentHuffmanNode.traverse(bit);
//                }
//                assert (indices[i] == currentHuffmanNode.getSymbol());
//            }
//            ////////////////////////////////


            try (OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerPixel(), 2048)) {
                for (final int index : indices) {
                    outBitStream.write(huffman.getCode(index));
                }
                planeDataSizes[planeCounter++] = outBitStream.getBytesWritten();
                //outBitStream.write(indices);
            } catch (Exception ex) {
                throw new ImageCompressionException("Unable to write indices to OutBitStream.", ex);
            }

            // TODO: Fill plane data size
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
                throw new ImageCompressionException("Failed to load reference image data.", e);
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
                                                                                   codebookSize,
                                                                                   options.getWorkerCount());

        Log("Starting LloydMax training.");
        lloydMax.train(options.isVerbose());
        final int[] qValues = lloydMax.getCentroids();
        Log("Finished LloydMax training.");

        Log(String.format("Saving cache file to %s", options.getOutputFile()));
        QuantizationValueCache cache = new QuantizationValueCache(options.getOutputFile());
        try {
            cache.saveQuantizationValues(options.getInputFile(), qValues);
        } catch (IOException e) {
            throw new ImageCompressionException("Unable to write cache.", e);
        }
        Log("Operation completed.");
    }
}
