package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.fileformat.QCMPFileHeader;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

public class ImageCompressor extends CompressorDecompressorBase {

    private final int codebookSize;

    public ImageCompressor(ParsedCliOptions options) {
        super(options);
        codebookSize = (int) Math.pow(2, options.getBitsPerPixel());
    }


    public void compress() throws Exception {

        Log(String.format("Compression with BPP = %d", options.getBitsPerPixel()));

        FileOutputStream fos = new FileOutputStream(options.getOutputFile(), false);
        DataOutputStream compressStream = new DataOutputStream(new BufferedOutputStream(fos, 8192));

        // Create and write header to output stream.
        final QCMPFileHeader header = createHeader();
        header.writeHeader(compressStream);

        boolean compressionResult = true;
        switch (options.getQuantizationType()) {
            case Scalar: {
                SQImageCompressor compressor = new SQImageCompressor(options);
                compressor.compress(compressStream);
            }
            break;
            case Vector1D:
            case Vector2D: {
                VQImageCompressor compressor = new VQImageCompressor(options);
                compressor.compress(compressStream);
            }
            break;
            case Vector3D:
            case Invalid:
                throw new Exception("Not supported quantization type");
        }

        compressStream.flush();
        fos.flush();

        compressStream.close();
        fos.close();
    }


    private QCMPFileHeader createHeader() {
        QCMPFileHeader header = new QCMPFileHeader();

        header.setQuantizationType(options.getQuantizationType());
        header.setBitsPerPixel((byte) options.getBitsPerPixel());
        header.setCodebookPerPlane(!options.hasReferencePlaneIndex());

        header.setImageSizeX(options.getImageDimension().getX());
        header.setImageSizeY(options.getImageDimension().getY());
        header.setImageSizeZ(options.getNumberOfPlanes());

        // If plane index is set then, we are compressing only one plane.
        if (options.hasPlaneIndexSet()) {
            header.setImageSizeZ(1);
        }

        header.setVectorDimension(options.getVectorDimension());

        return header;
    }


}
