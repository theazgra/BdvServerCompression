package azgracompress.compression;

import java.io.DataOutputStream;

public interface IImageCompressor {

    // TODO(Moravec): Replace default Exception with better Exception type.

    /**
     * Compress the image planes.
     * @param compressStream Compressed data stream.
     * @throws Exception when compression fails.
     */
    void compress(DataOutputStream compressStream) throws Exception;
}
