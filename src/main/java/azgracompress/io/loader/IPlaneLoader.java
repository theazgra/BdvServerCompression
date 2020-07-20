package azgracompress.io.loader;

import azgracompress.data.ImageU16;

import java.io.IOException;

public interface IPlaneLoader {
    ImageU16 loadPlaneU16(final int plane) throws IOException;

    int[] loadPlanesU16Data(int[] planes) throws IOException;

    int[] loadAllPlanesU16Data() throws IOException;
}
