package azgracompress.compression;

import azgracompress.compression.exception.ImageDecompressionException;
import azgracompress.data.ImageU16Dataset;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.fileformat.QuantizationType;
import azgracompress.utilities.Stopwatch;
import org.jetbrains.annotations.Nullable;

import java.io.*;


@SuppressWarnings("DuplicatedCode")
public class ImageDecompressor extends CompressorDecompressorBase {

    public ImageDecompressor(CompressionOptions options) {
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
        IImageDecompressor decompressor;
        switch (header.getQuantizationType()) {
            case Scalar:
                decompressor = new SQImageDecompressor(options);
                break;
            case Vector1D:
            case Vector2D:
            case Vector3D:
                decompressor = new VQImageDecompressor(options);
                break;
            case Invalid:
            default:
                return null;
        }

        // Forward listeners to image decompressor.
        duplicateAllListeners(decompressor);

        if (options.isVerbose())
            decompressor.addStatusListener(this::defaultLog);

        return decompressor;
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

        QCMPFileHeader header;
        try (FileInputStream fileInputStream = new FileInputStream(options.getInputDataInfo().getFilePath());
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
            logBuilder.append("Bits per pixel:\t\t").append(header.getBitsPerCodebookIndex()).append('\n');

            logBuilder.append("Codebook:\t\t").append(header.isCodebookPerPlane() ? "one per plane\n" : "one for " +
                    "all\n");

            final int codebookSize = (int) Math.pow(2, header.getBitsPerCodebookIndex());
            logBuilder.append("Codebook size:\t\t").append(codebookSize).append('\n');

            logBuilder.append("Image size X:\t\t").append(header.getImageSizeX()).append('\n');
            logBuilder.append("Image size Y:\t\t").append(header.getImageSizeY()).append('\n');
            logBuilder.append("Image size Z:\t\t").append(header.getImageSizeZ()).append('\n');

            logBuilder.append("Vector size X:\t\t").append(header.getVectorSizeX()).append('\n');
            logBuilder.append("Vector size Y:\t\t").append(header.getVectorSizeY()).append('\n');
            logBuilder.append("Vector size Z:\t\t").append(header.getVectorSizeZ()).append('\n');

            final long headerSize = header.getHeaderSize();
            final long fileSize = new File(options.getInputDataInfo().getFilePath()).length();
            final long dataSize = fileSize - header.getHeaderSize();

            final IImageDecompressor decompressor = getImageDecompressor(header);

            if (decompressor != null) {
                final long expectedDataSize = decompressor.getExpectedDataSize(header);
                validFile = (dataSize == expectedDataSize);

                logBuilder.append("File size:\t\t").append(fileSize).append(" B");

                final long KB = (fileSize / 1000);
                if (KB > 0) {
                    logBuilder.append(" (").append(KB).append(" KB)");
                    final long MB = (KB / 1000);
                    if (MB > 0) {
                        logBuilder.append(" (").append(MB).append(" MB)");
                    }
                }
                logBuilder.append('\n');

                logBuilder.append("Header size:\t\t").append(headerSize).append(" Bytes\n");
                logBuilder.append("Data size:\t\t").append(dataSize).append(" Bytes ")
                        .append(dataSize == expectedDataSize ? "(correct)\n" : "(INVALID)\n");

                final long pixelCount = header.getImageDims().multiplyTogether();
                final long uncompressedSize = 2 * pixelCount; // We assert 16 bit (2 byte) pixel.
                final double compressionRatio = (double) fileSize / (double) uncompressedSize;
                logBuilder.append(String.format("Compression ratio:\t%.4f\n", compressionRatio));

                final double BPP = ((double) fileSize * 8.0) / (double) pixelCount;
                logBuilder.append(String.format("Bits Per Pixel (BPP):\t%.4f\n", BPP));
            }
        }

        logBuilder.append("\n=== Input file is ").append(validFile ? "VALID" : "INVALID").append(" ===\n");

        if (header != null && options.isVerbose()) {
            final String prefix = header.getQuantizationType() != QuantizationType.Vector3D ? "Plane" : "Voxel layer";
            final long[] planeDataSizes = header.getPlaneDataSizes();
            long planeIndex = 0;
            for (final long planeDataSize : planeDataSizes) {
                logBuilder.append(String.format("%s %d: %d Bytes\n", prefix, planeIndex++, planeDataSize));
            }
        }

        return logBuilder.toString();
    }

    public boolean decompressToFile() {
        final Stopwatch decompressionStopwatch = Stopwatch.startNew();
        final long decompressedFileSize;
        try (FileInputStream fileInputStream = new FileInputStream(options.getInputDataInfo().getFilePath());
             DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            final QCMPFileHeader header = decompressQcmpHeader(dataInputStream);
            if (header == null)
                return false;

            decompressedFileSize = 2 * header.getImageDims().multiplyTogether();

            IImageDecompressor imageDecompressor = getImageDecompressor(header);
            if (imageDecompressor == null) {
                System.err.println("Unable to create correct decompressor.");
                return false;
            }

            if (!checkInputFileSize(header, imageDecompressor)) {
                return false;
            }

            try (FileOutputStream fos = new FileOutputStream(options.getOutputFilePath(), false);
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
        decompressionStopwatch.stop();
        final double seconds = decompressionStopwatch.totalElapsedSeconds();
        final double MBSize = ((double) decompressedFileSize / 1000.0) / 1000.0;
        final double MBPerSec = MBSize / seconds;
        reportStatusToListeners("Decompression speed: %.4f MB/s", MBPerSec);

        return true;
    }

    private boolean checkInputFileSize(QCMPFileHeader header, IImageDecompressor imageDecompressor) {
        final long fileSize = new File(options.getInputDataInfo().getFilePath()).length();
        final long dataSize = fileSize - header.getHeaderSize();
        final long expectedDataSize = imageDecompressor.getExpectedDataSize(header);
        if (dataSize != expectedDataSize) {
            reportStatusToListeners("Invalid file size.");
            return false;
        }
        return true;
    }

    public ImageU16Dataset decompressInMemory() {
        try (FileInputStream fileInputStream = new FileInputStream(options.getInputDataInfo().getFilePath());
             DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            final QCMPFileHeader header = decompressQcmpHeader(dataInputStream);
            if (header == null)
                return null;

            IImageDecompressor imageDecompressor = getImageDecompressor(header);
            if (imageDecompressor == null) {
                System.err.println("Unable to create correct decompressor.");
                return null;
            }

            if (!checkInputFileSize(header, imageDecompressor)) {
                return null;
            }

            final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();
            final short[][] decompressedData = new short[header.getImageSizeZ()][planePixelCount];


            try {
                imageDecompressor.decompressToBuffer(dataInputStream, decompressedData, header);
            } catch (ImageDecompressionException ex) {
                System.err.println(ex.getMessage());
                return null;
            }

            return new ImageU16Dataset(header.getImageDims().toV2i(), header.getImageSizeZ(), decompressedData);

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            return null;
        }
    }

    @Nullable
    private QCMPFileHeader decompressQcmpHeader(DataInputStream dataInputStream) throws IOException {
        final QCMPFileHeader header = readQCMPFileHeader(dataInputStream);
        if (header == null) {
            System.err.println("Failed to read QCMPFile header");
            return null;
        }
        if (!header.validateHeader()) {
            System.err.println("QCMPFile header is invalid");
            return null;
        }
        return header;
    }
}