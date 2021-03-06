package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.compression.CompressionOptions;
import cz.it4i.qcmp.compression.exception.ImageCompressionException;
import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;

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
     * Get data wrapping strategy configured for this loader.
     *
     * @return Wrapping strategy.
     */
    DataWrappingStrategy getWrappingStrategy();

    /**
     * Configure data wrapping strategy for this loader.
     *
     * @param strategy Wrapping strategy.
     */
    void setWrappingStrategy(DataWrappingStrategy strategy);

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
     * Load data of multiple specified planes. This functions exists, next to loadPlaneData, because some loaders
     * can take advantage in loading multiple planes in one call context.
     * <p>
     * Data are stored in 2D array.
     *
     * @param planes Zero based plane indices.
     * @return Planes data concatenated in single array.
     * @throws IOException when fails to load plane data.
     */
    default int[][] loadPlanesU16DataTo2dArray(final int[] planes) throws IOException {
        throw new IOException("Not Implemented in the current loader.");
    }

    /**
     * Load all planes data of the image dataset.
     *
     * @return Planes data concatenated in single array.
     * @throws IOException when fails to load plane data.
     */
    int[] loadAllPlanesU16Data() throws IOException;

    /**
     * Load row vectors from the entire dataset.
     *
     * @param vectorSize Width of the row vector.
     * @return Row vector data from the entire dataset.
     * @throws IOException When fails to load plane data.
     */
    default int[][] loadRowVectors(final int vectorSize) throws IOException {
        return loadRowVectors(vectorSize, new Range<>(0, getImageDimensions().getZ()));
    }

    /**
     * Load row vectors from specified plane range in the dataset.
     *
     * @param vectorSize Width of the row vector.
     * @param planeRange Source plane range.
     * @return Row vector data from the specified plane range.
     * @throws IOException When fails to load plane data.
     */
    int[][] loadRowVectors(final int vectorSize, final Range<Integer> planeRange) throws IOException;

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

    /**
     * Load correct type of vectors (quantization type in options) from specified plane range.
     *
     * @param planeRange Plane range to load vectors from.
     * @return Vector data from plane range.
     * @throws ImageCompressionException When fails to load plane range.
     */
    default int[][] loadVectorsFromPlaneRange(final CompressionOptions options,
                                              final Range<Integer> planeRange) throws ImageCompressionException {

        setWorkerCount(supportParallelLoading() ? options.getWorkerCount() : 1);

        try {
            switch (options.getQuantizationType()) {
                case Vector1D:
                    return loadRowVectors(options.getQuantizationVector().getX(), planeRange);
                case Vector2D:
                    return loadBlocks(options.getQuantizationVector().toV2i(), planeRange);
                case Vector3D:
                    return loadVoxels(options.getQuantizationVector(), planeRange);
                default: {
                    throw new ImageCompressionException("Invalid QuantizationType '" + options.getQuantizationType().toString() + "'");
                }
            }
        } catch (final IOException e) {
            throw new ImageCompressionException("Unable to load vectors QuantizationType=" + options.getQuantizationType(), e);
        }
    }
}
