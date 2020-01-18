package compression;

import cli.ParsedCliOptions;
import compression.data.Chunk2D;
import compression.data.ImageU16;
import compression.data.V2i;
import compression.fileformat.QCMPFileHeader;
import compression.io.OutBitStream;
import compression.io.RawDataIO;
import compression.quantization.scalar.LloydMaxU16ScalarQuantization;
import compression.quantization.scalar.ScalarQuantizer;
import compression.quantization.vector.CodebookEntry;
import compression.quantization.vector.LBGResult;
import compression.quantization.vector.LBGVectorQuantizer;
import compression.quantization.vector.VectorQuantizer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageCompressor extends CompressorDecompressorBase {

    private final int codebookSize;

    public ImageCompressor(ParsedCliOptions options) {
        super(options);
        codebookSize = (int) Math.pow(2, options.getBitsPerPixel());
    }


    public void compress() throws Exception {

        FileOutputStream fos = new FileOutputStream(options.getOutputFile(), false);
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(fos, 8192));

        // Create and write header to output stream.
        final QCMPFileHeader header = createHeader();
        header.writeHeader(dataOutputStream);

        boolean compressionResult = true;
        switch (options.getQuantizationType()) {
            case Scalar:
                compressUsingScalarQuantization(dataOutputStream);
                break;
            case Vector1D:
            case Vector2D:
                compressUsingVectorQuantization(dataOutputStream);
                break;
            case Vector3D:
            case Invalid:
                throw new Exception("Not supported quantization type");
        }

        dataOutputStream.flush();
        fos.flush();

        dataOutputStream.close();
        fos.close();
    }


    private ScalarQuantizer getScalarQuantizerFromPlane(final ImageU16 plane) {

        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(plane.getData(), codebookSize);
        lloydMax.train(false);
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCentroids());
    }

    private int[][] getPlaneVectors(final ImageU16 plane) {
        final V2i qVector = options.getVectorDimension();

        if (qVector.getY() > 1) {
            // 2D Quantization.
            return Chunk2D.chunksAsImageVectors(plane.as2dChunk().divideIntoChunks(qVector));
        } else {
            // 1D Quantization.
            return plane.as2dChunk().divideInto1DVectors(qVector.getX());
        }
    }

    private VectorQuantizer trainVectorQuantizerFromPlaneVectors(final int[][] planeVectors) {
        LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeVectors, codebookSize);
        LBGResult vqResult = vqInitializer.findOptimalCodebook(false);
        // TODO(Moravec): If verbose ask initializer for result.
        return new VectorQuantizer(vqResult.getCodebook());
    }

    private void compressUsingVectorQuantization(DataOutputStream compressStream) throws Exception {
        VectorQuantizer quantizer = null;
        if (options.hasReferencePlaneIndex()) {
            final ImageU16 referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                                   options.getImageDimension(),
                                                                   options.getReferencePlaneIndex());

            Log("Creating codebook from reference plane...");
            quantizer = trainVectorQuantizerFromPlaneVectors(getPlaneVectors(referencePlane));
            writeCodebookToOutputStream(quantizer, compressStream);
            Log("Wrote reference codebook.");
        }

        final int[] planeIndices = getPlaneIndicesForCompression();

        for (final int planeIndex : planeIndices) {
            Log(String.format("Loading plane %d...", planeIndex));
            final ImageU16 plane = RawDataIO.loadImageU16(options.getInputFile(),
                                                          options.getImageDimension(),
                                                          planeIndex);

            final int[][] planeVectors = getPlaneVectors(plane);

            if (!options.hasReferencePlaneIndex()) {
                Log("Creating plane codebook...");
                quantizer = trainVectorQuantizerFromPlaneVectors(planeVectors);
                writeCodebookToOutputStream(quantizer, compressStream);
                Log("Wrote plane codebook.");
            }

            assert (quantizer != null);

            Log("Writing quantization indices...");
            final int[] indices = quantizer.quantizeIntoIndices(planeVectors);

            OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerPixel(), 2048);
            outBitStream.write(indices);
            outBitStream.flush();
            Log(String.format("Finished processing of plane %d", planeIndex));
        }
    }

    private QCMPFileHeader createHeader() {
        QCMPFileHeader header = new QCMPFileHeader();

        header.setQuantizationType(options.getQuantizationType());
        header.setBitsPerPixel((byte) options.getBitsPerPixel());
        header.setCodebookPerPlane(!options.hasReferencePlaneIndex());

        header.setImageDimension(options.getImageDimension());

        // If plane index is set then, we are compressing only one plane.
        if (options.hasPlaneIndexSet()) {
            header.setImageSizeZ(1);
        }

        header.setVectorDimension(options.getVectorDimension());

        return header;
    }

    private void writeCodebookToOutputStream(final ScalarQuantizer quantizer,
                                             DataOutputStream compressStream) throws IOException {
        final int[] centroids = quantizer.getCentroids();
        for (final int quantizationValue : centroids) {
            compressStream.writeShort(quantizationValue);
        }
    }

    private void writeCodebookToOutputStream(final VectorQuantizer quantizer,
                                             DataOutputStream compressStream) throws IOException {
        final CodebookEntry[] codebook = quantizer.getCodebook();
        for (final CodebookEntry entry : codebook) {
            final int[] entryVector = entry.getVector();
            for (final int vecVal : entryVector) {
                compressStream.writeShort(vecVal);
            }
        }
    }


    private void compressUsingScalarQuantization(DataOutputStream compressStream) throws Exception {
        ScalarQuantizer quantizer = null;
        if (options.hasReferencePlaneIndex()) {
            final ImageU16 referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                                   options.getImageDimension(),
                                                                   options.getReferencePlaneIndex());

            Log("Creating codebook from reference plane...");
            quantizer = getScalarQuantizerFromPlane(referencePlane);
            writeCodebookToOutputStream(quantizer, compressStream);
            Log("Wrote reference codebook.");
        }

        final int[] planeIndices = getPlaneIndicesForCompression();

        for (final int planeIndex : planeIndices) {
            Log(String.format("Loading plane %d...", planeIndex));
            final ImageU16 plane = RawDataIO.loadImageU16(options.getInputFile(),
                                                          options.getImageDimension(),
                                                          planeIndex);

            if (!options.hasReferencePlaneIndex()) {
                Log("Creating plane codebook...");
                quantizer = getScalarQuantizerFromPlane(plane);
                writeCodebookToOutputStream(quantizer, compressStream);
                Log("Wrote plane codebook.");
            }

            assert (quantizer != null);

            Log("Writing quantization indices...");
            final int[] indices = quantizer.quantizeIntoIndices(plane.getData());
            OutBitStream outBitStream = new OutBitStream(compressStream, options.getBitsPerPixel(), 2048);
            outBitStream.write(indices);
            outBitStream.flush();
            Log(String.format("Finished processing of plane %d", planeIndex));
        }
    }
}
