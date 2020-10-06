package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.cache.ICacheFile;
import cz.it4i.qcmp.compression.exception.ImageDecompressionException;
import cz.it4i.qcmp.data.ImageU16Dataset;
import cz.it4i.qcmp.fileformat.QCMPFileHeader;
import cz.it4i.qcmp.fileformat.QuantizationType;
import cz.it4i.qcmp.utilities.Stopwatch;
import cz.it4i.qcmp.utilities.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Optional;


@SuppressWarnings("DuplicatedCode")
public class ImageDecompressor extends CompressorDecompressorBase {

    private IImageDecompressor cachedDecompressor = null;
    private QCMPFileHeader cachedHeader = null;

    public ImageDecompressor(final CompressionOptions passedOptions) {
        super(passedOptions);
    }


    public ImageDecompressor(final ICacheFile codebookCacheFile) {
        this(new CompressionOptions(codebookCacheFile));
        cachedDecompressor = getImageDecompressor(options.getQuantizationType());
        assert (cachedDecompressor != null);
        cachedDecompressor.preloadGlobalCodebook(codebookCacheFile);

        cachedHeader = new QCMPFileHeader();
        cachedHeader.setQuantizationType(codebookCacheFile.getHeader().getQuantizationType());
        cachedHeader.setBitsPerCodebookIndex((byte) ((int) Utils.log2(codebookCacheFile.getHeader().getCodebookSize())));
        cachedHeader.setVectorDimension(codebookCacheFile.getHeader().getVectorDim());
    }

    /**
     * Read compressed QCMP file header.
     *
     * @param inputStream Compressed data stream.
     * @return Decompressed file header.
     * @throws IOException when failed to read header.
     */
    private QCMPFileHeader readQCMPFileHeader(final DataInputStream inputStream) throws IOException {
        final QCMPFileHeader header = new QCMPFileHeader();
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
    private IImageDecompressor getImageDecompressor(final QuantizationType quantizationType) {
        final IImageDecompressor decompressor;
        switch (quantizationType) {
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
        final StringBuilder logBuilder = new StringBuilder();
        boolean validFile = true;

        final QCMPFileHeader header;
        try (final FileInputStream fileInputStream = new FileInputStream(options.getInputDataInfo().getFilePath());
             final DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {
            header = readQCMPFileHeader(dataInputStream);
        } catch (final IOException ioEx) {
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

            final IImageDecompressor decompressor = getImageDecompressor(header.getQuantizationType());

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
        try (final FileInputStream fileInputStream = new FileInputStream(options.getInputDataInfo().getFilePath());
             final DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            final QCMPFileHeader header = decompressQcmpHeader(dataInputStream);
            if (header == null)
                return false;

            decompressedFileSize = 2 * header.getImageDims().multiplyTogether();

            final IImageDecompressor imageDecompressor = getImageDecompressor(header.getQuantizationType());
            if (imageDecompressor == null) {
                System.err.println("Unable to create correct decompressor.");
                return false;
            }

            if (!checkInputFileSize(header, imageDecompressor)) {
                return false;
            }

            try (final FileOutputStream fos = new FileOutputStream(options.getOutputFilePath(), false);
                 final DataOutputStream decompressStream = new DataOutputStream(fos)) {

                imageDecompressor.decompress(dataInputStream, decompressStream, header);

            } catch (final ImageDecompressionException ex) {
                System.err.println(ex.getMessage());
                return false;
            }

        } catch (final IOException ioEx) {
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

    private boolean checkInputFileSize(final QCMPFileHeader header, final IImageDecompressor imageDecompressor) {
        final long fileSize = new File(options.getInputDataInfo().getFilePath()).length();
        final long dataSize = fileSize - header.getHeaderSize();
        final long expectedDataSize = imageDecompressor.getExpectedDataSize(header);
        if (dataSize != expectedDataSize) {
            reportStatusToListeners("Invalid file size.");
            return false;
        }
        return true;
    }

    public Optional<ImageU16Dataset> decompressInMemory() {
        try (final FileInputStream fileInputStream = new FileInputStream(options.getInputDataInfo().getFilePath());
             final DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            final QCMPFileHeader header = decompressQcmpHeader(dataInputStream);
            if (header == null)
                return Optional.empty();

            final IImageDecompressor imageDecompressor = getImageDecompressor(header.getQuantizationType());
            if (imageDecompressor == null) {
                System.err.println("Unable to create correct decompressor.");
                return Optional.empty();
            }

            if (!checkInputFileSize(header, imageDecompressor)) {
                return Optional.empty();
            }

            final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();
            final short[][] decompressedData = new short[header.getImageSizeZ()][planePixelCount];


            try {
                imageDecompressor.decompressToBuffer(dataInputStream, decompressedData, header);
            } catch (final ImageDecompressionException ex) {
                System.err.println(ex.getMessage());
                return Optional.empty();
            }

            return Optional.of(new ImageU16Dataset(header.getImageDims().toV2i(), header.getImageSizeZ(), decompressedData));

        } catch (final IOException ioEx) {
            ioEx.printStackTrace();
            return Optional.empty();
        }
    }


    public short[] decompressStream(final InputStream compressedStream, final int contentLength) throws ImageDecompressionException {
        try (final DataInputStream dis = new DataInputStream(new BufferedInputStream(compressedStream))) {
            assert (dis.markSupported());

            final QCMPFileHeader header = cachedHeader.copyOf();

            header.setImageSizeX(dis.readUnsignedShort());
            header.setImageSizeY(dis.readUnsignedShort());
            header.setImageSizeZ(dis.readUnsignedShort());


            final int chunkCount = dis.readUnsignedShort();
            final long[] chunkSizes = new long[chunkCount];

            dis.mark(contentLength);

            {
                int toSkip = contentLength - ((4 * 2) + (chunkCount * 2));
                while (toSkip > 0) {
                    final int skipped = dis.skipBytes(toSkip);
                    assert (skipped > 0);
                    toSkip -= skipped;
                }
                assert (toSkip == 0);
                for (int i = 0; i < chunkCount; i++) {
                    chunkSizes[i] = dis.readUnsignedShort();
                }
            }

            dis.reset();

            header.setPlaneDataSizes(chunkSizes);


            return cachedDecompressor.decompressStreamMode(dis, header);
        } catch (final IOException e) {
            throw new ImageDecompressionException("Unable to decompress chunk of image from stream.", e);
        }
    }

    @Nullable
    private QCMPFileHeader decompressQcmpHeader(final DataInputStream dataInputStream) throws IOException {
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