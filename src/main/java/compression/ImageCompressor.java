package compression;

import cli.ParsedCliOptions;
import compression.data.ImageU16;
import compression.fileformat.QCMPFileHeader;
import compression.io.OutBitStream;
import compression.io.RawDataIO;
import compression.quantization.scalar.LloydMaxU16ScalarQuantization;
import compression.quantization.scalar.ScalarQuantizer;

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
        DataOutputStream dataOutputStream = new DataOutputStream(fos);

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
        dataOutputStream.close();
        fos.flush();

        fos.close();
    }

    private QCMPFileHeader createHeader() {
        QCMPFileHeader header = new QCMPFileHeader();

        header.setQuantizationType(options.getQuantizationType());
        header.setBitsPerPixel((byte) options.getBitsPerPixel());
        header.setCodebookPerPlane(!options.hasReferencePlaneIndex());

        header.setImageDimension(options.getImageDimension());
        header.setVectorDimension(options.getVectorDimension());

        return header;
    }

    private void writeCodebookToOutputStream(final ScalarQuantizer quantizer,
                                             DataOutputStream outputStream) throws IOException {
        final int[] centroids = quantizer.getCentroids();
        for (final int quantizationValue : centroids) {
            // TODO(Moravec): Check this!
            outputStream.writeShort(quantizationValue);
            //outputStream.writeShort(TypeConverter.intToShort(quantizationValue));
        }
    }

    private ScalarQuantizer getScalarQuantizerFromPlane(final ImageU16 plane) {

        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(plane.getData(), codebookSize);
        lloydMax.train(options.isVerbose());
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCentroids());
    }

    private void compressUsingScalarQuantization(DataOutputStream outputStream) throws Exception {
        ScalarQuantizer quantizer = null;
        if (options.hasReferencePlaneIndex()) {
            final ImageU16 referencePlane = RawDataIO.loadImageU16(options.getInputFile(),
                                                                   options.getImageDimension(),
                                                                   options.getReferencePlaneIndex());
            quantizer = getScalarQuantizerFromPlane(referencePlane);
            writeCodebookToOutputStream(quantizer, outputStream);
        }

        final int[] planeIndices = getPlaneIndicesForCompression();

        for (final int planeIndex : planeIndices) {
            final ImageU16 plane = RawDataIO.loadImageU16(options.getInputFile(),
                                                          options.getImageDimension(),
                                                          planeIndex);

            if (!options.hasReferencePlaneIndex()) {
                quantizer = getScalarQuantizerFromPlane(plane);
                writeCodebookToOutputStream(quantizer, outputStream);
            }

            assert (quantizer != null);
            final int[] indices = quantizer.quantizeIntoIndices(plane.getData());

            OutBitStream outBitStream = new OutBitStream(outputStream, options.getBitsPerPixel(), 2048);
            outBitStream.write(indices);
            outBitStream.flush();
        }
    }
}
