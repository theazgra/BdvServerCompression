package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.cache.ICacheFile;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.fileformat.QuantizationType;
import cz.it4i.qcmp.io.InputData;

/**
 * Options for the compressor/decompressor.
 */
public class CompressionOptions implements Cloneable {

    public enum CodebookType {
        Individual,
        MiddlePlane,
        Global,
        Invalid
    }

    /**
     * Input image or compressed file.
     */
    private InputData inputDataInfo;

    /**
     * Output image or compressed file.
     */
    private String outputFilePath;

    /**
     * Type of quantization.
     */
    private QuantizationType quantizationType;

    /**
     * Type of the codebook.
     */
    private CodebookType codebookType = CodebookType.Individual;

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
     */
    private V3i quantizationVector = new V3i(0);

    /**
     * Number of workers to be used for different operations.
     */
    private int workerCount = 1;

    /**
     * Flag whether the CLI app should be verbose while running.
     */
    private boolean verbose = false;

    private boolean processAllTimepoints = false;

    public CompressionOptions() {
        final int cores = Runtime.getRuntime().availableProcessors();
        this.workerCount = (cores / 2);
    }

    public CompressionOptions(final ICacheFile codebookCacheFile) {
        this();
        quantizationType = codebookCacheFile.getHeader().getQuantizationType();
        bitsPerCodebookIndex = codebookCacheFile.getHeader().getBitsPerCodebookIndex();
        codebookType = CodebookType.Global;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Create clone of this object.
     *
     * @return Cloned object.
     */
    public CompressionOptions createClone() {
        try {
            return (CompressionOptions) clone();
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    // NOTE(Moravec): Generated getters and setters for the private fields.

    public boolean isVerbose() {
        return verbose;
    }

    public InputData getInputDataInfo() {
        return inputDataInfo;
    }

    public void setInputDataInfo(final InputData ifi) {
        this.inputDataInfo = ifi;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(final String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public QuantizationType getQuantizationType() {
        return quantizationType;
    }

    public void setQuantizationType(final QuantizationType quantizationType) {
        this.quantizationType = quantizationType;
    }

    public String getCodebookCacheFolder() {
        return codebookCacheFolder;
    }

    public void setCodebookCacheFolder(final String codebookCacheFolder) {
        this.codebookCacheFolder = codebookCacheFolder;
    }

    public int getBitsPerCodebookIndex() {
        return bitsPerCodebookIndex;
    }

    public void setBitsPerCodebookIndex(final int bitsPerCodebookIndex) {
        this.bitsPerCodebookIndex = bitsPerCodebookIndex;
    }

    public V3i getQuantizationVector() {
        return quantizationVector;
    }

    public void setQuantizationVector(final V3i quantizationVector) {
        this.quantizationVector = quantizationVector;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(final int workerCount) {
        this.workerCount = workerCount;
    }

    public CodebookType getCodebookType() {
        return codebookType;
    }

    public void setCodebookType(final CodebookType codebookType) {
        this.codebookType = codebookType;
    }

    public boolean isConsoleApplication() {
        return false;
    }

    public boolean shouldProcessAllTimepoints() {
        return processAllTimepoints;
    }

    public void setProcessAllTimepoints(final boolean processAllTimepoints) {
        this.processAllTimepoints = processAllTimepoints;
    }

}
