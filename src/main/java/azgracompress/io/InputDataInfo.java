package azgracompress.io;

import azgracompress.compression.Interval;
import azgracompress.data.V3i;

/**
 * Information about the input file.
 */
public class InputDataInfo {
    public enum DataLoaderType {
        RawDataLoader,
        SCIFIOLoader,
        ImageJBufferLoader
    }

    /**
     * Input file path.
     */
    private final String filePath;

    /**
     * Type of an input data source.
     */
    private DataLoaderType dataLoaderType = DataLoaderType.RawDataLoader;

    /**
     * Image dimension.
     */
    private V3i dimension;

    /**
     * Index of the plane to compress.
     */
    private Integer planeIndex = null;

    /**
     * Range of the planes to compress.
     */
    Interval<Integer> planeRange = null;

    public InputDataInfo(final String filePath) {
        this.filePath = filePath;
    }

    public boolean isPlaneIndexSet() {
        return (planeIndex != null);
    }

    public boolean isPlaneRangeSet() {
        return (planeRange != null);
    }

    public void setDimension(final V3i dimension) {
        this.dimension = dimension;
    }

    public String getFilePath() {
        return filePath;
    }

    public V3i getDimensions() {
        return dimension;
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

    public DataLoaderType getDataLoaderType() {
        return dataLoaderType;
    }

    public void setDataLoaderType(DataLoaderType dataLoaderType) {
        this.dataLoaderType = dataLoaderType;
    }
}
