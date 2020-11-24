package cz.it4i.qcmp.io;

import java.io.IOException;
import java.io.OutputStream;

public class OutBitStream implements AutoCloseable {
    private final OutputStream outStream;

    private final byte[] buffer;
    private int bufferPosition;

    private byte bitBuffer = 0x00;
    private byte bitBufferSize = 0x00;

    private final int bitsPerValue;

    private long bytesWritten = 0;

    public OutBitStream(final OutputStream outputStream, final int bitsPerValue, final int bufferSize) {
        outStream = outputStream;

        this.bitsPerValue = bitsPerValue;

        buffer = new byte[bufferSize];
        bufferPosition = 0;

        bitBuffer = 0;
        bitBufferSize = 0;
    }

    /**
     * Flush the memory buffer to the underlying stream.
     */
    private void flushBuffer() throws IOException {
        outStream.write(buffer, 0, bufferPosition);
        bytesWritten += bufferPosition;
        bufferPosition = 0;
    }

    /**
     * Flush the bit buffer into the memory buffer.
     */
    private void flushBitBuffer() throws IOException {
        if (bitBufferSize > 0) {

            buffer[bufferPosition++] = bitBuffer;

            bitBuffer = 0;
            bitBufferSize = 0;

            if (bufferPosition == buffer.length) {
                flushBuffer();
            }
        }
    }

    public void flush() throws IOException {
        flushBitBuffer();
        flushBuffer();
    }

    /**
     * Write bit to the memory bit buffer.
     *
     * @param bit True for 1
     */
    public void writeBit(final int bit) throws IOException {
        writeBit(bit > 0);
    }

    private void writeBit(final boolean bit) throws IOException {
        ++bitBufferSize;

        if (bit) {
            bitBuffer |= (1 << (8 - bitBufferSize));
        }

        if (bitBufferSize == 8) {
            flushBitBuffer();
        }
    }

    public void write(final int value) throws IOException {
        int bit;

        for (int shift = 0; shift < bitsPerValue; shift++) {
            bit = (value & (1 << shift));
            writeBit(bit);
        }
    }


    public void write(final boolean[] bits) throws IOException {
        for (final boolean bit : bits) {
            writeBit(bit);
        }
    }

    public void write(final int[] values) throws IOException {
        for (final int value : values) {
            write(value);
        }
    }

    /**
     * Flush the bitsteam on close.
     *
     * @throws Exception when flush fails.
     */
    @Override
    public void close() throws IOException {
        flush();
    }

    /**
     * Get the number of bytes written to this stream so far.
     *
     * @return Bytes written.
     */
    public long getBytesWritten() {
        // Bytes written to the underlying stream + bytes count in this stream buffer.
        return bytesWritten + bufferPosition + ((bitBufferSize > 0) ? 1 : 0);
    }
}
