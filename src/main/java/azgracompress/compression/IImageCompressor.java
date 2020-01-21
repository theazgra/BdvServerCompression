package azgracompress.compression;

import azgracompress.compression.exception.ImageCompressionException;

import java.io.DataOutputStream;

public interface IImageCompressor {

    /**
     * Compress the image planes.
     * @param compressStream Compressed data stream.
     * @throws ImageCompressionException when compression fails.
     */
    void compress(DataOutputStream compressStream) throws ImageCompressionException;
}
