package azgracompress.compression;

import azgracompress.fileformat.QCMPFileHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface IImageDecompressor {
    /**
     * Get correct size of data block.
     *
     * @param header QCMPFile header with information about compressed file.
     * @return Expected size of data.
     */
    long getExpectedDataSize(final QCMPFileHeader header);

    /**
     * Decompress the image planes.
     *
     * @param compressedStream Input stream of compressed data.
     * @param decompressStream Output stream for decompressed data.
     * @param header           QCMPFile information.
     * @throws ImageDecompressionException when decompression fails.
     */
    void decompress(DataInputStream compressedStream,
                    DataOutputStream decompressStream,
                    final QCMPFileHeader header) throws ImageDecompressionException;

}
