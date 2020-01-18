package azgracompress.io;

import java.io.IOException;
import java.io.InputStream;

public class InBitStream {

    private InputStream inputStream;
    private byte[] buffer;
    private int bufferPosition;
    private int bytesAvailable;

    private byte bitBuffer;
    private byte bitBufferSize;

    private final int bitsPerValue;

    private boolean allowReadFromUnderlyingStream = true;

    public InBitStream(InputStream inputStream, final int bitsPerValue, final int bufferSize) {
        this.inputStream = inputStream;
        this.bitsPerValue = bitsPerValue;

        buffer = new byte[bufferSize];
        bufferPosition = 0;
        bytesAvailable = 0;

        bitBuffer = 0;
        bitBufferSize = 0;
    }

    public void readToBuffer() throws IOException {
        bytesAvailable = inputStream.read(buffer, 0, buffer.length);
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

    private int readBit() throws IOException {
        if (bitBufferSize == 0) {
            readByteToBitBuffer();
        }
        --bitBufferSize;
        int bit = bitBuffer & (1 << bitBufferSize);
        return (bit > 0 ? 1 : 0);
    }

    public int readValue() throws IOException {
        int result = 0;
        int bit;

        //writing => bit = (value & (1 << shift));
        for (int shift = 0; shift < bitsPerValue; shift++) {
            bit = readBit();
            result |= (bit << shift);
        }
        return result;
    }

    public int[] readNValues(final int n) throws IOException {
        int[] values = new int[n];
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


}
