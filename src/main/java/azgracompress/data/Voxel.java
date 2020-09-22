package azgracompress.data;

@SuppressWarnings("DuplicatedCode")
public final class Voxel {
    /**
     * Voxel dimensions.
     */
    private final V3i dims;


    public Voxel(final V3i voxelDims) {
        dims = voxelDims;
    }

    /**
     * Check whether the coordinates are inside voxel of dimension `dims`.
     *
     * @param x Zero based x coordinate.
     * @param y Zero based y coordinate.
     * @param z Zero based z coordinate.
     * @return True if coordinates are inside this voxel.
     */
    public boolean isInsideVoxel(final int x, final int y, final int z) {
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
    public int dataIndex(final int x, final int y, final int z) {
        return dataIndex(x, y, z, dims);
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
    public static int dataIndex(final int x, final int y, final int z, final V3i voxelDims) {
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

    public void reconstructFromVoxels(final V3i voxelDims,
                                      final int[][] voxelData,
                                      final short[][] reconstructedData,
                                      final int planeIndexOffset) {

        final int xVoxelCount = (int) Math.ceil((double) dims.getX() / (double) voxelDims.getX());
        final int yVoxelCount = (int) Math.ceil((double) dims.getY() / (double) voxelDims.getY());
        final int planeVoxelCount = xVoxelCount * yVoxelCount;

        final int planeDimX = dims.getX();
        final int planeDimY = dims.getY();
        final int voxelDimX = voxelDims.getX();
        final int voxelDimY = voxelDims.getY();
        final int voxelDimZ = voxelDims.getZ();

        int voxelOffset = 0;
        for (int planeOffset = 0; planeOffset < dims.getZ(); planeOffset += voxelDimZ) {

            for (int voxelZ = 0; voxelZ < voxelDimZ; voxelZ++) {
                final int planeIndex = planeOffset + voxelZ;
                if (planeIndex >= dims.getZ())
                    break;


                for (int voxelIndex = 0; voxelIndex < planeVoxelCount; voxelIndex++) {
                    for (int voxelY = 0; voxelY < voxelDimY; voxelY++) {
                        final int dstY = ((voxelIndex / xVoxelCount) * voxelDimY) + voxelY;
                        if (dstY >= planeDimY) {
                            break;
                        }

                        for (int voxelX = 0; voxelX < voxelDimX; voxelX++) {
                            final int dstX = ((voxelIndex % xVoxelCount) * voxelDimX) + voxelX;
                            if (dstX >= planeDimX) {
                                break;
                            }


                            final int indexInsideVoxel = dataIndex(voxelX, voxelY, voxelZ, voxelDims);
                            final int indexInsidePlane = Block.index(dstX, dstY, planeDimX);

                            // reconstructedData are 2D data while voxelData are 3D data!
                            reconstructedData[planeIndexOffset + planeIndex][indexInsidePlane] =
                                    (short) voxelData[(voxelOffset + voxelIndex)][indexInsideVoxel];
                        }
                    }

                }
            }
            voxelOffset += planeVoxelCount;
        }

    }

    /**
     * Reconstruct an 3D dataset from voxels, which divided the original dataset.
     *
     * @param voxelDims Voxel dimensions.
     * @param voxelData Voxel data.
     * @return Dataset reconstructed from the voxel data.
     */
    public ImageU16Dataset reconstructFromVoxelsToDataset(final V3i voxelDims, final int[][] voxelData) {
        final short[][] reconstructedData = new short[dims.getZ()][dims.toV2i().multiplyTogether()];
        reconstructFromVoxels(voxelDims, voxelData, reconstructedData, 0);
        return new ImageU16Dataset(dims.toV2i(), dims.getZ(), reconstructedData);
    }

    public short[] reconstructFromVoxelsToVoxelArray(final V3i voxelDims, final int[][] voxelData) {
        final short[] reconstructedVoxel = new short[(int) dims.multiplyTogether()];

        final int xVoxelCount = (int) Math.ceil((double) dims.getX() / (double) voxelDims.getX());
        final int yVoxelCount = (int) Math.ceil((double) dims.getY() / (double) voxelDims.getY());
        final int planeVoxelCount = xVoxelCount * yVoxelCount;

        final int planeDimX = dims.getX();
        final int planeDimY = dims.getY();
        final int voxelDimX = voxelDims.getX();
        final int voxelDimY = voxelDims.getY();
        final int voxelDimZ = voxelDims.getZ();

        int voxelOffset = 0;
        for (int planeOffset = 0; planeOffset < dims.getZ(); planeOffset += voxelDimZ) {

            for (int voxelZ = 0; voxelZ < voxelDimZ; voxelZ++) {
                final int planeIndex = planeOffset + voxelZ;
                if (planeIndex >= dims.getZ())
                    break;

                for (int voxelIndex = 0; voxelIndex < planeVoxelCount; voxelIndex++) {
                    for (int voxelY = 0; voxelY < voxelDimY; voxelY++) {
                        final int dstY = ((voxelIndex / xVoxelCount) * voxelDimY) + voxelY;
                        if (dstY >= planeDimY) {
                            break;
                        }

                        for (int voxelX = 0; voxelX < voxelDimX; voxelX++) {
                            final int dstX = ((voxelIndex % xVoxelCount) * voxelDimX) + voxelX;
                            if (dstX >= planeDimX) {
                                break;
                            }

                            final int indexInsideVoxel = dataIndex(voxelX, voxelY, voxelZ, voxelDims);
                            final int dstIndex = dataIndex(dstX, dstY, planeIndex);
                            if (dstIndex >= reconstructedVoxel.length) {
                                System.out.printf("Fail\n");
                            }
                            reconstructedVoxel[dstIndex] =
                                    (short) voxelData[(voxelOffset + voxelIndex)][indexInsideVoxel];

                        }
                    }

                }
            }
            voxelOffset += planeVoxelCount;
        }
        return reconstructedVoxel;
    }

    public final V3i getDims() {
        return dims;
    }
}
