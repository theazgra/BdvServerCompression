package azgracompress.io;

import azgracompress.ScifioWrapper;
import azgracompress.cli.InputFileInfo;
import azgracompress.data.ImageU16;
import azgracompress.utilities.TypeConverter;
import io.scif.FormatException;
import io.scif.Reader;
import io.scif.jj2000.j2k.NotImplementedError;

import java.io.IOException;

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

    @Override
    public int[] loadPlanesU16Data(int[] planes) throws IOException {
        throw new NotImplementedError("NOT IMPLEMENTED");
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        throw new NotImplementedError("NOT IMPLEMENTED");
    }
}
