package azgracompress.compression;

import azgracompress.cache.QuantizationCacheManager;
import azgracompress.cli.InputFileInfo;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.Chunk2D;
import azgracompress.data.ImageU16;
import azgracompress.huffman.Huffman;
import azgracompress.io.IPlaneLoader;
import azgracompress.io.PlaneLoaderFactory;
import azgracompress.quantization.vector.*;
import azgracompress.utilities.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;

public class VQImageCompressor extends CompressorDecompressorBase implements IImageCompressor {

    public VQImageCompressor(ParsedCliOptions options) {
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
                                                                  options.getVectorDimension().toV3i());
        LBGResult vqResult = vqInitializer.findOptimalCodebook(false);
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
            Log("Wrote quantization vectors to compressed stream.");
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

        final VQCodebook codebook = cacheManager.loadVQCodebook(options.getInputFileInfo().getFilePath(),
                                                                getCodebookSize(),
                                                                options.getVectorDimension().toV3i());
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
    public long[] compress(DataOutputStream compressStream) throws ImageCompressionException {
        final InputFileInfo inputFileInfo = options.getInputFileInfo();
        Stopwatch stopwatch = new Stopwatch();
        final boolean hasGeneralQuantizer = options.hasCodebookCacheFolder() || options.shouldUseMiddlePlane();
        final IPlaneLoader planeLoader;
        final int[] huffmanSymbols = createHuffmanSymbols(getCodebookSize());
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputFileInfo);
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create SCIFIO reader. " + e.getMessage());
        }
        VectorQuantizer quantizer = null;
        Huffman huffman = null;

        if (options.hasCodebookCacheFolder()) {
            Log("Loading codebook from cache file.");
            quantizer = loadQuantizerFromCache();
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
            Log("Cached quantizer with huffman coder created.");
            writeQuantizerToCompressStream(quantizer, compressStream);
        } else if (options.shouldUseMiddlePlane()) {
            stopwatch.restart();

            final int middlePlaneIndex = getMiddlePlaneIndex();
            ImageU16 middlePlane = null;
            try {
                middlePlane = planeLoader.loadPlaneU16(middlePlaneIndex);
            } catch (IOException ex) {
                throw new ImageCompressionException("Unable to load reference plane data.", ex);
            }

            Log(String.format("Training vector quantizer from middle plane %d.", middlePlaneIndex));
            final int[][] refPlaneVectors = middlePlane.toQuantizationVectors(options.getVectorDimension());
            quantizer = trainVectorQuantizerFromPlaneVectors(refPlaneVectors);
            huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
            writeQuantizerToCompressStream(quantizer, compressStream);
            stopwatch.stop();
            Log("Middle plane codebook created in: " + stopwatch.getElapsedTimeString());
        }

        final int[] planeIndices = getPlaneIndicesForCompression();
        long[] planeDataSizes = new long[planeIndices.length];
        int planeCounter = 0;

        for (final int planeIndex : planeIndices) {
            stopwatch.restart();
            Log(String.format("Loading plane %d.", planeIndex));

            ImageU16 plane = null;
            try {
                plane = planeLoader.loadPlaneU16(planeIndex);
            } catch (IOException ex) {
                throw new ImageCompressionException("Unable to load plane data.", ex);
            }

            final int[][] planeVectors = plane.toQuantizationVectors(options.getVectorDimension());

            if (!hasGeneralQuantizer) {
                Log(String.format("Training vector quantizer from plane %d.", planeIndex));
                quantizer = trainVectorQuantizerFromPlaneVectors(planeVectors);
                huffman = createHuffmanCoder(huffmanSymbols, quantizer.getFrequencies());
                writeQuantizerToCompressStream(quantizer, compressStream);
                Log("Wrote plane codebook.");
            }

            assert (quantizer != null);

            Log("Compressing plane...");
            final int[] indices = quantizer.quantizeIntoIndices(planeVectors, options.getWorkerCount());

            planeDataSizes[planeCounter++] = writeHuffmanEncodedIndices(compressStream, huffman, indices);

            stopwatch.stop();
            Log("Plane time: " + stopwatch.getElapsedTimeString());
            Log(String.format("Finished processing of plane %d.", planeIndex));
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
        ImageU16 refPlane = planeLoader.loadPlaneU16(planeIndex);
        return refPlane.toQuantizationVectors(options.getVectorDimension());
    }

    private int[][] loadConfiguredPlanesData() throws ImageCompressionException {
        final int vectorSize = options.getVectorDimension().getX() * options.getVectorDimension().getY();
        final InputFileInfo inputFileInfo = options.getInputFileInfo();
        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(inputFileInfo);
        } catch (Exception e) {
            throw new ImageCompressionException("Unable to create SCIFIO reader. " + e.getMessage());
        }
        int[][] trainData = null;
        Stopwatch s = new Stopwatch();
        s.start();
        if (inputFileInfo.isPlaneIndexSet()) {
            Log("VQ: Loading single plane data.");
            try {

                trainData = loadPlaneQuantizationVectors(planeLoader, inputFileInfo.getPlaneIndex());
            } catch (IOException e) {
                throw new ImageCompressionException("Failed to load plane data.", e);
            }
        } else {
            Log(inputFileInfo.isPlaneRangeSet() ? "VQ: Loading plane range data." : "VQ: Loading all planes data.");
            final int[] planeIndices = getPlaneIndicesForCompression();

            final int chunkCountPerPlane = Chunk2D.calculateRequiredChunkCountPerPlane(
                    inputFileInfo.getDimensions().toV2i(),
                    options.getVectorDimension());
            final int totalChunkCount = chunkCountPerPlane * planeIndices.length;

            trainData = new int[totalChunkCount][vectorSize];

            int[][] planeVectors;
            int planeCounter = 0;
            for (final int planeIndex : planeIndices) {
                try {
                    planeVectors = loadPlaneQuantizationVectors(planeLoader, planeIndex);
                    assert (planeVectors.length == chunkCountPerPlane) : "Wrong chunk count per plane";
                } catch (IOException e) {
                    throw new ImageCompressionException(String.format("Failed to load plane %d image data.",
                                                                      planeIndex), e);
                }

                System.arraycopy(planeVectors,
                                 0,
                                 trainData,
                                 (planeCounter * chunkCountPerPlane),
                                 chunkCountPerPlane);
                ++planeCounter;
            }
        }
        s.stop();
        Log("Quantization vector load took: " + s.getElapsedTimeString());
        return trainData;
    }

    @Override
    public void trainAndSaveCodebook() throws ImageCompressionException {
        final int[][] trainingData = loadConfiguredPlanesData();

        LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(trainingData,
                                                                  getCodebookSize(),
                                                                  options.getWorkerCount(),
                                                                  options.getVectorDimension().toV3i());
        Log("Starting LBG optimization.");
        LBGResult lbgResult = vqInitializer.findOptimalCodebook(options.isVerbose());
        Log("Learned the optimal codebook.");

        Log("Saving cache file to %s", options.getOutputFile());
        QuantizationCacheManager cacheManager = new QuantizationCacheManager(options.getOutputFile());
        try {
            cacheManager.saveCodebook(options.getInputFileInfo().getFilePath(), lbgResult.getCodebook());
        } catch (IOException e) {
            throw new ImageCompressionException("Unable to write VQ cache.", e);
        }
        Log("Operation completed.");
    }


}
