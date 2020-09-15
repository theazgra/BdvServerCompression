package azgracompress.compression;

import azgracompress.cache.ICacheFile;
import azgracompress.compression.exception.ImageCompressionException;

import java.io.DataOutputStream;

public interface IImageCompressor extends IListenable {

    /**
     * Compress the image planes.
     *
     * @param compressStream Compressed data stream.
     * @return Size of compressed chunks.
     * @throws ImageCompressionException when compression fails.
     */
    long[] compress(DataOutputStream compressStream) throws ImageCompressionException;


    /**
     * Compress image planes in stream mode. QCMP header is not written to the stream only compressed image data without any additional
     * META information.
     *
     * @param compressStream Compressed data stream.
     * @return Size of compressed chunks.
     * @throws ImageCompressionException when compression fails
     */
    long[] compressStreamMode(DataOutputStream compressStream) throws ImageCompressionException;

    /**
     * Train codebook from selected frames and save the learned codebook to cache file.
     *
     * @throws ImageCompressionException when training or saving fails.
     */
    void trainAndSaveCodebook() throws ImageCompressionException;

    /**
     * Preload compressor codebook and Huffman tree for stream compressor from provided cache file.
     *
     * @param codebookCacheFile Codebook cache file.
     */
    void preloadGlobalCodebook(final ICacheFile codebookCacheFile);
}
