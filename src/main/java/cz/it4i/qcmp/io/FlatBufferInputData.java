package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.HyperStackDimensions;

/**
 * Input data backed by the single buffer object.
 * In this buffer all the dataset pixels are stored.
 */
public class FlatBufferInputData extends InputData {
    /**
     * Reference to the buffer.
     */
    private final Object imageBuffer;

    /**
     * Name of the image used in caching.
     */
    private final String cacheHint;

    /**
     * Create input data backed by buffer object.
     *
     * @param imageBuffer       Image buffer reference.
     * @param datasetDimensions Dataset dimensions.
     * @param cacheHint         Name of the image used in caching.
     */
    public FlatBufferInputData(final Object imageBuffer,
                               final HyperStackDimensions datasetDimensions,
                               final String cacheHint) {
        super(datasetDimensions);
        this.imageBuffer = imageBuffer;
        setDataLoaderType(DataLoaderType.FlatBufferLoader);
        this.cacheHint = cacheHint;
    }

    /**
     * Get buffer with the image plane data.
     *
     * @return Pointer to pixel buffer.
     */
    public Object getPixelBuffer() {
        return imageBuffer;
    }

    @Override
    public String getCacheFileName() {
        return cacheHint;
    }
}
