package azgracompress.cache;

import azgracompress.compression.CompressionOptions;
import azgracompress.data.V3i;
import azgracompress.fileformat.QuantizationType;
import azgracompress.quantization.scalar.SQCodebook;
import azgracompress.quantization.vector.VQCodebook;

import java.io.*;


public class QuantizationCacheManager {

    /**
     * Folders where cache files are stored.
     */
    private final String cacheFolder;

    /**
     * Create cache manager with the cache folder.
     *
     * @param cacheFolder Cache folder
     */
    public QuantizationCacheManager(final String cacheFolder) {
        this.cacheFolder = cacheFolder;
        //noinspection ResultOfMethodCallIgnored
        new File(this.cacheFolder).mkdirs();
    }

    /**
     * Get cache file for scalar quantizer.
     *
     * @param trainFile    Input image file name.
     * @param codebookSize Codebook size.
     * @return Cache file for scalar quantizer.
     */
    private File getCacheFilePathForSQ(final String trainFile, final int codebookSize) {
        final File inputFile = new File(trainFile);
        return new File(cacheFolder, String.format("%s_%d_bits.qvc",
                                                   inputFile.getName(), codebookSize));
    }

    /**
     * Get cache file for vector quantizer.
     *
     * @param trainFile    Input image file name.
     * @param codebookSize Size of the codebook.
     * @param vDim         Vector dimensions.
     * @return Cache file for vector quantizer.
     */
    private File getCacheFilePathForVQ(final String trainFile,
                                       final int codebookSize,
                                       final V3i vDim) {
        final File inputFile = new File(trainFile);
        final String cacheFileName = String.format("%s_%d_%dx%dx%d.qvc", inputFile.getName(), codebookSize,
                                                   vDim.getX(), vDim.getY(), vDim.getZ());
//         System.out.println("getCacheFilePathForVQ()=" + cacheFileName);
        return new File(cacheFolder, cacheFileName);
    }


    /**
     * Create CacheFileHeader for ScalarQuantization cache.
     *
     * @param trainFile Image file used for training.
     * @param codebook  Final SQ codebook.
     * @return SQ cache file header.
     */
    private CacheFileHeader createHeaderForSQ(final String trainFile, final SQCodebook codebook) {
        final CacheFileHeader header = new CacheFileHeader();
        header.setQuantizationType(QuantizationType.Scalar);
        header.setCodebookSize(codebook.getCodebookSize());
        header.setTrainFileName(trainFile);
        header.setVectorDims(new V3i(0));
        return header;
    }

    /**
     * Find the correct quantization type based on vector dimension.
     *
     * @param vectorDims Quantization vector dimensions.
     * @return Correct QuantizationType.
     */
    private QuantizationType getQuantizationTypeFromVectorDimensions(final V3i vectorDims) {
        if (vectorDims.getX() > 1) {
            if (vectorDims.getY() == 1 && vectorDims.getZ() == 1) {
                return QuantizationType.Vector1D;
            } else if (vectorDims.getY() > 1 && vectorDims.getZ() == 1) {
                return QuantizationType.Vector2D;
            } else {
                return QuantizationType.Vector3D;
            }
        } else if (vectorDims.getX() == 1 && vectorDims.getY() > 1 && vectorDims.getZ() == 1) {
            return QuantizationType.Vector1D;
        }
        return QuantizationType.Invalid;
    }

    /**
     * Create CacheFileHeader for VQ cache.
     *
     * @param trainFile Image file used for training.
     * @param codebook  Final VQ codebook.
     * @return VQ cache file header.
     */
    private CacheFileHeader createHeaderForVQ(final String trainFile, final VQCodebook codebook) {
        final CacheFileHeader header = new CacheFileHeader();
        header.setQuantizationType(getQuantizationTypeFromVectorDimensions(codebook.getVectorDims()));
        header.setCodebookSize(codebook.getCodebookSize());
        header.setTrainFileName(trainFile);
        header.setVectorDims(codebook.getVectorDims());
        return header;
    }

