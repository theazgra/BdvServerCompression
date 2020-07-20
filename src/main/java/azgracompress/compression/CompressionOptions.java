package azgracompress.compression;

import azgracompress.cli.InputFileInfo;
import azgracompress.data.V2i;
import azgracompress.data.V3i;
import azgracompress.fileformat.QuantizationType;

/**
 * Options for the compressor/decompressor.
 */
public class CompressionOptions {
    /**
     * Input image or compressed file.
     */
    private InputFileInfo inputFileInfo;

    /**
     * Output image or compressed file.
     */
    private String outputFilePath;

    /**
     * Type of quantization.
     */
    private QuantizationType quantizationType;

    /**
     * Directory which contains codebook caches.
     */
    private String codebookCacheFolder = null;

    /**
     * Number of bits, which are used to encode single codebook index.
     */
    private int bitsPerCodebookIndex = 8;

    /**
     * Dimensions of the quantization vector.
     * TODO: Once we support 3D quantization, this should be V3i.
     */
    private V2i vectorDimension = new V2i(0);

    /**
     * Dimensions of the input image data.
     */
    private V3i imageDimension = new V3i(0);

    /**
     * Flag, whether to use middle plane as reference plane for codebook creation.
     */
    private boolean useMiddlePlane = false;

    /**
     * Index of the plane to compress.
     */
    private Integer planeIndex = null;

    /**
     * Range of the planes to compress.
     */
    Interval<Integer> planeRange = null;

    /**
     * Number of workers to be used for different operations.
     */
    private int workerCount = 1;

    /**
     * Flag whether the CLI app should be verbose while running.
     */
    private boolean verbose;

    public boolean isPlaneIndexSet() {
        return (planeIndex != null);
    }

    public boolean isPlaneRangeSet() {
        return (planeRange != null);
    }

    public boolean hasCodebookCacheFolder() {
        return codebookCacheFolder != null;
    }

    protected void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    // NOTE(Moravec): Generated getters and setters for the private fields.

    public boolean isVerbose() {
        return verbose;
    }

    public InputFileInfo getInputFileInfo() {
        return inputFileInfo;
    }

    public void setInputFileInfo(InputFileInfo ifi) {
        this.inputFileInfo = ifi;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public QuantizationType getQuantizationType() {
        return quantizationType;
    }

    public void setQuantizationType(QuantizationType quantizationType) {
        this.quantizationType = quantizationType;
    }

    public String getCodebookCacheFolder() {
        return codebookCacheFolder;
    }

    public void setCodebookCacheFolder(String codebookCacheFolder) {
        this.codebookCacheFolder = codebookCacheFolder;
    }

    public int getBitsPerCodebookIndex() {
        return bitsPerCodebookIndex;
    }

    public void setBitsPerCodebookIndex(int bitsPerCodebookIndex) {
        this.bitsPerCodebookIndex = bitsPerCodebookIndex;
    }

    public V2i getVectorDimension() {
        return vectorDimension;
    }

    public void setVectorDimension(V2i vectorDimension) {
        this.vectorDimension = vectorDimension;
    }

    public V3i getImageDimension() {
        return imageDimension;
    }

    public void setImageDimension(V3i imageDimension) {
        this.imageDimension = imageDimension;
    }

    public boolean shouldUseMiddlePlane() {
        return useMiddlePlane;
    }

    public void setUseMiddlePlane(boolean useMiddlePlane) {
        this.useMiddlePlane = useMiddlePlane;
    }

    public Integer getPlaneIndex() {
        return planeIndex;
    }

    public void setPlaneIndex(Integer planeIndex) {
        this.planeIndex = planeIndex;
    }

    public Interval<Integer> getPlaneRange() {
        return planeRange;
    }

    public void setPlaneRange(Interval<Integer> planeRange) {
        this.planeRange = planeRange;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }
}
