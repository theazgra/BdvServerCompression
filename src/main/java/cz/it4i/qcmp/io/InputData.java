package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.HyperStackDimensions;
import cz.it4i.qcmp.data.Range;

/**
 * Information about the input file.
 */
public abstract class InputData {
    public enum DataLoaderType {
        RawDataLoader,
        SCIFIOLoader,
        ImageJBufferLoader,
        FlatBufferLoader,
        CallbackLoader
    }

    // NOTE(Moravec): Supporting this will need a lot of work. If only we had C++ templates.
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
     * Dataset dimensions.
     */
    private HyperStackDimensions dimension;

    /**
     * Index of the plane to compress.
     */
    private Integer planeIndex = null;

    /**
     * Range of the planes to compress.
     */
    Range<Integer> planeRange = null;

    /**
     * Create InputData object with dataset dimensions.
     *
     * @param datasetDimensions Dataset dimensions.
     */
    public InputData(final HyperStackDimensions datasetDimensions) {
        this.dimension = datasetDimensions;
    }


    public boolean isPlaneIndexSet() {
        return (planeIndex != null);
    }

    public boolean isPlaneRangeSet() {
        return (planeRange != null);
    }

    public void setDimension(final HyperStackDimensions dimension) {
        this.dimension = dimension;
    }

    public HyperStackDimensions getDimensions() {
        return dimension;
    }

    public Integer getPlaneIndex() {
        return planeIndex;
    }

    public void setPlaneIndex(final Integer planeIndex) {
        this.planeIndex = planeIndex;
    }

    public Range<Integer> getPlaneRange() {
        return planeRange;
    }

    public void setPlaneRange(final Range<Integer> planeRange) {
        this.planeRange = planeRange;
    }

    public DataLoaderType getDataLoaderType() {
        return dataLoaderType;
    }

    public void setDataLoaderType(final DataLoaderType dataLoaderType) {
        this.dataLoaderType = dataLoaderType;
    }

    public PixelType getPixelType() {
        return pixelType;
    }

    public void setPixelType(final PixelType pixelType) {
        this.pixelType = pixelType;
    }

    /**
     * This is mostly used by loaders which are backed by file.
     *
     * @return Basic non-overloaded versions always returns null.
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
