package azgracompress.compression;

import java.io.DataOutputStream;

public interface IImageCompressor {

    /**
     * Compress the image planes.
     * @param compressStream Compressed data stream.
     * @throws ImageCompressionException when compression fails.
     */
    void compress(DataOutputStream compressStream) throws ImageCompressionException;
}
