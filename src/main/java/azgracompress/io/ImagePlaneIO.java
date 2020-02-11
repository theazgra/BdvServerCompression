package azgracompress.io;

import azgracompress.data.ImageU16;
import azgracompress.data.V3i;
import io.scif.jj2000.j2k.NotImplementedError;

import java.io.IOException;

/**
 * Class handling loading and writing of plane data.
 * Multiple data types should be supported.
 * For start: RAW, TIFF
 */
public class ImagePlaneIO {

    public static ImageU16 loadImageU16(final String rawFile,
                                        final V3i rawDataDimension,
                                        final int plane) throws IOException {

        // TODO(Moravec): Handle loading of different types.
        // TODO(Moravec): If the loaded image is not U16 convert it to U16 image.
        throw new NotImplementedError();
    }

    public static int[] loadPlanesData(final String rawFile,
                                       final V3i rawDataDims,
                                       int[] planes) throws IOException {
        // TODO(Moravec): Handle loading of different types.
        // TODO(Moravec): If the loaded image is not U16 convert it to U16 image.
        throw new NotImplementedError();
    }

    public static int[] loadAllPlanesData(final String rawFile, final V3i imageDims) throws IOException {
        // TODO(Moravec): Handle loading of different types.
        // TODO(Moravec): If the loaded image is not U16 convert it to U16 image.
        throw new NotImplementedError();
    }

    public static void writeImageU16(final String rawFile,
                                     final ImageU16 image,
                                     final boolean littleEndian) throws IOException {

        // TODO(Moravec): Handle writing of U16 image to multiple types.
        throw new NotImplementedError();
    }
}
