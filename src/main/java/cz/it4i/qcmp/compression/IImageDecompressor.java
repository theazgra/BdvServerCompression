package cz.it4i.qcmp.compression;

import cz.it4i.qcmp.cache.ICacheFile;
import cz.it4i.qcmp.compression.exception.ImageDecompressionException;
import cz.it4i.qcmp.fileformat.QCMPFileHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface IImageDecompressor extends IListenable {
    /**
     * Get correct size of data block.
     *
     * @param header QCMPFile header with information about compressed file.
     * @return Expected size of data.
     */
    long getExpectedDataSize(final QCMPFileHeader header);

    /**
     * Decompress the image planes to decompress stream.
     *
     * @param compressedStream Input stream of compressed data.
     * @param decompressStream Output stream for decompressed data.
     * @param header           QCMPFile information.
     * @throws ImageDecompressionException when decompression fails.
     */
    void decompress(DataInputStream compressedStream,
                    DataOutputStream decompressStream,
                    final QCMPFileHeader header) throws ImageDecompressionException;

    /**
     * Decompress the image planes to memory buffer.
     *
     * @param compressedStream Input stream of compressed data.
     * @param buffer           Buffer to store decompressed pixels.
     * @param header           QCMPFile information.
     * @throws ImageDecompressionException when decompression fails.
     */
    void decompressToBuffer(DataInputStream compressedStream,
                            short[][] buffer,
                            final QCMPFileHeader header) throws ImageDecompressionException;

    short[] decompressStreamMode(final DataInputStream compressedStream,
                                 final QCMPFileHeader header) throws ImageDecompressionException;

    /**
     * Preload decompressor codebook and Huffman tree for stream decompressor from provided cache file.
     *
     * @param codebookCacheFile Codebook cache file.
     */
    void preloadGlobalCodebook(final ICacheFile codebookCacheFile);
}
