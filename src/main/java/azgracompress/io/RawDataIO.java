package azgracompress.io;

import azgracompress.data.ImageU16;
import azgracompress.utilities.TypeConverter;

import java.io.FileOutputStream;
import java.io.IOException;

public class RawDataIO {

    public static void writeImageU16(final String rawFile,
                                     final ImageU16 image,
                                     final boolean littleEndian) throws IOException {
        byte[] buffer = TypeConverter.unsignedShortArrayToByteArray(image.getData(), littleEndian);
        writeBytesToFile(rawFile, buffer);
    }

    public static void writeDataI32(String rawFile,
                                    int[] differenceData,
                                    final boolean littleEndian) throws IOException {
        byte[] buffer = TypeConverter.intArrayToByteArray(differenceData, littleEndian);
        writeBytesToFile(rawFile, buffer);
    }

    public static void writeBytesToFile(String rawFile,
                                        byte[] buffer) throws IOException {
        FileOutputStream fileStream = new FileOutputStream(rawFile, false);
        fileStream.write(buffer, 0, buffer.length);
        fileStream.flush();
        fileStream.close();
    }


}
