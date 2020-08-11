package azgracompress.io;

import azgracompress.data.V3i;

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
     * @param imageBuffer     Image buffer reference.
     * @param imageDimensions Image dimensions.
     * @param pixelType       Image pixel type.
     * @param cacheHint       Name of the image used in caching.
     */
    public FlatBufferInputData(final Object imageBuffer,
                               final V3i imageDimensions,
                               final PixelType pixelType,
                               final String cacheHint) {
        this.imageBuffer = imageBuffer;
        setDataLoaderType(DataLoaderType.ImageJBufferLoader);
        setDimension(imageDimensions);
        setPixelType(pixelType);
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
