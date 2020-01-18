package compression;

import cli.ParsedCliOptions;
import compression.data.ImageU16;
import compression.fileformat.QCMPFileHeader;
import compression.io.OutBitStream;
import compression.io.RawDataIO;
import compression.quantization.scalar.LloydMaxU16ScalarQuantization;
import compression.quantization.scalar.ScalarQuantizer;

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
                break;
            case Vector2D:
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
                                             DataOutputStream outputStream) throws IOException {
        final int[] centroids = quantizer.getCentroids();
        for (final int quantizationValue : centroids) {
            outputStream.writeShort(quantizationValue);
        }
    }

    private ScalarQuantizer getScalarQuantizerFromPlane(final ImageU16 plane) {

        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(plane.getData(), codebookSize);
        lloydMax.train(false);
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCentroids());
    }

    private void compressUsingScalarQuantization(DataOutputStream outputStream) throws Exception {
        ScalarQuantizer quantizer = null;
        if (options.hasReferencePlaneIndex()) {
            final ImageU16 referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                                   options.getImageDimension(),
                                                                   options.getReferencePlaneIndex());

            Log("Creating codebook from reference plane...");
            quantizer = getScalarQuantizerFromPlane(referencePlane);
            writeCodebookToOutputStream(quantizer, outputStream);
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
                writeCodebookToOutputStream(quantizer, outputStream);
                Log("Wrote plane codebook.");
            }

            assert (quantizer != null);

            Log("Writing quantization indices...");
            final int[] indices = quantizer.quantizeIntoIndices(plane.getData());
            OutBitStream outBitStream = new OutBitStream(outputStream, options.getBitsPerPixel(), 2048);
            outBitStream.write(indices);
            outBitStream.flush();
            Log(String.format("Finished processing of plane %d", planeIndex));
        }
    }
}
