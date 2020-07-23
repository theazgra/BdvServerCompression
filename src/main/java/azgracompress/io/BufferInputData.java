package azgracompress.io;

import azgracompress.data.V3i;

/**
 * Input data backed by the buffer object.
 */
public class BufferInputData extends InputData {

    /**
     * Reference to the buffer.
     */
    private final Object[] imageBuffers;


    /**
     * Create input data backed by buffer object.
     *
     * @param imageBuffers    Image buffer references.
     * @param imageDimensions Image dimensions.
     * @param pixelType       Image pixel type.
     */
    public BufferInputData(final Object[] imageBuffers, final V3i imageDimensions, final PixelType pixelType) {
        this.imageBuffers = imageBuffers;
        setDataLoaderType(DataLoaderType.ImageJBufferLoader);
        setDimension(imageDimensions);
        setPixelType(pixelType);
    }

    /**
     * Get buffer with the image plane data.
     *
     * @param planeIndex Plane index.
     * @return Pointer to array of corresponding plane pixel values.
     */
    public Object getPixelBuffer(final int planeIndex) {
        assert (planeIndex >= 0);
        assert (planeIndex < imageBuffers.length);
        return imageBuffers[planeIndex];
    }
}
