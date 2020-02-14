package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageDecompressionException;
import azgracompress.fileformat.QCMPFileHeader;

import java.io.*;


public class ImageDecompressor extends CompressorDecompressorBase {

    public ImageDecompressor(ParsedCliOptions options) {
        super(options);
    }


    /**
     * Read compressed QCMP file header.
     *
     * @param inputStream Compressed data stream.
     * @return Decompressed file header.
     * @throws IOException when failed to read header.
     */
    private QCMPFileHeader readQCMPFileHeader(DataInputStream inputStream) throws IOException {
        QCMPFileHeader header = new QCMPFileHeader();
        if (!header.readHeader(inputStream)) {
            // Invalid QCMPFile header.
            return null;
        }
        return header;
    }

    /**
     * Get image plane decompressor for set quantization type.
     *
     * @return Correct implementation of image decompressor.
     */
    private IImageDecompressor getImageDecompressor(final QCMPFileHeader header) {
        switch (header.getQuantizationType()) {
            case Scalar:
                return new SQImageDecompressor(options);
            case Vector1D:
            case Vector2D:
                return new VQImageDecompressor(options);
            case Vector3D:
            case Invalid:
            default:
                return null;
        }
    }

    /**
     * Inspect the compressed file by returning information contained in its header.
     *
     * @return Information from header.
     * @throws IOException When fails to read the header.
     */
    public String inspectCompressedFile() throws IOException {
        StringBuilder logBuilder = new StringBuilder();
        boolean validFile = true;

        QCMPFileHeader header = null;
        try (FileInputStream fileInputStream = new FileInputStream(options.getInputFileInfo().getFilePath());
             DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {
            header = readQCMPFileHeader(dataInputStream);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            return "";
        }

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

            final long fileSize = new File(options.getInputFileInfo().getFilePath()).length();
            final long dataSize = fileSize - QCMPFileHeader.QCMP_HEADER_SIZE;
            final IImageDecompressor decompressor = getImageDecompressor(header);
            if (decompressor != null) {
                final long expectedDataSize = decompressor.getExpectedDataSize(header);
                validFile = (dataSize == expectedDataSize);
                logBuilder.append(String.format("File size:\t\t%d B (%d kB) (%d MB)\n",
                                                fileSize,
                                                (fileSize / 1000),
                                                ((fileSize / 1000) / 1000)));
                logBuilder.append("Data size:\t\t").append(dataSize).append(" Bytes ").append(dataSize == expectedDataSize ? "(correct)\n" : "(INVALID)\n");
            }
        }

        logBuilder.append("\n=== Input file is ").append(validFile ? "VALID" : "INVALID").append(" ===\n");
        return logBuilder.toString();
    }

    public boolean decompress() {

        try (FileInputStream fileInputStream = new FileInputStream(options.getInputFileInfo().getFilePath());
             DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            final QCMPFileHeader header = readQCMPFileHeader(dataInputStream);

            if (header == null) {
                System.err.println("Failed to read QCMPFile header");
                return false;
            }
            if (!header.validateHeader()) {
                System.err.println("QCMPFile header is invalid");
                return false;
            }

            IImageDecompressor imageDecompressor = getImageDecompressor(header);
            if (imageDecompressor == null) {
                System.err.println("Unable to create correct decompressor.");
                return false;
            }

            final long fileSize = new File(options.getInputFileInfo().getFilePath()).length();
            final long dataSize = fileSize - QCMPFileHeader.QCMP_HEADER_SIZE;
            final long expectedDataSize = imageDecompressor.getExpectedDataSize(header);
            if (dataSize != expectedDataSize) {
                System.err.println("Invalid file size.");
                return false;
            }

            try (FileOutputStream fos = new FileOutputStream(options.getOutputFile(), false);
                 DataOutputStream decompressStream = new DataOutputStream(fos)) {

                imageDecompressor.decompress(dataInputStream, decompressStream, header);

            } catch (ImageDecompressionException ex) {
                System.err.println(ex.getMessage());
                return false;
            }

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            return false;
        }

        return true;
    }
}