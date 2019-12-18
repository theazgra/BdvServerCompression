package compression.io;

import compression.data.ImageU16;
import compression.data.V3i;
import compression.utilities.TypeConverter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class RawDataIO {
    /**
     * Load single U16 image from RAW data file.
     *
     * @param rawFile          Path to the raw file.
     * @param rawDataDimension X (Width), Y (Height) of plane and Z(Number of planes)
     * @param plane            Plane index.
     * @return U16 image specified by the plane
     */
    public static ImageU16 loadImageU16(final String rawFile,
                                        final V3i rawDataDimension,
                                        final int plane) throws Exception {
        FileInputStream fileStream = new FileInputStream(rawFile);

        final long planeSize = (long) rawDataDimension.getX() * (long) rawDataDimension.getY() * 2;
        final long expectedFileSize = planeSize * rawDataDimension.getZ();
        final long fileSize = fileStream.getChannel().size();


        if (expectedFileSize != fileSize) {
            throw new Exception(
                    "File specified by `rawFile` doesn't contains raw data for image of dimensions `rawDataDimension`");
        }

        final long planeOffset = plane * planeSize;

        byte[] buffer = new byte[(int) planeSize];
        if (fileStream.skip(planeOffset) != planeOffset) {
            throw new Exception("Failed to skip.");
        }
        if (fileStream.read(buffer, 0, (int) planeSize) != planeSize) {
            throw new Exception("Read wrong number of bytes.");
        }

        fileStream.close();

        ImageU16 image = new ImageU16(rawDataDimension.getX(),
                                      rawDataDimension.getY(),
                                      TypeConverter.shortBytesToShortArray(buffer));
        return image;
    }

    public static void writeImageU16(final String rawFile,
                                     final ImageU16 image,
                                     final boolean littleEndian) throws IOException {
        byte[] buffer = TypeConverter.shortArrayToByteArray(image.getData(), littleEndian);
        writeBytesToFile(rawFile, buffer);
    }

    public static boolean writeDataI32(String rawFile, int[] differenceData, final boolean littleEndian) {

        byte[] buffer = TypeConverter.intArrayToByteArray(differenceData, littleEndian);
        try {
            writeBytesToFile(rawFile, buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void writeBytesToFile(String rawFile, byte[] buffer) throws IOException {
        FileOutputStream fileStream = new FileOutputStream(rawFile, false);
        fileStream.write(buffer, 0, buffer.length);
        fileStream.flush();
        fileStream.close();
    }
}