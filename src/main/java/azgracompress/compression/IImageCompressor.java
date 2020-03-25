package azgracompress.compression;

import azgracompress.compression.exception.ImageCompressionException;

import java.io.DataOutputStream;

public interface IImageCompressor {

    /**
     * Compress the image planes.
     *
     * @param compressStream Compressed data stream.
     * @throws ImageCompressionException when compression fails.
     */
    long[] compress(DataOutputStream compressStream) throws ImageCompressionException;

    /**
     * Train codebook from selected frames and save the learned codebook to cache file.
     *
     * @throws ImageCompressionException when training or saving fails.
     */
    void trainAndSaveCodebook() throws ImageCompressionException;
}
