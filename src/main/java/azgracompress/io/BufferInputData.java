package azgracompress.io;

import azgracompress.data.V3i;

/**
 * Input data backed by the buffer object.
 */
public class BufferInputData extends InputData {

    /**
     * Reference to the buffer.
     */
    private final Object buffer;


    /**
     * Create input data backed by buffer object.
     *
     * @param buffer          Buffer object reference.
     * @param imageDimensions Image dimensions.
     * @param pixelType       Image pixel type.
     */
    public BufferInputData(final Object buffer, final V3i imageDimensions, final PixelType pixelType) {
        this.buffer = buffer;
        setDataLoaderType(DataLoaderType.ImageJBufferLoader);
        setDimension(imageDimensions);
        setPixelType(pixelType);
    }

    /**
     * Get buffer with the data.
     *
     * @return Pointer to array of corresponding pixel values.
     */
    public Object getBuffer() {
        return buffer;
    }
}
