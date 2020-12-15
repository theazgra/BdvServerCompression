package cz.it4i.qcmp.io;

import cz.it4i.qcmp.fileformat.IFileHeader;
import cz.it4i.qcmp.fileformat.IQcmpFile;
import cz.it4i.qcmp.fileformat.QCMPFileHeaderV1;
import cz.it4i.qcmp.fileformat.QCMPFileHeaderV2;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class QcmpFileReader {
    private static final int QCMP_HEADER_MAGIC_VALUE_SIZE = 8;

    /**
     * Read cache file from file.
     *
     * @param path File path.
     * @return Cache file or null if reading fails.
     */
    public static IQcmpFile readCacheFile(final String path) throws IOException {
        try (final FileInputStream fis = new FileInputStream(path)) {
            return readQcmpFile(fis);
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
     * Create correct Qcmp header version by analyzing the magic value.
     *
     * @param magicValue Magic value of the qcmp file.
     * @return Correct version of QCMP header.
     * @throws IOException when the magic value is unknown.
     */
    private static IFileHeader getCorrectQcmpHeader(final String magicValue) throws IOException {
        switch (magicValue) {
            case QCMPFileHeaderV1.MAGIC_VALUE:
                return new QCMPFileHeaderV1();
            case QCMPFileHeaderV2.MAGIC_VALUE:
                return new QCMPFileHeaderV2();
            default:
                throw new IOException("Invalid QCMP file. Unknown QCMP magic value: " + magicValue);
        }
    }

    /**
     * Read cache file by DataInputStream.
     *
     * @param inputStream Input stream.
     * @return Cache file or null, if exception occurs.
     */
    private static IQcmpFile readQcmpFile(final InputStream inputStream) throws IOException {
        final DataInputStream dis = asDataInputStream(inputStream);

        final byte[] magicValueBuffer = new byte[QCMP_HEADER_MAGIC_VALUE_SIZE];
        RawDataIO.readFullBuffer(dis, magicValueBuffer);
        final String magicValue = new String(magicValueBuffer);

        final IFileHeader header = getCorrectQcmpHeader(magicValue);
        header.readFromStream(dis);

        assert ((header.getHeaderVersion() == 1 && header instanceof QCMPFileHeaderV1) ||
                header.getHeaderVersion() == 2 && header instanceof QCMPFileHeaderV2);


        // TODO(Moravec): Create the IQcmpFile impl. Think about it!
        return null;
    }
}
