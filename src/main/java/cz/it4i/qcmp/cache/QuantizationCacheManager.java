package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.compression.CompressionOptions;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.fileformat.QuantizationType;
import cz.it4i.qcmp.fileformat.QvcHeaderV1;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;
import cz.it4i.qcmp.quantization.vector.VQCodebook;

import java.io.*;
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
    private QvcHeaderV1 createHeaderForSQ(final String trainFile, final SQCodebook codebook) {
        final QvcHeaderV1 header = new QvcHeaderV1();
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
    private QvcHeaderV1 createHeaderForVQ(final String trainFile, final VQCodebook codebook) {
        final QvcHeaderV1 header = new QvcHeaderV1();
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

        final QvcHeaderV1 header = createHeaderForSQ(new File(trainFile).getName(), codebook);
        final SqQvcFile cacheFile = new SqQvcFile(header, codebook);

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

        final QvcHeaderV1 header = createHeaderForVQ(new File(trainFile).getName(), codebook);
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
     * Read data from file to cache file.
     *
     * @param file      Cache file.
     * @param cacheFile Actual cache file object.
     * @return Cache file with data from disk.
     * @throws IOException when fails to read the cache file from disk.
     */
    private IQvcFile readCacheFile(final File file, final IQvcFile cacheFile) throws IOException {
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
    public SqQvcFile loadSQCacheFile(final String imageFile, final int codebookSize) {
        final File path = getCacheFilePathForSQ(imageFile, codebookSize);
        try {
            return (SqQvcFile) readCacheFile(path, new SqQvcFile());
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
    public VqQvcFile loadVQCacheFile(final String trainFile,
                                     final int codebookSize,
                                     final V3i vDim) {
        final File path = getCacheFilePathForVQ(trainFile, codebookSize, vDim);
        try {
            return (VqQvcFile) readCacheFile(path, new VqQvcFile());
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

    private static IQvcFile getCacheFile(final QuantizationType qt) {
        if (qt.isOneOf(QuantizationType.Vector1D, QuantizationType.Vector2D, QuantizationType.Vector3D))
            return new VqQvcFile();
        else if (qt == QuantizationType.Scalar)
            return new SqQvcFile();

        assert (false) : "Invalid quantization type.";
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
        return readCacheFile(path);
    }

    /**
     * Read cache file by DataInputStream.
     *
     * @param inputStream Input stream.
     * @return Cache file or null, if exception occurs.
     */
    private static IQvcFile readCacheFileImpl(final InputStream inputStream) {
        final DataInputStream dis;
        if (inputStream instanceof DataInputStream) {
            dis = (DataInputStream) inputStream;
        } else {
            dis = new DataInputStream(inputStream);
        }

        final QvcHeaderV1 header = new QvcHeaderV1();
        try {
            header.readFromStream(dis);
        } catch (final IOException e) {
            System.err.println("Failed to read CacheFileHeader from input stream");
            e.printStackTrace();
            return null;
        }

        final IQvcFile cacheFile = getCacheFile(header.getQuantizationType());
        assert (cacheFile != null);
        try {
            cacheFile.readFromStream(dis, header);
        } catch (final IOException e) {
            System.err.println("Failed to read cache file from input stream.");
            e.printStackTrace();
            return null;
        }
        return cacheFile;
    }

    /**
     * Read cache file from input stream.
     *
     * @param inputStream Input data stream.
     * @return Cache file or null if reading fails.
     */
    public static IQvcFile readCacheFile(final InputStream inputStream) {
        return readCacheFileImpl(inputStream);
    }


    /**
     * Read cache file from file.
     *
     * @param path File path.
     * @return Cache file or null if reading fails.
     */
    public static IQvcFile readCacheFile(final String path) {
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
        QvcHeaderV1 header = null;
        final long fileSize;
        try (final FileInputStream fis = new FileInputStream(path);
             final DataInputStream dis = new DataInputStream(fis)) {
            fileSize = fis.getChannel().size();
            header = new QvcHeaderV1();
            header.readFromStream(dis);
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }
        final StringBuilder reportBuilder = new StringBuilder();
        final long expectedFileSize = header.getExpectedDataSize();
        if (expectedFileSize == fileSize) {
            reportBuilder.append("\u001B[32mCache file is VALID ").append(fileSize).append(" bytes\u001B[0m\n");
        } else {
            reportBuilder.append("\u001B[31mCache file is INVALID.\u001B[0m\n\t")
                    .append(fileSize).append(" bytes instead of expected ")
                    .append(expectedFileSize).append(" bytes.\n");
        }
        header.report(reportBuilder, path);

        if (verbose) {

            final IQvcFile cacheFile = getCacheFile(header.getQuantizationType());
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
