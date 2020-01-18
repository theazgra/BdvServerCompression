package azgracompress.io;

import java.io.IOException;
import java.io.OutputStream;

public class OutBitStream {
    private OutputStream outStream;

    private byte[] buffer;
    private int bufferPosition;

    private byte bitBuffer = 0x00;
    private byte bitBufferSize = 0x00;

    private final int bitsPerValue;

    public OutBitStream(OutputStream outputStream, final int bitsPerValue, final int bufferSize) {
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
    private void writeBit(final int bit) throws IOException {
        ++bitBufferSize;
        if (bit > 0) {
            bitBuffer |= (1 << (8 - bitBufferSize));
        }

        //        if (bit > 0) {
        //            bitBuffer |= (1 << bitBufferSize);
        //        }
        //        ++bitBufferSize;

        if (bitBufferSize == 8) {
            flushBitBuffer();
        }
    }

    //    public void write(final byte value) {
    //
    //    }
    //
    //    public void write(final short value) {
    //
    //    }


    public void write(final int value) throws IOException {
        int bit;

        for (int shift = 0; shift < bitsPerValue; shift++) {

            bit = (value & (1 << shift));

            //bit = (value & (1 << (31 - shift)));
            writeBit(bit);
        }
    }

    public void write(final int[] values) throws IOException {
        for (final int value : values) {
            write(value);
        }
    }
}
