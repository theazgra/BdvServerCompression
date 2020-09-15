package azgracompress.io;

import java.io.ByteArrayOutputStream;

public class MemoryOutputStream extends ByteArrayOutputStream {
    public MemoryOutputStream() {
        super();
    }

    public MemoryOutputStream(final int initialBufferSize) {
        super(initialBufferSize);
    }

    public byte[] getBuffer() {
        return this.buf;
    }

    public int getCurrentBufferLength() {
        return this.count;
    }
}
