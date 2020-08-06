package azgracompress.io.loader;

import azgracompress.data.V3i;

import java.io.IOException;

/**
 * Interface for dataset loaders.
 */
public interface IPlaneLoader {
    /**
     * Load specified plane data.
     *
     * @param plane Zero based plane index.
     * @return u16 plane data.
     * @throws IOException when fails to load plane data.
     */
    int[] loadPlaneData(final int plane) throws IOException;

    /**
     * Load data of multiple specified planes. This functions exists, next to loadPlaneData, because some loaders
     * can take advantage in loading multiple planes in one call context.
     *
     * @param planes Zero based plane indices.
     * @return Planes data concatenated in single array.
     * @throws IOException when fails to load plane data.
     */
    int[] loadPlanesU16Data(int[] planes) throws IOException;

    /**
     * Load all planes data of the image dataset.
     *
     * @return Planes data concatenated in single array.
     * @throws IOException when fails to load plane data.
     */
    int[] loadAllPlanesU16Data() throws IOException;

    /**
     * Load voxels from entire dataset.
     *
     * @param voxelDim Voxel dimensions.
     * @return Voxel data from the entire dataset.
     * @throws IOException when fails to load plane data.
     */
    int[][] loadVoxels(final V3i voxelDim) throws IOException;
}
