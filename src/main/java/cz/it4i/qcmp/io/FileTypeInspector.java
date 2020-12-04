package cz.it4i.qcmp.io;

import cz.it4i.qcmp.fileformat.QCMPFileHeader;
import cz.it4i.qcmp.fileformat.QvcHeaderV1;
import cz.it4i.qcmp.fileformat.QvcHeaderV2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileTypeInspector {
    public enum FileType {
        /**
         * Compressed image hyperstack.
         */
        Qcmp,
        /**
         * Codebook cache file.
         */
        Qvc,
        /**
         * Invalid file path.
         */
        InvalidPath,
        /**
         * No known magic value was matched.
         */
        Unknown
    }

    /**
     * Inspect file specified by path and return its type.
     *
     * @param filePath Path to the file.
     * @return File type.
     */
    public static FileType inspectFile(final String filePath) {
        // QCMPFileHeader.MAGIC_VALUE   // 8 bytes
        // QvcHeaderV1.MAGIC_VALUE      // 9 bytes
        // QvcHeaderV2.MAGIC_VALUE      // 9 bytes
        try (final FileInputStream stream = new FileInputStream(filePath)) {
            final byte[] buf1 = new byte[QCMPFileHeader.MAGIC_VALUE.length()];
            RawDataIO.readFullBuffer(stream, buf1);
            if (new String(buf1).equals(QCMPFileHeader.MAGIC_VALUE)) {
                return FileType.Qcmp;
            }

            final byte[] buf2 = new byte[QvcHeaderV1.MAGIC_VALUE.length()];
            System.arraycopy(buf1, 0, buf2, 0, buf1.length);
            final int read = stream.read(buf2, buf1.length, 1);
            if (read != 1)
                return FileType.Unknown;

            final String magicValue = new String(buf2);
            if (magicValue.equals(QvcHeaderV1.MAGIC_VALUE) || magicValue.equals(QvcHeaderV2.MAGIC_VALUE))
                return FileType.Qvc;
        } catch (final FileNotFoundException e) {
            return FileType.InvalidPath;
        } catch (final IOException e) {
            return FileType.Unknown;
        }
        return FileType.Unknown;
    }
}
