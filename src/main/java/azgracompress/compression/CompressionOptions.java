package azgracompress.compression;

import azgracompress.io.InputData;
import azgracompress.data.V2i;
import azgracompress.fileformat.QuantizationType;

/**
 * Options for the compressor/decompressor.
 */
public class CompressionOptions {

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
     * TODO: Once we support 3D quantization, this should be V3i.
     */
    private V2i vectorDimension = new V2i(0);

    /**
     * Number of workers to be used for different operations.
     */
    private int workerCount = 1;

    /**
     * Flag whether the CLI app should be verbose while running.
     */
    private boolean verbose;


    protected void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    // NOTE(Moravec): Generated getters and setters for the private fields.

    public boolean isVerbose() {
        return verbose;
    }

    public InputData getInputDataInfo() {
        return inputDataInfo;
    }

    public void setInputDataInfo(InputData ifi) {
        this.inputDataInfo = ifi;
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

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public CodebookType getCodebookType() {
        return codebookType;
    }

    public void setCodebookType(CodebookType codebookType) {
        this.codebookType = codebookType;
    }
}
