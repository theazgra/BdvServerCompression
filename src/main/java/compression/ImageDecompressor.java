package compression;

import cli.ParsedCliOptions;
import compression.fileformat.QCMPFileHeader;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ImageDecompressor extends CompressorDecompressorBase {
    public ImageDecompressor(ParsedCliOptions options) {
        super(options);
    }

    public boolean decompress() {
        return true;
    }

    private DataInputStream openCompressedFile() throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(options.getInputFile());
        return new DataInputStream(fis);
    }

    private QCMPFileHeader readQCMPFileHeader(DataInputStream inputStream) throws IOException {
        QCMPFileHeader header = new QCMPFileHeader();
        if (!header.readHeader(inputStream)) {
            // Not valid QCMP file.
            return null;
        }
        return header;
    }

    public String inspectCompressedFile() throws IOException {
        StringBuilder logBuilder = new StringBuilder();
        DataInputStream inputStream = openCompressedFile();
        final QCMPFileHeader header = readQCMPFileHeader(inputStream);
        if (header == null) {
            logBuilder.append("Input file is not valid QCMPFile\n");
        } else {


            final boolean validHeader = header.validateHeader();
            logBuilder.append("Header is:\t\t").append(validHeader ? "valid" : "invalid").append('\n');

            logBuilder.append("Magic value:\t\t").append(header.getMagicValue()).append('\n');
            logBuilder.append("Quantization type\t");
            switch (header.getQuantizationType()) {
                case Scalar:
                    logBuilder.append("Scalar\n");
                    break;
                case Vector1D:
                    logBuilder.append("Vector1D\n");
                    break;
                case Vector2D:
                    logBuilder.append("Vector2D\n");
                    break;
                case Vector3D:
                    logBuilder.append("Vector3D\n");
                    break;
                case Invalid:
                    logBuilder.append("INVALID\n");
                    break;
            }
            logBuilder.append("Bits per pixel:\t\t").append(header.getBitsPerPixel()).append('\n');
            logBuilder.append("Codebook:\t\t").append(header.isCodebookPerPlane() ? "one per plane\n" : "one for all\n");

            logBuilder.append("Image size X:\t\t").append(header.getImageSizeX()).append('\n');
            logBuilder.append("Image size Y:\t\t").append(header.getImageSizeY()).append('\n');
            logBuilder.append("Image size Z:\t\t").append(header.getImageSizeZ()).append('\n');

            logBuilder.append("Vector size X:\t\t").append(header.getVectorSizeX()).append('\n');
            logBuilder.append("Vector size Y:\t\t").append(header.getVectorSizeY()).append('\n');
            logBuilder.append("Vector size Z:\t\t").append(header.getVectorSizeZ()).append('\n');
        }
        return logBuilder.toString();
    }
}





















