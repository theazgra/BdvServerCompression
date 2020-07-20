package azgracompress.io;

import azgracompress.ScifioWrapper;
import azgracompress.cli.InputFileInfo;
import azgracompress.data.ImageU16;
import azgracompress.data.V3i;
import azgracompress.utilities.TypeConverter;
import io.scif.FormatException;
import io.scif.Reader;

import java.io.IOException;
import java.util.Arrays;

public class SCIFIOLoader implements IPlaneLoader {

    private final InputFileInfo inputFileInfo;
    private final Reader reader;

    /**
     * Create SCIFIO reader from input file.
     *
     * @param inputFileInfo Input file info.
     * @throws IOException     When fails to create SCIFIO reader.
     * @throws FormatException When fails to create SCIFIO reader.
     */
    public SCIFIOLoader(final InputFileInfo inputFileInfo) throws IOException, FormatException {
        this.inputFileInfo = inputFileInfo;
        this.reader = ScifioWrapper.getReader(this.inputFileInfo.getFilePath());
    }

    @Override
    public ImageU16 loadPlaneU16(int plane) throws IOException {
        byte[] planeBytes;
        try {
            planeBytes = reader.openPlane(0, plane).getBytes();
        } catch (FormatException e) {
            throw new IOException("Unable to open plane with the reader. " + e.getMessage());
        }
        final int[] data = TypeConverter.unsignedShortBytesToIntArray(planeBytes);
        return new ImageU16(inputFileInfo.getDimensions().toV2i(), data);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public int[] loadPlanesU16Data(int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneU16(planes[0]).getData();
        }

        final int planeValueCount = inputFileInfo.getDimensions().getX() * inputFileInfo.getDimensions().getY();
        final long planeDataSize = 2 * (long) planeValueCount;

        final long totalValueCount = (long) planeValueCount * planes.length;

        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Integer count is too big.");
        }

        int[] values = new int[(int) totalValueCount];
        Arrays.sort(planes);

        byte[] planeBytes;
        for (int i = 0; i < planes.length; i++) {
            final int plane = planes[i];

            try {
                planeBytes = reader.openPlane(0, plane).getBytes();
            } catch (FormatException e) {
                throw new IOException("Unable to open plane.");
            }
            if (planeBytes.length != planeDataSize) {
                throw new IOException("Bad byte count read from plane.");
            }

            TypeConverter.unsignedShortBytesToIntArray(planeBytes, values, (i * planeValueCount));
        }
        return values;
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        final V3i imageDims = inputFileInfo.getDimensions();
        final long planePixelCount = (long) imageDims.getX() * (long) imageDims.getY();
        final long dataSize = planePixelCount * (long) imageDims.getZ();

        if (dataSize > (long) Integer.MAX_VALUE) {
            throw new IOException("FileSize is too big.");
        }

        int[] values = new int[(int) dataSize];
        byte[] planeBytes;
        for (int plane = 0; plane < imageDims.getZ(); plane++) {
            try {
                planeBytes = reader.openPlane(0, plane).getBytes();
            } catch (FormatException e) {
                throw new IOException("Unable to open plane.");
            }
            if (planeBytes.length != 2 * planePixelCount) {
                throw new IOException("Bad byte count read from plane.");
            }
            TypeConverter.unsignedShortBytesToIntArray(planeBytes, values, (int) (plane * planePixelCount));
        }

        return values;
    }
}
