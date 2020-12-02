package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.cache.IQvcFile;
import cz.it4i.qcmp.compression.exception.ImageCompressionException;
import cz.it4i.qcmp.io.InputData;

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
     * @param inputData      Chunk input data.
     * @return Size of compressed chunks.
     * @throws ImageCompressionException when compression fails
     */
    long[] compressStreamChunk(final DataOutputStream compressStream,
                               final InputData inputData) throws ImageCompressionException;

    /**
     * Train codebook from selected frames and save the learned codebook to cache file.
     *
     * @throws ImageCompressionException when training or saving fails.
     */
    void trainAndSaveCodebook() throws ImageCompressionException;

    /**
     * Train all codebook sizes from selected frames and save learned codebooks to cache files.
     *
     * @throws ImageCompressionException when training or saving of any file fails.
     */
    void trainAndSaveAllCodebooks() throws ImageCompressionException;


    /**
     * Preload compressor codebook and Huffman tree for stream compressor from provided cache file.
     *
     * @param codebookCacheFile Codebook cache file.
     */
    void preloadGlobalCodebook(final IQvcFile codebookCacheFile);
}
