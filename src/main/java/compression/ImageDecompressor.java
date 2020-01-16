package compression;

import cli.ParsedCliOptions;
import compression.fileformat.QCMPFileHeader;

import java.io.*;


public class ImageDecompressor extends CompressorDecompressorBase {

    private FileInputStream fileInputStream = null;
    private DataInputStream dataInputStream = null;

    public ImageDecompressor(ParsedCliOptions options) {
        super(options);
    }

    public boolean decompress() {
        return true;
    }

    private void openCompressStreams() throws FileNotFoundException {
        fileInputStream = new FileInputStream(options.getInputFile());
        dataInputStream = new DataInputStream(fileInputStream);
    }

    private void closeInputStreams() throws IOException {
        fileInputStream.close();
        dataInputStream.close();
    }

    private long getExpectedDataSizeForScalarQuantization(final QCMPFileHeader header) {
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());

        long codebookDataSize = 2 * codebookSize;
        codebookDataSize *= (header.isCodebookPerPlane() ? header.getImageSizeZ() : 1);

        final long pixelCount = header.getImageSizeX() * header.getImageSizeY() * header.getImageSizeZ();
        final long pixelDataSize = (pixelCount * header.getBitsPerPixel()) / 8;

        return (codebookDataSize + pixelDataSize);
    }

    private long getExpectedDataSize(final QCMPFileHeader header) {
        switch (header.getQuantizationType()) {
            case Scalar: {
                return getExpectedDataSizeForScalarQuantization(header);
            }
            case Vector1D:
            case Vector2D:
                break;
            case Vector3D:
            case Invalid:
                return -1;
        }
        return -1;
    }

    private boolean isValidQCMPFile() throws IOException {
        openCompressStreams();
        final QCMPFileHeader header = readQCMPFileHeader(dataInputStream);
        closeInputStreams();

        if (header == null) {
            return false;
        } else {
            final long fileSize = new File(options.getInputFile()).length();
            final long dataSize = fileSize - QCMPFileHeader.QCMP_HEADER_SIZE;
            final long expectedDataSize = getExpectedDataSize(header);
            return (dataSize == expectedDataSize);
        }
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
        boolean validFile = true;
        openCompressStreams();
        final QCMPFileHeader header = readQCMPFileHeader(dataInputStream);
        closeInputStreams();

        if (header == null) {
            logBuilder.append("Input file is not valid QCMPFile\n");
            validFile = false;
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
            logBuilder.append("Codebook:\t\t").append(header.isCodebookPerPlane() ? "one per plane\n" : "one for " +
                    "all\n");

            logBuilder.append("Image size X:\t\t").append(header.getImageSizeX()).append('\n');
            logBuilder.append("Image size Y:\t\t").append(header.getImageSizeY()).append('\n');
            logBuilder.append("Image size Z:\t\t").append(header.getImageSizeZ()).append('\n');

            logBuilder.append("Vector size X:\t\t").append(header.getVectorSizeX()).append('\n');
            logBuilder.append("Vector size Y:\t\t").append(header.getVectorSizeY()).append('\n');
            logBuilder.append("Vector size Z:\t\t").append(header.getVectorSizeZ()).append('\n');

            final long fileSize = new File(options.getInputFile()).length();
            final long dataSize = fileSize - QCMPFileHeader.QCMP_HEADER_SIZE;
            final long expectedDataSize = getExpectedDataSize(header);
            validFile = (dataSize == expectedDataSize);

            logBuilder.append("Data size:\t\t").append(dataSize).append(" Bytes ").append(dataSize == expectedDataSize ? "(correct)\n" : "(INVALID)\n");
        }

        logBuilder.append("\n=== Input file is ").append(validFile ? "VALID" : "INVALID").append(" ===\n");
        return logBuilder.toString();
    }
}





















