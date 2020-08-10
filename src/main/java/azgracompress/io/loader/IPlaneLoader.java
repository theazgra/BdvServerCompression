package azgracompress.io.loader;

import azgracompress.data.Range;
import azgracompress.data.V2i;
import azgracompress.data.V3i;

import java.io.IOException;

/**
 * Interface for dataset loaders.
 */
public interface IPlaneLoader {

    /**
     * Check whether current loader supports threading.
     *
     * @return True if current loader can use more threads for loading.
     */
    default boolean supportParallelLoading() {
        return false;
    }

    /**
     * Get dimensions of the image, for which the loader was created.
     *
     * @return Image of the loader image.
     */
    V3i getImageDimensions();

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
     * Load blocks from the entire dataset.
     *
     * @param blockDim Dimensions of the 2D block. (Matrix)
     * @return Block data from the entire dataset.
     * @throws IOException When fails to load plane data.
     */
    default int[][] loadBlocks(final V2i blockDim) throws IOException {
        return loadBlocks(blockDim, new Range<>(0, getImageDimensions().getZ()));
    }

    /**
     * Load blocks from specified plane range in the dataset.
     *
     * @param blockDim   Dimensions of the 2D block. (Matrix)
     * @param planeRange Source plane range.
     * @return Block data from the specified plane range.
     * @throws IOException When fails to load plane data.
     */
    int[][] loadBlocks(final V2i blockDim, final Range<Integer> planeRange) throws IOException;

    /**
     * Load voxels from entire dataset.
     *
     * @param voxelDim Voxel dimensions.
     * @return Voxel data from the entire dataset.
     * @throws IOException when fails to load plane data.
     */
    default int[][] loadVoxels(final V3i voxelDim) throws IOException {
        return loadVoxels(voxelDim, new Range<>(0, getImageDimensions().getZ()));
    }

    /**
     * Load voxels from specified plane range in the dataset.
     * Plane range should be divisible by `voxelDim.getZ()`
     *
     * @param voxelDim   Voxel dimensions.
     * @param planeRange Source plane range.
     * @return Voxel data from the specified plane range.
     * @throws IOException when fails to load plane data.
     */
    int[][] loadVoxels(final V3i voxelDim, final Range<Integer> planeRange) throws IOException;

    /**
     * Set thread count, which can be used by the loader if needed.
     *
     * @param threadCount Available thread count for loader.
     */
    void setWorkerCount(final int threadCount);
}
