package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.compression.CompressionOptions;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.fileformat.QuantizationType;
import cz.it4i.qcmp.fileformat.QvcHeaderV2;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;
import cz.it4i.qcmp.quantization.vector.VQCodebook;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


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
        return new File(cacheFolder, String.format("%s_%d_bits.qvc", inputFile.getName(), codebookSize));
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
        final String cacheFileName = String.format("%s_%d_%dx%dx%d.qvc", inputFile.getName(),
                                                   codebookSize, vDim.getX(), vDim.getY(), vDim.getZ());
        return new File(cacheFolder, cacheFileName);
    }


    /**
     * Create CacheFileHeader for ScalarQuantization cache.
     *
     * @param trainFile Image file used for training.
     * @param codebook  Final SQ codebook.
     * @return SQ cache file header.
     */
    private QvcHeaderV2 createHeaderForSQ(final String trainFile, final SQCodebook codebook) {
        final QvcHeaderV2 header = new QvcHeaderV2();
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
    private QvcHeaderV2 createHeaderForVQ(final String trainFile, final VQCodebook codebook) {
        final QvcHeaderV2 header = new QvcHeaderV2();
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

        final QvcHeaderV2 header = createHeaderForSQ(new File(trainFile).getName(), codebook);
        final SqQvcFile cacheFile = new SqQvcFile(header, codebook);

        try (final FileOutputStream fos = new FileOutputStream(fileName, false);
             final DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (final IOException ex) {
            throw new IOException("Failed to save SQ QVC file\n" + ex.getMessage());
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

        final QvcHeaderV2 header = createHeaderForVQ(new File(trainFile).getName(), codebook);
        final VqQvcFile cacheFile = new VqQvcFile(header, codebook);

        try (final FileOutputStream fos = new FileOutputStream(fileName, false);
             final DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (final IOException ex) {
            throw new IOException("Failed to save VQ cache file\n" + ex.getMessage());
        }
        return fileName;
    }

    /**
     * Check if the SQ cache file for given image file exists.
     *
     * @param imageFile    Image file.
     * @param codebookSize Scalar quantization codebook size.
     * @return True if cache file exists and and can be loaded.
     */
    public boolean doesSqQvcFileExists(final String imageFile, final int codebookSize) {
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
    public boolean doesVqQvcFileExists(final String imageFile, final int codebookSize, final V3i vDim) {
        return getCacheFilePathForVQ(imageFile, codebookSize, vDim).exists();
    }

    /**
     * Load SQ cache file from disk.
     *
     * @param imageFile    Input image file.
     * @param codebookSize Codebook size.
     * @return SQ cache file.
     */
    public SqQvcFile loadSQCacheFile(final String imageFile, final int codebookSize) {
        final File fileInfo = getCacheFilePathForSQ(imageFile, codebookSize);
        return (SqQvcFile) QvcFileReader.readCacheFile(fileInfo.getAbsolutePath());
    }

    /**
     * Read VQ cache file disk.
     *
     * @param trainFile    Input image file.
     * @param codebookSize Codebook size.
     * @param vDim         Quantization vector dimension.
     * @return VQ cache file.
     */
    public VqQvcFile loadVQCacheFile(final String trainFile,
                                     final int codebookSize,
                                     final V3i vDim) {
        final File fileInfo = getCacheFilePathForVQ(trainFile, codebookSize, vDim);
        return (VqQvcFile) QvcFileReader.readCacheFile(fileInfo.getAbsolutePath());
    }

    /**
     * Read SQ codebook from cache file.
     *
     * @param trainFile    Input image file.
     * @param codebookSize Codebook size.
     * @return SQ codebook or null.
     */
    public SQCodebook loadSQCodebook(final String trainFile, final int codebookSize) {
        final SqQvcFile cacheFile = loadSQCacheFile(trainFile, codebookSize);
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
        final VqQvcFile cacheFile = loadVQCacheFile(trainFile, codebookSize, vDim);
        if (cacheFile != null)
            return cacheFile.getCodebook();
        else
            return null;

    }

    /**
     * Tries to load all (different codebook sizes) available cache files for given file and quantization type.
     *
     * @param compressionParams Parameters used to find cache file.
     * @return Cache files which are available for given file and quantization type.
     */
    public ArrayList<IQvcFile> loadAvailableCacheFiles(final CompressionOptions compressionParams) {
        final ArrayList<IQvcFile> availableCacheFiles = new ArrayList<>();

        final int originalBPCI = compressionParams.getBitsPerCodebookIndex();
        try {

            for (int bpci = 2; bpci < 9; bpci++) { // 2 to 8
                compressionParams.setBitsPerCodebookIndex(bpci);
                final IQvcFile bpciCacheFile = loadCacheFile(compressionParams);
                if (bpciCacheFile != null) {
                    availableCacheFiles.add(bpciCacheFile);
                }
            }
        } finally {
            compressionParams.setBitsPerCodebookIndex(originalBPCI);
        }
        return availableCacheFiles;
    }

    /**
     * Tries to load cache file for specified file and quantization type. Also Bits Per Codebook index and quantization vector dimensions
     * are used when searching for given cache file.
     *
     * @param compressionParams Parameters used to find cache file.
     * @return Quantization cache file or null if requested file doesn't exist.
     */
    public IQvcFile loadCacheFile(final CompressionOptions compressionParams) {
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

        return QvcFileReader.readCacheFile(path);
    }


    /**
     * Inspect cache file specified by the path.
     *
     * @param path Path to cache file.
     */
    public static void inspectCacheFile(final String path, final boolean verbose) {
        final IQvcFile qvcFile = QvcFileReader.readCacheFile(path);
        if (qvcFile == null) {
            System.err.println("Provided path is not of valid QVC file.");
            return;
        }
        if (!qvcFile.getHeader().validateHeader()) {
            System.err.println("Provided file is corrupted.");
            return;
        }
        final StringBuilder reportBuilder = new StringBuilder();
        qvcFile.getHeader().report(reportBuilder, path);

        if (verbose) {
            qvcFile.report(reportBuilder);
        }

        System.out.println(reportBuilder);
    }
}
