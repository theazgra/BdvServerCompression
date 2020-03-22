package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageCompressionException;
import azgracompress.fileformat.QCMPFileHeader;

import java.io.*;

public class ImageCompressor extends CompressorDecompressorBase {

    final int PLANE_DATA_SIZES_OFFSET = 23;
    private final int codebookSize;

    public ImageCompressor(ParsedCliOptions options) {
        super(options);
        codebookSize = (int) Math.pow(2, options.getBitsPerPixel());
    }

    /**
     * Create compressor based on set options.
     *
     * @return Correct implementation of image compressor or null if configuration is not valid.
     */
    private IImageCompressor getImageCompressor() {
        switch (options.getQuantizationType()) {
            case Scalar: {
                return new SQImageCompressor(options);
            }
            case Vector1D:
            case Vector2D: {
                return new VQImageCompressor(options);
            }
            case Vector3D:
            case Invalid:
            default:
                return null;
        }
    }

    private void reportCompressionRatio(final QCMPFileHeader header, final int written) {
        final long originalDataSize = 2 * header.getImageSizeX() * header.getImageSizeY() * header.getImageSizeZ();
        final double compressionRatio = (double) written / (double) originalDataSize;
        System.out.println(String.format("Compression ratio: %.5f", compressionRatio));
    }

    public boolean trainAndSaveCodebook() {
        Log("=== Training codebook ===");
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

        try (FileOutputStream fos = new FileOutputStream(options.getOutputFile(), false);
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

        try (RandomAccessFile raf = new RandomAccessFile(options.getOutputFile(), "rw")) {
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
     * Create QCMPFile header for compressed file.
     *
     * @return Valid QCMPFile header for compressed file.
     */
    private QCMPFileHeader createHeader() {
        QCMPFileHeader header = new QCMPFileHeader();


        header.setQuantizationType(options.getQuantizationType());
        header.setBitsPerPixel((byte) options.getBitsPerPixel());

        // Codebook per plane is used only if middle plane isn't set nor is the cache folder.
        final boolean oneCodebook = options.shouldUseMiddlePlane() || options.hasCodebookCacheFolder();
        header.setCodebookPerPlane(!oneCodebook);

        header.setImageSizeX(options.getImageDimension().getX());
        header.setImageSizeY(options.getImageDimension().getY());
        header.setImageSizeZ(options.getNumberOfPlanes());

        header.setVectorDimension(options.getVectorDimension());

        return header;
    }
}
