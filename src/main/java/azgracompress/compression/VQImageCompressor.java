package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.Chunk2D;
import azgracompress.data.ImageU16;
import azgracompress.data.V2i;
import azgracompress.io.OutBitStream;
import azgracompress.io.RawDataIO;
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
        final V2i qVector = options.getVectorDimension();

        if (qVector.getY() > 1) {
            // 2D Quantization, return `matrices`.
            return Chunk2D.chunksAsImageVectors(plane.as2dChunk().divideIntoChunks(qVector));
        } else {
            // 1D Quantization, return row vectors.
            return plane.as2dChunk().divideInto1DVectors(qVector.getX());
        }
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
     * Compress the image file specified by parsed CLI options using vector quantization.
     *
     * @param compressStream Stream to which compressed data will be written.
     * @throws ImageCompressionException When compress process fails.
     */
    public void compress(DataOutputStream compressStream) throws ImageCompressionException {
        VectorQuantizer quantizer = null;
        Stopwatch stopwatch = new Stopwatch();
        if (options.hasReferencePlaneIndex()) {
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

            if (!options.hasReferencePlaneIndex()) {
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


}