    /**
     * Save SQ codebook to cache.
     *
     * @param trainFile Image file used for training.
     * @param codebook  SQ codebook.
     * @return Path to the saved cache file.
     * @throws IOException when fails to save the cache file.
     */
    public String saveCodebook(final String trainFile, final SQCodebook codebook) throws IOException {
        final String fileName = getCacheFilePathForSQ(trainFile, codebook.getCodebookSize()).getAbsolutePath();

        final CacheFileHeader header = createHeaderForSQ(new File(trainFile).getName(), codebook);
        final SQCacheFile cacheFile = new SQCacheFile(header, codebook);

        try (final FileOutputStream fos = new FileOutputStream(fileName, false);
             final DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (final IOException ex) {
            throw new IOException("Failed to save SQ cache file\n" + ex.getMessage());
        }
        return fileName;
    }


    /**
     * Save VQ codebook to cache.
     *
     * @param trainFile Image file used for training.
     * @param codebook  VQ codebook.
     * @return Path to the saved cache file.
     * @throws IOException when fails to save the cache file.
     */
    public String saveCodebook(final String trainFile, final VQCodebook codebook) throws IOException {
        final String fileName = getCacheFilePathForVQ(trainFile,
                                                      codebook.getCodebookSize(),
                                                      codebook.getVectorDims()).getAbsolutePath();

        final CacheFileHeader header = createHeaderForVQ(new File(trainFile).getName(), codebook);
        final VQCacheFile cacheFile = new VQCacheFile(header, codebook);

        try (final FileOutputStream fos = new FileOutputStream(fileName, false);
             final DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (final IOException ex) {
            throw new IOException("Failed to save VQ cache file\n" + ex.getMessage());
        }
        return fileName;
    }

    /**
     * Read data from file to cache file.
     *
     * @param file      Cache file.
     * @param cacheFile Actual cache file object.
     * @return Cache file with data from disk.
     * @throws IOException when fails to read the cache file from disk.
     */
    private ICacheFile readCacheFile(final File file, final ICacheFile cacheFile) throws IOException {
        try (final FileInputStream fis = new FileInputStream(file);
             final DataInputStream dis = new DataInputStream(fis)) {

            cacheFile.readFromStream(dis);
            return cacheFile;
        }
    }

    /**
     * Check if the SQ cache file for given image file exists.
     *
     * @param imageFile    Image file.
     * @param codebookSize Scalar quantization codebook size.
     * @return True if cache file exists and and can be loaded.
     */
    public boolean doesSQCacheExists(final String imageFile, final int codebookSize) {
        return getCacheFilePathForSQ(imageFile, codebookSize).exists();
    }

    /**
     * Check if the VQ cache file for given image file exists.
     *
     * @param imageFile    Image file.
     * @param codebookSize Scalar quantization codebook size.
     * @param vDim         Quantization vector dimensions.
     * @return True if cache file exists and and can be loaded.
     */
    public boolean doesVQCacheExists(final String imageFile, final int codebookSize, final V3i vDim) {
        return getCacheFilePathForVQ(imageFile, codebookSize, vDim).exists();
    }

    /**
     * Load SQ cache file from disk.
     *
     * @param imageFile    Input image file.
     * @param codebookSize Codebook size.
     * @return SQ cache file.
     */
    public SQCacheFile loadSQCacheFile(final String imageFile, final int codebookSize) {
        final File path = getCacheFilePathForSQ(imageFile, codebookSize);
        try {
            return (SQCacheFile) readCacheFile(path, new SQCacheFile());
        } catch (final IOException e) {
            System.err.println("Failed to read SQ cache file." + path);
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Read VQ cache file disk.
     *
     * @param trainFile    Input image file.
     * @param codebookSize Codebook size.
     * @param vDim         Quantization vector dimension.
     * @return VQ cache file.
     */
    public VQCacheFile loadVQCacheFile(final String trainFile,
                                       final int codebookSize,
                                       final V3i vDim) {
        final File path = getCacheFilePathForVQ(trainFile, codebookSize, vDim);
        try {
            return (VQCacheFile) readCacheFile(path, new VQCacheFile());
        } catch (final IOException e) {
            System.err.println("Failed to read VQ cache file." + path);
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Read SQ codebook from cache file.
     *
     * @param trainFile    Input image file.
     * @param codebookSize Codebook size.
     * @return SQ codebook or null.
     */
    public SQCodebook loadSQCodebook(final String trainFile, final int codebookSize) {
        final SQCacheFile cacheFile = loadSQCacheFile(trainFile, codebookSize);
        if (cacheFile != null)
            return cacheFile.getCodebook();
        else
            return null;
    }

    /**
     * Read VQ codebook from cache file.
     *
     * @param trainFile    Input image file.
     * @param codebookSize Codebook size.
     * @param vDim         Quantization vector dimension.
     * @return VQ codebook.
     */
    public VQCodebook loadVQCodebook(final String trainFile,
                                     final int codebookSize,
                                     final V3i vDim) {
        final VQCacheFile cacheFile = loadVQCacheFile(trainFile, codebookSize, vDim);
        if (cacheFile != null)
            return cacheFile.getCodebook();
        else
            return null;

    }

    private static ICacheFile getCacheFile(final QuantizationType qt) {
        if (qt.isOneOf(QuantizationType.Vector1D, QuantizationType.Vector2D, QuantizationType.Vector3D))
            return new VQCacheFile();
        else if (qt == QuantizationType.Scalar)
            return new SQCacheFile();

        assert (false) : "Invalid quantization type.";
        return null;
    }

    public ICacheFile loadCacheFile(final CompressionOptions compressionParams) {
        final String path;
        final int codebookSize = (int) Math.pow(2, compressionParams.getBitsPerCodebookIndex());
        switch (compressionParams.getQuantizationType()) {

            case Scalar:
                path = getCacheFilePathForSQ(compressionParams.getInputDataInfo().getCacheFileName(),
                                             codebookSize).getAbsolutePath();
                break;
            case Vector1D:
            case Vector2D:
            case Vector3D:
                path = getCacheFilePathForVQ(compressionParams.getInputDataInfo().getCacheFileName(),
                                             codebookSize,
                                             compressionParams.getQuantizationVector()).getAbsolutePath();
                break;
            default:
                return null;
        }
        return readCacheFile(path);
    }

    /**
     * Read cache file by DataInputStream.
     *
     * @param inputStream Input stream.
     * @return Cache file or null, if exception occurs.
     */
    private static ICacheFile readCacheFileImpl(final InputStream inputStream) {
        try (final DataInputStream dis = new DataInputStream(inputStream)) {
            final CacheFileHeader header = new CacheFileHeader();
            header.readFromStream(dis);

            final ICacheFile cacheFile = getCacheFile(header.getQuantizationType());
            assert (cacheFile != null);
            cacheFile.readFromStream(dis, header);
            return cacheFile;
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Read cache file from input stream.
     *
     * @param inputStream Input data stream.
     * @return Cache file or null if reading fails.
     */
    public static ICacheFile readCacheFile(final InputStream inputStream) {
        return readCacheFileImpl(inputStream);
    }


    /**
     * Read cache file from file.
     *
     * @param path File path.
     * @return Cache file or null if reading fails.
     */
    public static ICacheFile readCacheFile(final String path) {
        try (final FileInputStream fis = new FileInputStream(path)) {
            return readCacheFileImpl(fis);
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Inspect cache file specified by the path.
     *
     * @param path Path to cache file.
     */
    public static void inspectCacheFile(final String path, final boolean verbose) {
        CacheFileHeader header = null;
        final long fileSize;
        try (final FileInputStream fis = new FileInputStream(path);
             final DataInputStream dis = new DataInputStream(fis)) {
            fileSize = fis.getChannel().size();
            header = new CacheFileHeader();
            header.readFromStream(dis);
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }
        final StringBuilder reportBuilder = new StringBuilder();
        final long expectedFileSize = header.getExpectedFileSize();
        if (expectedFileSize == fileSize) {
            reportBuilder.append("\u001B[32mCache file is VALID ").append(fileSize).append(" bytes\u001B[0m\n");
        } else {
            reportBuilder.append("\u001B[31mCache file is INVALID.\u001B[0m\n\t")
                    .append(fileSize).append(" bytes instead of expected ")
                    .append(expectedFileSize).append(" bytes.\n");
        }
        header.report(reportBuilder);

        if (verbose) {

            final ICacheFile cacheFile = getCacheFile(header.getQuantizationType());
            assert (cacheFile != null);

            try (final FileInputStream fis = new FileInputStream(path);
                 final DataInputStream dis = new DataInputStream(fis)) {
                cacheFile.readFromStream(dis);
            } catch (final Exception e) {
                reportBuilder.append(e.getMessage());
            }

            cacheFile.report(reportBuilder);
        }

        System.out.println(reportBuilder);
    }
}
