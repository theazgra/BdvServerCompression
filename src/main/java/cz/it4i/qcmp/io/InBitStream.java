package cz.it4i.qcmp.io;

import java.io.IOException;
import java.io.InputStream;

public class InBitStream implements AutoCloseable {

    private final InputStream inputStream;
    private final byte[] buffer;
    private int bufferPosition;
    private int bytesAvailable;

    private byte bitBuffer;
    private byte bitBufferSize;

    private final int bitsPerValue;

    private boolean allowReadFromUnderlyingStream = true;

    public InBitStream(final InputStream inputStream, final int bitsPerValue, final int bufferSize) {
        this.inputStream = inputStream;
        this.bitsPerValue = bitsPerValue;

        buffer = new byte[bufferSize];
        bufferPosition = 0;
        bytesAvailable = 0;

        bitBuffer = 0;
        bitBufferSize = 0;
    }

    /**
     * Read entire buffer from input stream.
     *
     * @throws IOException when unable to read from input stream.
     */
    public void fillEntireBuffer() throws IOException {
        int toRead = buffer.length;
        while (toRead > 0) {
            toRead -= inputStream.read(buffer, buffer.length - toRead, toRead);
        }
        bytesAvailable = buffer.length;
        bufferPosition = 0;
    }

    /**
     * Read whole buffer from input stream.
     *
     * @throws IOException when unable to read from input stream.
     */
    private void readToBuffer() throws IOException {
        bytesAvailable = inputStream.read(buffer, 0, buffer.length);

        if (bytesAvailable <= 0) {
            throw new IOException("Unable to read from underlying stream. We have probably reached the end of stream.");
        }

        bufferPosition = 0;
    }


    private void readByteToBitBuffer() throws IOException {

        if (!(bufferPosition < bytesAvailable)) {
            if (!allowReadFromUnderlyingStream) {
                throw new IOException("Can not read from underlying stream.");
            }
            readToBuffer();
        }

        if (bufferPosition < bytesAvailable) {
            bitBuffer = buffer[bufferPosition++];
            bitBufferSize = 8;
        } else {
            assert (false) : "Underlying buffer is empty.";
        }
    }


    public boolean readBit() throws IOException {
        return (readBitFromBuffer() == 1);
    }

    private int readBitFromBuffer() throws IOException {
        if (bitBufferSize == 0) {
            readByteToBitBuffer();
        }
        --bitBufferSize;
        final int bit = bitBuffer & (1 << bitBufferSize);
        return (bit > 0 ? 1 : 0);
    }

    public int readValue() throws IOException {
        int result = 0;
        int bit;

        //writing => bit = (value & (1 << shift));
        for (int shift = 0; shift < bitsPerValue; shift++) {
            bit = readBitFromBuffer();
            result |= (bit << shift);
        }
        return result;
    }

    public int[] readNValues(final int n) throws IOException {
        final int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = readValue();
        }
        return values;
    }

    public boolean canReadFromUnderlyingStream() {
        return allowReadFromUnderlyingStream;
    }

    public void setAllowReadFromUnderlyingStream(final boolean allowReadFromUnderlyingStream) {
        this.allowReadFromUnderlyingStream = allowReadFromUnderlyingStream;
    }


    @Override
    public void close() throws IOException {
        bitBufferSize = 0;
        bytesAvailable = 0;
    }
}
