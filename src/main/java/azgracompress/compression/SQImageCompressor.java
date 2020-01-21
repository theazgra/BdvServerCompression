package azgracompress.compression;

import azgracompress.U16;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.ImageU16;
import azgracompress.io.OutBitStream;
import azgracompress.io.RawDataIO;
import azgracompress.quantization.scalar.LloydMaxU16ScalarQuantization;
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
        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(planeData, codebookSize);
        lloydMax.train(false);
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCentroids());
    }

    /**
     * Writes the scalar quantizer to the compressed stream.
     *
     * @param quantizer      Quantizer used for compression of the image.
     * @param compressStream Compressed data stream.
     * @throws IOException when writing to the stream fails.
     */
    private void writeCodebookToOutputStream(final ScalarQuantizer quantizer,
                                             DataOutputStream compressStream) throws IOException {
        final int[] centroids = quantizer.getCentroids();
        for (final int quantizationValue : centroids) {
            compressStream.writeShort(quantizationValue);
        }
        if (options.isVerbose()) {
            Log("Wrote quantization values to compressed stream.");
        }
    }

    /**
     * Compress the image file specified by parsed CLI options using scalar quantization.
     *
     * @param compressStream Stream to which compressed data will be written.
     * @throws Exception When compress process fails.
     */
    public void compress(DataOutputStream compressStream) throws Exception {
        ScalarQuantizer quantizer = null;
        Stopwatch stopwatch = new Stopwatch();
        if (options.hasReferencePlaneIndex()) {
            stopwatch.restart();
            final ImageU16 referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                                   options.getImageDimension(),
                                                                   options.getReferencePlaneIndex());

            Log(String.format("Training scalar quantizer from reference plane %d.", options.getReferencePlaneIndex()));
            quantizer = trainScalarQuantizerFromData(referencePlane.getData());
            stopwatch.stop();
            writeCodebookToOutputStream(quantizer, compressStream);
            Log("Reference codebook created in: " + stopwatch.getElapsedTimeString());
        }

        final int[] planeIndices = getPlaneIndicesForCompression();
        for (final int planeIndex : planeIndices) {
            stopwatch.restart();
            Log(String.format("Loading plane %d.", planeIndex));
            final ImageU16 plane = RawDataIO.loadImageU16(options.getInputFile(),
                                                          options.getImageDimension(),
                                                          planeIndex);

            if (!options.hasReferencePlaneIndex()) {
                Log(String.format("Training scalar quantizer from plane %d.", planeIndex));
                quantizer = trainScalarQuantizerFromData(plane.getData());
                writeCodebookToOutputStream(quantizer, compressStream);
            }

            assert (quantizer != null);

            Log("Compressing plane...");
            final int[] indices = quantizer.quantizeIntoIndices(plane.getData());

            try (OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerPixel(), 2048)) {
                outBitStream.write(indices);
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
            }
            stopwatch.stop();
            Log("Plane time: " + stopwatch.getElapsedTimeString());
            Log(String.format("Finished processing of plane %d", planeIndex));
        }
    }
}
