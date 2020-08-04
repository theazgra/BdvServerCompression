package azgracompress.io.loader;

import azgracompress.data.V3i;

public abstract class BasicLoader {
    protected final V3i dims;

    protected BasicLoader(final V3i datasetDims) {
        this.dims = datasetDims;
    }

    /**
     * Check whether the coordinates are inside voxel of dimension `dims`.
     *
     * @param x Zero based x coordinate.
     * @param y Zero based y coordinate.
     * @param z Zero based z coordinate.
     * @return True if coordinates are inside this voxel.
     */
    protected boolean isInsideVoxel(final int x, final int y, final int z) {
        return (((x >= 0) && (x < dims.getX())) && (y >= 0) && (y < dims.getY()) && (z >= 0) && (z < dims.getZ()));
    }

    /**
     * Calculate the index inside data array for this voxel.
     *
     * @param x Zero based x coordinate.
     * @param y Zero based y coordinate.
     * @param z Zero based z coordinate.
     * @return Index inside data array.
     */
    protected int voxelDataIndex(final int x, final int y, final int z) {
        return voxelDataIndex(x, y, z, dims);
    }

    /**
     * Calculate the index inside data array for voxel of given dimensions.
     *
     * @param x         Zero based x coordinate.
     * @param y         Zero based y coordinate.
     * @param z         Zero based z coordinate.
     * @param voxelDims Chunk dimensions.
     * @return Index inside chunk dimension data array.
     */
    protected int voxelDataIndex(final int x, final int y, final int z, final V3i voxelDims) {
        assert (x >= 0 && x < voxelDims.getX()) : "Index X out of bounds.";
        assert (y >= 0 && y < voxelDims.getY()) : "Index Y out of bounds.";
        assert (z >= 0 && z < voxelDims.getZ()) : "Index Z out of bounds.";

        return (z * (voxelDims.getX() * voxelDims.getY())) + (y * voxelDims.getX()) + x;
    }

    /**
     * Calculate required voxel count, which are needed when dividing dataset to voxels of given size.
     *
     * @param datasetDims Dataset dimensions.
     * @param voxelDims   One voxel dimensions.
     * @return Number of voxels needed to divide the dataset.
     */
    public static int calculateRequiredVoxelCount(final V3i datasetDims, final V3i voxelDims) {
        final int xChunkCount = (int) Math.ceil((double) datasetDims.getX() / (double) voxelDims.getX());
        final int yChunkCount = (int) Math.ceil((double) datasetDims.getY() / (double) voxelDims.getY());
        final int zChunkCount = (int) Math.ceil((double) datasetDims.getZ() / (double) voxelDims.getZ());
        return (xChunkCount * yChunkCount * zChunkCount);
    }
}
