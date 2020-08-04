package azgracompress.io.loader;

import azgracompress.data.V3i;

import java.io.IOException;

public interface IPlaneLoader {
    int[] loadPlaneData(final int plane) throws IOException;

    int[] loadPlanesU16Data(int[] planes) throws IOException;

    int[] loadAllPlanesU16Data() throws IOException;

    int[][] loadVoxels(final V3i voxelDim) throws IOException;
}
