package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.HyperStackDimensions;

/**
 * Input data backed by the buffer object.
 */
public class BufferInputData extends InputData {

    /**
     * Reference to the buffer.
     */
    private final Object[] imageBuffers;

    private final String cacheHint;

    /**
     * Create input data backed by buffer object.
     *
     * @param imageBuffers      Image buffer references.
     * @param datasetDimensions Dataset dimensions.
     * @param cacheHint         Name of the image used in caching.
     */
    public BufferInputData(final Object[] imageBuffers,
                           final HyperStackDimensions datasetDimensions,
                           final String cacheHint) {
        super(datasetDimensions);
        this.imageBuffers = imageBuffers;
        setDataLoaderType(DataLoaderType.ImageJBufferLoader);
        this.cacheHint = cacheHint;
    }

    /**
     * Get buffer with the image plane data.
     *
     * @param planeIndex Plane index.
     * @return Pointer to array of corresponding plane pixel values.
     */
    public Object getPixelBuffer(final int planeIndex) {
        return imageBuffers[planeIndex];
    }

    @Override
    public String getCacheFileName() {
        return cacheHint;
    }
}
