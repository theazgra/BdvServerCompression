package azgracompress.io.loader;

import azgracompress.data.V3i;

import java.io.IOException;

public abstract class BasicLoader {
    protected final V3i dims;

    protected BasicLoader(final V3i datasetDims) {
        this.dims = datasetDims;
    }

    /**
     * Abstract method to load specified plane data.
     *
     * @param plane Zero based plane index.
     * @return Plane data.
     * @throws IOException when fails to load plane data for some reason.
     */
    public abstract int[] loadPlaneData(final int plane) throws IOException;

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

    /**
     * Load dataset into voxel data.
     *
     * @param voxelDim Single voxel dimensions.
     * @return Voxel data arranged in arrays.
     * @throws IOException When fails to load plane data.
     */
    protected int[][] loadVoxelsImplGray16(final V3i voxelDim) throws IOException {
        final int xVoxelCount = (int) Math.ceil((double) dims.getX() / (double) voxelDim.getX());
        final int yVoxelCount = (int) Math.ceil((double) dims.getY() / (double) voxelDim.getY());


        int[][] voxels = new int[calculateRequiredVoxelCount(dims, voxelDim)][(int) voxelDim.multiplyTogether()];


        int dstZ, dstY, dstX, voxelX, voxelY, voxelZ, voxelIndex;

        for (int srcZ = 0; srcZ < dims.getZ(); srcZ++) {

            final int[] srcPlaneBuffer = loadPlaneData(srcZ);

            dstZ = srcZ / voxelDim.getZ();
            voxelZ = srcZ - (dstZ * voxelDim.getZ());

            for (int srcY = 0; srcY < dims.getY(); srcY++) {
                dstY = srcY / voxelDim.getY();
                voxelY = srcY - (dstY * voxelDim.getY());

                for (int srcX = 0; srcX < dims.getX(); srcX++) {
                    dstX = srcX / voxelDim.getX();
                    voxelX = srcX - (dstX * voxelDim.getX());
                    voxelIndex = (dstZ * (xVoxelCount * yVoxelCount)) + (dstY * xVoxelCount) + dstX;

                    voxels[voxelIndex][voxelDataIndex(voxelX, voxelY, voxelZ, voxelDim)] = srcPlaneBuffer[(srcY * dims.getX()) + srcX];
                }
            }
        }
        return voxels;
    }

}
