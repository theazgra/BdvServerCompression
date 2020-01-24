package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.data.Chunk2D;
import azgracompress.data.ImageU16;
import azgracompress.io.OutBitStream;
import azgracompress.io.RawDataIO;
import azgracompress.quantization.QuantizationValueCache;
import azgracompress.quantization.vector.CodebookEntry;
import azgracompress.quantization.vector.LBGResult;
import azgracompress.quantization.vector.LBGVectorQuantizer;
import azgracompress.quantization.vector.VectorQuantizer;
import azgracompress.utilities.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;

public class VQImageCompressor extends CompressorDecompressorBase implements IImageCompressor {

    public VQImageCompressor(ParsedCliOptions options) {
        super(options);
    }

    /**
     * Get image vectors from the plane. Vector dimensions are specified by parsed CLI options.
     *
     * @param plane Image plane.
     * @return Image vectors.
     */
    private int[][] getPlaneVectors(final ImageU16 plane) {
        return plane.toQuantizationVectors(options.getVectorDimension());
    }

    /**
     * Train vector quantizer from plane vectors.
     *
     * @param planeVectors Image vectors.
     * @return Trained vector quantizer with codebook of set size.
     */
    private VectorQuantizer trainVectorQuantizerFromPlaneVectors(final int[][] planeVectors) {
        LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeVectors, codebookSize);
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
        final CodebookEntry[] codebook = quantizer.getCodebook();
        try {
            for (final CodebookEntry entry : codebook) {
                final int[] entryVector = entry.getVector();
                for (final int vecVal : entryVector) {
                    compressStream.writeShort(vecVal);
                }
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
        QuantizationValueCache cache = new QuantizationValueCache(options.getCodebookCacheFolder());
        try {
            final CodebookEntry[] codebook = cache.readCachedValues(options.getInputFile(),
                                                                    codebookSize,
                                                                    options.getVectorDimension().getX(),
                                                                    options.getVectorDimension().getY());
            return new VectorQuantizer(codebook);

        } catch (IOException e) {
            throw new ImageCompressionException("Failed to read quantization vectors from cache.", e);
        }
    }

    /**
     * Compress the image file specified by parsed CLI options using vector quantization.
     *
     * @param compressStream Stream to which compressed data will be written.
     * @throws ImageCompressionException When compress process fails.
     */
    public void compress(DataOutputStream compressStream) throws ImageCompressionException {
        Stopwatch stopwatch = new Stopwatch();
        final boolean hasGeneralQuantizer = options.hasCodebookCacheFolder() || options.hasReferencePlaneIndex();
        VectorQuantizer quantizer = null;

        if (options.hasCodebookCacheFolder()) {
            Log("Loading codebook from cache file.");
            quantizer = loadQuantizerFromCache();
            Log("Cached quantizer created.");
        } else if (options.hasReferencePlaneIndex()) {
            stopwatch.restart();

            ImageU16 referencePlane = null;
            try {
                referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                        options.getImageDimension(),
                                                        options.getReferencePlaneIndex());
            } catch (Exception ex) {
                throw new ImageCompressionException("Unable to load reference plane data.", ex);
            }

            Log(String.format("Training vector quantizer from reference plane %d.", options.getReferencePlaneIndex()));
            final int[][] refPlaneVectors = getPlaneVectors(referencePlane);
            quantizer = trainVectorQuantizerFromPlaneVectors(refPlaneVectors);
            writeQuantizerToCompressStream(quantizer, compressStream);
            stopwatch.stop();
            Log("Reference codebook created in: " + stopwatch.getElapsedTimeString());
        }

        final int[] planeIndices = getPlaneIndicesForCompression();


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

            final int[][] planeVectors = getPlaneVectors(plane);

            if (!hasGeneralQuantizer) {
                Log(String.format("Training vector quantizer from plane %d.", planeIndex));
                quantizer = trainVectorQuantizerFromPlaneVectors(planeVectors);
                writeQuantizerToCompressStream(quantizer, compressStream);
                Log("Wrote plane codebook.");
            }

            assert (quantizer != null);

            Log("Compression plane...");
            final int[] indices = quantizer.quantizeIntoIndices(planeVectors);

            try (OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerPixel(), 2048)) {
                outBitStream.write(indices);
            } catch (Exception ex) {
                throw new ImageCompressionException("Unable to write indices to OutBitStream.", ex);
            }
            stopwatch.stop();
            Log("Plane time: " + stopwatch.getElapsedTimeString());
            Log(String.format("Finished processing of plane %d.", planeIndex));
        }
    }


    /**
     * Load plane and convert the plane into quantization vectors.
     *
     * @param planeIndex Zero based plane index.
     * @return Quantization vectors of configured quantization.
     * @throws IOException When reading fails.
     */
    private int[][] loadPlaneQuantizationVectors(final int planeIndex) throws IOException {
        ImageU16 refPlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                   options.getImageDimension(),
                                                   planeIndex);

        return refPlane.toQuantizationVectors(options.getVectorDimension());
    }

    private int[][] loadConfiguredPlanesData() throws ImageCompressionException {
        final int vectorSize = options.getVectorDimension().getX() * options.getVectorDimension().getY();
        int[][] trainData = null;
        Stopwatch s = new Stopwatch();
        s.start();
        if (options.hasPlaneIndexSet()) {
            Log("VQ: Loading single plane data.");
            try {
                trainData = loadPlaneQuantizationVectors(options.getPlaneIndex());
            } catch (IOException e) {
                throw new ImageCompressionException("Failed to load reference image data.", e);
            }
        } else {
            Log(options.hasPlaneRangeSet() ? "Loading plane range data." : "Loading all planes data.");
            final int[] planeIndices = getPlaneIndicesForCompression();

            final int chunkCountPerPlane = Chunk2D.calculateRequiredChunkCountPerPlane(
                    options.getImageDimension().toV2i(),
                    options.getVectorDimension());
            final int totalChunkCount = chunkCountPerPlane * planeIndices.length;

            trainData = new int[totalChunkCount][vectorSize];

            int[][] planeVectors;
            int planeCounter = 0;
            for (final int planeIndex : planeIndices) {
                Log("Loading plane %d vectors", planeIndex);
                try {
                    planeVectors = loadPlaneQuantizationVectors(planeIndex);
                    assert (planeVectors.length == chunkCountPerPlane) : "Wrong chunk count per plane";
                } catch (IOException e) {
                    throw new ImageCompressionException(String.format("Failed to load plane %d image data.",
                                                                      planeIndex), e);
                }

                System.arraycopy(planeVectors, 0, trainData, (planeCounter * chunkCountPerPlane), chunkCountPerPlane);
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

        LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(trainingData, codebookSize);
        Log("Starting LBG optimization.");
        LBGResult lbgResult = vqInitializer.findOptimalCodebook(options.isVerbose());
        Log("Learned the optimal codebook.");


        Log("Saving cache file to %s", options.getOutputFile());
        QuantizationValueCache cache = new QuantizationValueCache(options.getOutputFile());
        try {
            cache.saveQuantizationValues(options.getInputFile(), lbgResult.getCodebook());
        } catch (IOException e) {
            throw new ImageCompressionException("Unable to write cache.", e);
        }
        Log("Operation completed.");
    }


}
