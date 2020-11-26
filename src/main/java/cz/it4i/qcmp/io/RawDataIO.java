package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.ImageU16;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RawDataIO {

    public static void writeImageU16(final String rawFile,
                                     final ImageU16 image,
                                     final boolean littleEndian) throws IOException {
        final byte[] buffer = TypeConverter.unsignedShortArrayToByteArray(image.getData(), littleEndian);
        writeBytesToFile(rawFile, buffer);
    }

    public static void writeDataI32(final String rawFile,
                                    final int[] differenceData,
                                    final boolean littleEndian) throws IOException {
        final byte[] buffer = TypeConverter.intArrayToByteArray(differenceData, littleEndian);
        writeBytesToFile(rawFile, buffer);
    }

    public static void writeBytesToFile(final String rawFile,
                                        final byte[] buffer) throws IOException {
        final FileOutputStream fileStream = new FileOutputStream(rawFile, false);
        fileStream.write(buffer, 0, buffer.length);
        fileStream.flush();
        fileStream.close();
    }

    /**
     * Read exactly N bytes from stream into the buffer.
     *
     * @param stream Input stream.
     * @param buffer Memory buffer.
     * @throws IOException when unable to read buffer.length bytes.
     */
    public static void readFullBuffer(final InputStream stream, final byte[] buffer) throws IOException {
        int toRead = buffer.length;
        while (toRead > 0) {
            final int read = stream.read(buffer, (buffer.length - toRead), toRead);
            if (read < 0) {
                throw new IOException("Unable to read requested number of bytes.");
            }
            toRead -= read;
        }
    }


    public static void write(final String outFile, final int[] sliceData, final boolean littleEndian) throws IOException {
        writeBytesToFile(outFile, TypeConverter.unsignedShortArrayToByteArray(sliceData, littleEndian));
    }
}
