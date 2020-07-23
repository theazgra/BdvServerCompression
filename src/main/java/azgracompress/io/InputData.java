package azgracompress.io;

import azgracompress.compression.Interval;
import azgracompress.data.V3i;

/**
 * Information about the input file.
 */
public abstract class InputData {
    public enum DataLoaderType {
        RawDataLoader,
        SCIFIOLoader,
        ImageJBufferLoader
    }

    public enum PixelType {
        Gray16,
        AnythingElse
    }


    /**
     * Type of an input data source.
     */
    private DataLoaderType dataLoaderType = DataLoaderType.RawDataLoader;

    /**
     * Pixel type of the image data.
     */
    private PixelType pixelType = PixelType.Gray16;

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


    public boolean isPlaneIndexSet() {
        return (planeIndex != null);
    }

    public boolean isPlaneRangeSet() {
        return (planeRange != null);
    }

    public void setDimension(final V3i dimension) {
        this.dimension = dimension;
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

    public PixelType getPixelType() {
        return pixelType;
    }

    public void setPixelType(PixelType pixelType) {
        this.pixelType = pixelType;
    }


    /**
     * Override in FileInputData!!!
     *
     * @return null!
     */
    public String getFilePath() {
        return null;
    }

    /**
     * Get name used in creation of qcmp cache file.
     *
     * @return Name used for cache file.
     */
    public abstract String getCacheFileName();
}
