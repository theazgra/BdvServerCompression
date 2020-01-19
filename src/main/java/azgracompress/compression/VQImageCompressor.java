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
     * @throws IOException When unable to write quantizer.
     */
    private void writeQuantizerToCompressStream(final VectorQuantizer quantizer,
                                                DataOutputStream compressStream) throws IOException {
        final CodebookEntry[] codebook = quantizer.getCodebook();
        for (final CodebookEntry entry : codebook) {
            final int[] entryVector = entry.getVector();
            for (final int vecVal : entryVector) {
                compressStream.writeShort(vecVal);
            }
        }
        if (options.isVerbose()) {
            Log("Wrote quantization vectors to compressed stream.");
        }
    }

    /**
     * Compress the image file specified by parsed CLI options using vector quantization.
     *
     * @param compressStream Stream to which compressed data will be written.
     * @throws Exception When compress process fails.
     */
    public void compress(DataOutputStream compressStream) throws Exception {
        VectorQuantizer quantizer = null;
        if (options.hasReferencePlaneIndex()) {
            final ImageU16 referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                                   options.getImageDimension(),
                                                                   options.getReferencePlaneIndex());

            Log(String.format("Training vector quantizer from reference plane %d.", options.getReferencePlaneIndex()));
            final int[][] refPlaneVectors = getPlaneVectors(referencePlane);
            quantizer = trainVectorQuantizerFromPlaneVectors(refPlaneVectors);
            writeQuantizerToCompressStream(quantizer, compressStream);
            Log("Wrote reference codebook.");
        }

        final int[] planeIndices = getPlaneIndicesForCompression();

        for (final int planeIndex : planeIndices) {
            Log(String.format("Loading plane %d.", planeIndex));
            final ImageU16 plane = RawDataIO.loadImageU16(options.getInputFile(),
                                                          options.getImageDimension(),
                                                          planeIndex);

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
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
            }
            Log(String.format("Finished processing of plane %d.", planeIndex));
            //            OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerPixel(), 2048);
            //            outBitStream.write(indices);
            //            outBitStream.flush();
            //            Log(String.format("Finished processing of plane %d.", planeIndex));
        }
    }


}
