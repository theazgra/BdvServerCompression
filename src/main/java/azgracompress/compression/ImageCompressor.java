package azgracompress.compression;

import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.fileformat.QCMPFileHeader;

import java.io.*;

public class ImageCompressor extends CompressorDecompressorBase {
    final int PLANE_DATA_SIZES_OFFSET = 23;

    public ImageCompressor(CompressionOptions options) {
        super(options);
    }

    /**
     * Create compressor based on set options.
     *
     * @return Correct implementation of image compressor or null if configuration is not valid.
     */
    private IImageCompressor getImageCompressor() {
        IImageCompressor compressor;
        switch (options.getQuantizationType()) {
            case Scalar:
                compressor = new SQImageCompressor(options);
                break;
            case Vector1D:
            case Vector2D:

                compressor = new VQImageCompressor(options);
                break;
            case Vector3D:
            case Invalid:
            default:
                return null;
        }

        // Forward listeners to image compressor.
        duplicateAllListeners(compressor);
        return compressor;
    }

    private void reportCompressionRatio(final QCMPFileHeader header, final int written) {
        final long originalDataSize = 2 * header.getImageSizeX() * header.getImageSizeY() * header.getImageSizeZ();
        final double compressionRatio = (double) written / (double) originalDataSize;
        System.out.println(String.format("Compression ratio: %.5f", compressionRatio));
    }

    public boolean trainAndSaveCodebook() {
        reportStatusToListeners("=== Training codebook ===");
        IImageCompressor imageCompressor = getImageCompressor();
        if (imageCompressor == null) {
            return false;
        }
        try {
            imageCompressor.trainAndSaveCodebook();
        } catch (ImageCompressionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean compress() {
        IImageCompressor imageCompressor = getImageCompressor();

        if (imageCompressor == null) {
            return false;
        }

        long[] planeDataSizes = null;

        try (FileOutputStream fos = new FileOutputStream(options.getOutputFilePath(), false);
             DataOutputStream compressStream = new DataOutputStream(new BufferedOutputStream(fos, 8192))) {

            final QCMPFileHeader header = createHeader();
            header.writeHeader(compressStream);

            planeDataSizes = imageCompressor.compress(compressStream);

            if (options.isVerbose()) {
                reportCompressionRatio(header, compressStream.size());
            }
        } catch (ImageCompressionException ex) {
            System.err.println(ex.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (planeDataSizes == null) {
            System.err.println("Plane data sizes are unknown!");
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(options.getOutputFilePath(), "rw")) {
            raf.seek(PLANE_DATA_SIZES_OFFSET);
            writePlaneDataSizes(raf, planeDataSizes);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Write plane data size to compressed file.
     *
     * @param outStream      Compressed file stream.
     * @param planeDataSizes Written compressed plane sizes.
     * @throws IOException when fails to write plane data size.
     */
    private void writePlaneDataSizes(RandomAccessFile outStream, final long[] planeDataSizes) throws IOException {
        for (final long planeDataSize : planeDataSizes) {
            outStream.writeInt((int) planeDataSize);
        }
    }

    /**
     * Get number of planes to be compressed.
     *
     * @return Number of planes for compression.
     */
    private int getNumberOfPlanes() {
        if (options.getInputDataInfo().isPlaneIndexSet()) {
            return 1;
        } else if (options.getInputDataInfo().isPlaneRangeSet()) {
            final Range<Integer> planeRange = options.getInputDataInfo().getPlaneRange();
            return ((planeRange.getInclusiveTo() + 1) - planeRange.getFrom());
        } else {
            return options.getInputDataInfo().getDimensions().getZ();
        }
    }


    /**
     * Create QCMPFile header for compressed file.
     *
     * @return Valid QCMPFile header for compressed file.
     */
    private QCMPFileHeader createHeader() {
        QCMPFileHeader header = new QCMPFileHeader();


        header.setQuantizationType(options.getQuantizationType());
        header.setBitsPerCodebookIndex((byte) options.getBitsPerCodebookIndex());

        header.setCodebookPerPlane(options.getCodebookType() == CompressionOptions.CodebookType.Individual);

        header.setImageSizeX(options.getInputDataInfo().getDimensions().getX());
        header.setImageSizeY(options.getInputDataInfo().getDimensions().getY());
        header.setImageSizeZ(getNumberOfPlanes());

        header.setVectorDimension(options.getVectorDimension());

        return header;
    }
}
