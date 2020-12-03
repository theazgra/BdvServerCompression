package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.*;
import cz.it4i.qcmp.io.RawDataIO;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class QvcFileReader {
    private static final int QVC_HEADER_MAGIC_VALUE_SIZE = 9;

    /**
     * Read cache file from file.
     *
     * @param path File path.
     * @return Cache file or null if reading fails.
     */
    public static IQvcFile readCacheFile(final String path) throws IOException {
        try (final FileInputStream fis = new FileInputStream(path)) {
            return readCacheFileImpl(fis);
        }
    }

    /**
     * Make DataInputStream from InputStream.
     *
     * @param inputStream Some input stream.
     * @return DataInputStream.
     */
    private static DataInputStream asDataInputStream(final InputStream inputStream) {
        if (inputStream instanceof DataInputStream) {
            return (DataInputStream) inputStream;
        } else {
            return new DataInputStream(inputStream);
        }
    }


    /**
     * Create correct Qvc header version by analyzing the magic value.
     *
     * @param magicValue Magic value of the qvc file.
     * @return Correct version of QVC header.
     * @throws IOException when the magic value is unknown.
     */
    private static IQvcHeader getCorrectQvcHeader(final String magicValue) throws IOException {
        switch (magicValue) {
            case QvcHeaderV1.MAGIC_VALUE:
                return new QvcHeaderV1();
            case QvcHeaderV2.MAGIC_VALUE:
                return new QvcHeaderV2();
            default:
                throw new IOException("Invalid QVC file. Unknown QVC magic value: " + magicValue);
        }
    }

    /**
     * Create correct Qvc file by analyzing the quantization type.
     *
     * @param quantizationType Quantization type of codebook.
     * @return Correct version of QVC file.
     * @throws IOException when the quantization type is unknown.
     */
    private static IQvcFile getCorrectQvcFile(final QuantizationType quantizationType) throws IOException {
        switch (quantizationType) {
            case Scalar:
                return new SqQvcFile();
            case Vector1D:
            case Vector2D:
            case Vector3D:
                return new VqQvcFile();
            default:
                throw new IOException("Invalid quantization type. Unable to create qvc file impl.");
        }
    }

    /**
     * Read cache file by DataInputStream.
     *
     * @param inputStream Input stream.
     * @return Cache file or null, if exception occurs.
     */
    private static IQvcFile readCacheFileImpl(final InputStream inputStream) throws IOException {
        final DataInputStream dis = asDataInputStream(inputStream);

        final byte[] magicValueBuffer = new byte[QVC_HEADER_MAGIC_VALUE_SIZE];
        RawDataIO.readFullBuffer(dis, magicValueBuffer);
        final String magicValue = new String(magicValueBuffer);

        final IQvcHeader header = getCorrectQvcHeader(magicValue);
        header.readFromStream(dis);

        final IQvcFile cacheFile = getCorrectQvcFile(header.getQuantizationType());
        cacheFile.readFromStream(dis, header);

        return cacheFile;
    }
}
