package azgracompress.io;

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
     * @param buffer Buffer object reference.
     */
    public BufferInputData(Object buffer) {
        this.buffer = buffer;
    }

    /**
     * Get buffer with the data.
     * @return Pointer to array of corresponding pixel values.
     */
    public Object getBuffer() {
        return buffer;
    }
}
