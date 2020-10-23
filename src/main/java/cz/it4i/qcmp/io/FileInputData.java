package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.HyperStackDimensions;

public class FileInputData extends InputData {

    /**
     * Input file path.
     */
    private final String filePath;

    /**
     * Create input data backed by data file.
     *
     * @param filePath          Path to the file.
     * @param datasetDimensions Dataset dimensions.
     */
    public FileInputData(final String filePath,
                         final HyperStackDimensions datasetDimensions) {
        super(datasetDimensions);
        this.filePath = filePath;
    }

    /**
     * Get path to the data file.
     *
     * @return Path to data file.
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
