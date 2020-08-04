package azgracompress.cache;

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
        return new File(cacheFolder, String.format("%s_%d_%dx%dx%d.qvc", inputFile.getName(), codebookSize,
                                                   vDim.getX(), vDim.getY(), vDim.getZ()));
    }


    /**
     * Create CacheFileHeader for ScalarQuantization cache.
     *
     * @param trainFile Image file used for training.
     * @param codebook  Final SQ codebook.
     * @return SQ cache file header.
     */
    private CacheFileHeader createHeaderForSQ(final String trainFile, final SQCodebook codebook) {
        CacheFileHeader header = new CacheFileHeader();
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
        CacheFileHeader header = new CacheFileHeader();
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
     * @throws IOException when fails to save the cache file.
     */
    public void saveCodebook(final String trainFile, final SQCodebook codebook) throws IOException {
        final String fileName = getCacheFilePathForSQ(trainFile, codebook.getCodebookSize()).getAbsolutePath();

        final CacheFileHeader header = createHeaderForSQ(new File(trainFile).getName(), codebook);
        final SQCacheFile cacheFile = new SQCacheFile(header, codebook);

        try (FileOutputStream fos = new FileOutputStream(fileName, false);
             DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (IOException ex) {
            throw new IOException("Failed to save SQ cache file\n" + ex.getMessage());
        }
    }

    /**
     * Save VQ codebook to cache.
     *
     * @param trainFile Image file used for training.
     * @param codebook  VQ codebook.
     * @throws IOException when fails to save the cache file.
     */
    public void saveCodebook(final String trainFile, final VQCodebook codebook) throws IOException {
        final String fileName = getCacheFilePathForVQ(trainFile,
                                                      codebook.getCodebookSize(),
                                                      codebook.getVectorDims()).getAbsolutePath();

        final CacheFileHeader header = createHeaderForVQ(new File(trainFile).getName(), codebook);
        final VQCacheFile cacheFile = new VQCacheFile(header, codebook);

        try (FileOutputStream fos = new FileOutputStream(fileName, false);
             DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (IOException ex) {
            throw new IOException("Failed to save VQ cache file\n" + ex.getMessage());
        }
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
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {

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
    private SQCacheFile loadSQCacheFile(final String imageFile, final int codebookSize) {
        final File path = getCacheFilePathForSQ(imageFile, codebookSize);
        try {
            return (SQCacheFile) readCacheFile(path, new SQCacheFile());
        } catch (IOException e) {
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
    private VQCacheFile loadVQCacheFile(final String trainFile,
                                        final int codebookSize,
                                        final V3i vDim) {
        final File path = getCacheFilePathForVQ(trainFile, codebookSize, vDim);
        try {
            return (VQCacheFile) readCacheFile(path, new VQCacheFile());
        } catch (IOException e) {
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

    /**
     * Inspect cache file specified by the path.
     *
     * @param path Path to cache file.
     */
    public static void inspectCacheFile(final String path) {
        CacheFileHeader header = null;
        long fileSize;
        try (FileInputStream fis = new FileInputStream(path);
             DataInputStream dis = new DataInputStream(fis)) {
            fileSize = fis.getChannel().size();
            header = new CacheFileHeader();
            header.readFromStream(dis);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        StringBuilder reportBuilder = new StringBuilder();
        final long expectedFileSize = header.getExpectedFileSize();
        if (expectedFileSize == fileSize) {
            reportBuilder.append("\u001B[32mCache file is VALID. ").append(fileSize).append(" bytes\u001B[0m\n ");
        } else {
            reportBuilder.append("\u001B[31mCache file is INVALID.\u001B[0m\n\t")
                    .append(fileSize).append(" bytes instead of expected ")
                    .append(expectedFileSize).append(" bytes.\n");
        }

        header.report(reportBuilder);

        System.out.println(reportBuilder);
    }
}
