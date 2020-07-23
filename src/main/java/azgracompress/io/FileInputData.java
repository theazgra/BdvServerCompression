package azgracompress.io;

/**
 * Input data backed by the file.
 */
public class FileInputData extends InputData {

    /**
     * Input file path.
     */
    private final String filePath;

    /**
     * Create input data backed by data file.
     *
     * @param filePath
     */
    public FileInputData(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Get path to the data file.
     *
     * @return
     */
    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getCacheFileName() {
        return filePath;
    }
}
