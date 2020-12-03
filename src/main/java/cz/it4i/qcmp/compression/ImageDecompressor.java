package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.compression.exception.ImageDecompressionException;
import cz.it4i.qcmp.data.ImageU16Dataset;
import cz.it4i.qcmp.fileformat.IQvcFile;
import cz.it4i.qcmp.fileformat.QCMPFileHeader;
import cz.it4i.qcmp.fileformat.QuantizationType;
import cz.it4i.qcmp.utilities.Stopwatch;
import cz.it4i.qcmp.utilities.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("DuplicatedCode")
public class ImageDecompressor extends CompressorDecompressorBase {

    private IImageDecompressor cachedDecompressor = null;
    private QCMPFileHeader cachedHeader = null;

    public ImageDecompressor(final CompressionOptions passedOptions) {
        super(passedOptions);
    }


    public ImageDecompressor(final IQvcFile codebookCacheFile) {
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
        header.readFromStream(inputStream);
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

        if (!header.validateHeader()) {
            logBuilder.append("Input file is not valid QCMP file\n");
            validFile = false;
        } else {
            header.report(logBuilder, options.getInputDataInfo().getFilePath());
        }


        if (validFile && options.isVerbose()) {
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
        final double seconds = decompressionStopwatch.getElapsedInUnit(TimeUnit.SECONDS);
        final double MBSize = ((double) decompressedFileSize / 1000.0) / 1000.0;
        final double MBPerSec = MBSize / seconds;
        reportStatusToListeners("Decompression speed: %.4f MB/s", MBPerSec);

        return true;
    }

    private boolean checkInputFileSize(final QCMPFileHeader header, final IImageDecompressor imageDecompressor) {
        final long fileSize = new File(options.getInputDataInfo().getFilePath()).length();
        final long dataSize = fileSize - header.getHeaderSize();
        final long expectedDataSize = header.getExpectedDataSize();
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