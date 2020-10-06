package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.*;

import java.io.IOException;

public abstract class BasicLoader {
    protected final V3i dims;
    protected int threadCount = 1;

    private DataWrappingStrategy wrappingStrategy = DataWrappingStrategy.MirroredRepeat;

    protected BasicLoader(final V3i datasetDims) {
        this.dims = datasetDims;
    }

    public V3i getImageDimensions() {
        return dims;
    }


    public DataWrappingStrategy getWrappingStrategy() {
        return wrappingStrategy;
    }

    public void setWrappingStrategy(final DataWrappingStrategy strategy) {
        wrappingStrategy = strategy;
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
     * Read value from plane at specified offset.
     *
     * @param plane  Zero based plane index.
     * @param offset Offset on flattened plane data.
     * @return Plane value at the index.
     */
    protected int valueAt(final int plane, final int offset) {
        new Exception().printStackTrace(System.err);
        assert (false) : "Unimplemented overload of BasicLoader::valueAt()";
        return Integer.MIN_VALUE;
    }

    /**
     * Wrap column (x) index based on specified wrapping strategy.
     *
     * @param x Column index.
     * @return Wrapped index.
     */
    private int wrapColumnIndex(final int x) {
        if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
            return dims.getX() - 1;
        } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
            return (dims.getX() - ((x - dims.getX()) + 1));
        }
        return x;
    }

    /**
     * Wrap row (y) index based on specified wrapping strategy.
     *
     * @param y Row index.
     * @return Wrapped index.
     */
    private int wrapRowIndex(final int y) {
        if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
            return dims.getY() - 1;
        } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
            return (dims.getY() - ((y - dims.getY()) + 1));
        }
        return y;
    }

    /**
     * Wrap plane (z) index based on specified wrapping strategy.
     *
     * @param z Plane index.
     * @return Wrapped index.
     */
    private int wrapPlaneIndex(final int z) {
        if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
            return dims.getZ() - 1;
        } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
            return (dims.getZ() - ((z - dims.getZ()) + 1));
        }
        return z;
    }

    protected int[][] loadRowVectorsImplByLoadPlaneData(final int vectorSize, final Range<Integer> planeRange) throws IOException {
        final int rowVectorCount = (int) Math.ceil((double) dims.getX() / (double) vectorSize);
        final int planeCount = planeRange.getTo() - planeRange.getFrom();
        final int vectorCount = planeCount * dims.getY() * rowVectorCount;
        final int[][] rowVectors = new int[vectorCount][vectorSize];

        int vectorIndex = 0;
        int baseX, srcX;

        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            final int[] planeData = loadPlaneData(plane);
            for (int row = 0; row < dims.getY(); row++) {
                for (int rowVectorIndex = 0; rowVectorIndex < rowVectorCount; rowVectorIndex++) {
                    // Copy single vector.
                    baseX = rowVectorIndex * vectorSize;

                    for (int vectorX = 0; vectorX < vectorSize; vectorX++) {
                        srcX = baseX + vectorX;
                        if (srcX >= dims.getX()) {
                            if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                                break;
                            srcX = wrapColumnIndex(srcX);
                        }
                        rowVectors[vectorIndex][vectorX] = planeData[Block.index(srcX, row, dims.getX())];
                    }
                    ++vectorIndex;
                }
            }
        }
        return rowVectors;
    }

    protected int[][] loadRowVectorsImplByValueAt(final int vectorSize, final Range<Integer> planeRange) {
        final int rowVectorCount = (int) Math.ceil((double) dims.getX() / (double) vectorSize);
        final int vectorCount = dims.getZ() * dims.getY() * rowVectorCount;

        final int[][] rowVectors = new int[vectorCount][vectorSize];

        int vectorIndex = 0;
        int baseX, srcX;

        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            for (int row = 0; row < dims.getY(); row++) {
                for (int rowVectorIndex = 0; rowVectorIndex < rowVectorCount; rowVectorIndex++) {
                    // Copy single vector.
                    baseX = rowVectorIndex * vectorSize;

                    for (int vectorX = 0; vectorX < vectorSize; vectorX++) {
                        srcX = (baseX + vectorX);
                        if (srcX >= dims.getX()) {
                            if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                                break;
                            srcX = wrapColumnIndex(srcX);
                        }
                        rowVectors[vectorIndex][vectorX] = valueAt(plane, Block.index(srcX, row, dims.getY()));
                    }
                    ++vectorIndex;
                }
            }
        }
        return rowVectors;
    }

    protected int[][] loadBlocksImplByLoadPlaneData(final V2i blockDim, final Range<Integer> planeRange) throws IOException {
        final int blockSize = blockDim.multiplyTogether();
        final int planeCount = planeRange.getTo() - planeRange.getFrom();
        final int blockCount = planeCount * Block.calculateRequiredChunkCount(dims.toV2i(), blockDim);

        final int[][] blocks = new int[blockCount][blockSize];

        int blockIndex = 0;
        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            final int[] planeData = loadPlaneData(plane);
            for (int blockYOffset = 0; blockYOffset < dims.getY(); blockYOffset += blockDim.getY()) {
                for (int blockXOffset = 0; blockXOffset < dims.getX(); blockXOffset += blockDim.getX()) {
                    loadBlock(blocks[blockIndex++], planeData, blockXOffset, blockYOffset, blockDim);
                }
            }
        }
        return blocks;
    }

    protected int[][] loadBlocksImplByValueAt(final V2i blockDim, final Range<Integer> planeRange) {
        final int blockSize = blockDim.multiplyTogether();
        final int planeCount = planeRange.getTo() - planeRange.getFrom();
        final int blockCount = planeCount * Block.calculateRequiredChunkCount(dims.toV2i(), blockDim);

        final int[][] blocks = new int[blockCount][blockSize];

        int blockIndex = 0;
        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            for (int blockYOffset = 0; blockYOffset < dims.getY(); blockYOffset += blockDim.getY()) {
                for (int blockXOffset = 0; blockXOffset < dims.getX(); blockXOffset += blockDim.getX()) {
                    loadBlock(blocks[blockIndex++], plane, blockXOffset, blockYOffset, blockDim);
                }
            }
        }
        return blocks;
    }

    private void loadBlock(final int[] block, final int planeIndex, final int blockXOffset, final int blockYOffset, final V2i blockDim) {
        int srcX, srcY;
        for (int y = 0; y < blockDim.getY(); y++) {
            srcY = blockYOffset + y;
            // Row overflow
            if (srcY >= dims.getY()) {
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    break;
                }
                srcY = wrapRowIndex(srcY);
            }

            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;

                // Column overflow.
                if (srcX >= dims.getX()) {
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcX = wrapColumnIndex(srcX);
                }
                block[Block.index(x, y, blockDim.getX())] = valueAt(planeIndex, Block.index(srcX, srcY, dims.getX()));
            }
        }
    }


    private void loadBlock(final int[] block, final int[] planeData, final int blockXOffset, final int blockYOffset, final V2i blockDim) {
        int srcX, srcY;
        for (int y = 0; y < blockDim.getY(); y++) {
            srcY = blockYOffset + y;
            // Row overflow
            if (srcY >= dims.getY()) {
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    break;
                }
                srcY = wrapRowIndex(srcY);
            }
            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;
                // Column overflow.
                if (srcX >= dims.getX()) {
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcX = wrapColumnIndex(srcX);
                }

                block[Block.index(x, y, blockDim.getX())] = planeData[Block.index(srcX, srcY, dims.getX())];
            }
        }
    }

    private void loadVoxel(final int[] voxel, final int voxelXOffset, final int voxelYOffset, final int voxelZOffset, final V3i voxelDim) {
        int srcX, srcY, srcZ;
        for (int z = 0; z < voxelDim.getZ(); z++) {
            srcZ = voxelZOffset + z;

            if (srcZ >= dims.getZ()) {
                // Handle plane overflow.
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    break;
                }
                srcZ = wrapPlaneIndex(srcZ);
            }

            for (int y = 0; y < voxelDim.getY(); y++) {
                srcY = voxelYOffset + y;

                if (srcY >= dims.getY()) {
                    // Handle row overflow.
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcY = wrapRowIndex(srcY);
                }

                for (int x = 0; x < voxelDim.getX(); x++) {
                    srcX = voxelXOffset + x;

                    if (srcX >= dims.getX()) {
                        // Handle column overflow.
                        if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                            break;
                        }
                        srcX = wrapColumnIndex(srcX);
                    }

                    voxel[Voxel.dataIndex(x, y, z, voxelDim)] = valueAt(srcZ, Block.index(srcX, srcY, dims.getX()));
                }
            }
        }
    }

    private void loadVoxel(final int[] voxel,
                           final int[][] planesData,
                           final int voxelXOffset,
                           final int voxelYOffset,
                           final V3i voxelDim) {
        int srcX, srcY;
        for (int z = 0; z < voxelDim.getZ(); z++) {
            // NOTE(Moravec): Plane overflow is handled by the caller which provides correct planesData.
            for (int y = 0; y < voxelDim.getY(); y++) {
                srcY = voxelYOffset + y;

                if (srcY >= dims.getY()) {
                    // Handle row overflow.
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcY = wrapRowIndex(srcY);
                }

                for (int x = 0; x < voxelDim.getX(); x++) {
                    srcX = voxelXOffset + x;

                    if (srcX >= dims.getX()) {
                        // Handle column overflow.
                        if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                            break;
                        }
                        srcX = wrapColumnIndex(srcX);
                    }

                    voxel[Voxel.dataIndex(x, y, z, voxelDim)] = planesData[z][Block.index(srcX, srcY, dims.getX())];
                }
            }
        }
    }

    /**
     * Allocate 2D voxel data array for specified voxel layer.
     *
     * @param voxelDim   Single voxel dimension.
     * @param planeRange Voxel layer depth.
     * @return Allocated array.
     */
    private int[][] allocateVoxelArray(final V3i voxelDim, final Range<Integer> planeRange) {
        final int voxelElementCount = (int) voxelDim.multiplyTogether();
        final int rangeSize = planeRange.getTo() - planeRange.getFrom();
        final V3i srcVoxel = new V3i(dims.getX(), dims.getY(), rangeSize);
        final int voxelCount = Voxel.calculateRequiredVoxelCount(srcVoxel, voxelDim);
        return new int[voxelCount][voxelElementCount];
    }

    /**
     * Load specified planes from dataset to voxel of specified dimensions.
     * This overload uses the valueAt function to read src data.
     *
     * @param voxelDim   Single voxel dimensions.
     * @param planeRange Range of planes to load voxels from.
     * @return Voxel data arranged in arrays.
     */
    protected int[][] loadVoxelsImplByValueAt(final V3i voxelDim, final Range<Integer> planeRange) {
        final int[][] voxels = allocateVoxelArray(voxelDim, planeRange);
        int voxelIndex = 0;

        for (int voxelZOffset = planeRange.getFrom(); voxelZOffset < planeRange.getTo(); voxelZOffset += voxelDim.getZ()) {
            for (int voxelYOffset = 0; voxelYOffset < dims.getY(); voxelYOffset += voxelDim.getY()) {
                for (int voxelXOffset = 0; voxelXOffset < dims.getX(); voxelXOffset += voxelDim.getX()) {
                    loadVoxel(voxels[voxelIndex++], voxelXOffset, voxelYOffset, voxelZOffset, voxelDim);
                }
            }
        }
        return voxels;
    }

    private void preloadPlanesData(final int[][] planesData, final int planeOffset, final int count) throws IOException {
        for (int i = 0; i < count; i++) {
            if (planeOffset + i < dims.getZ())
                planesData[i] = loadPlaneData(planeOffset + i);
            else {
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    planesData[i] = new int[dims.toV2i().multiplyTogether()];
                } else if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
                    assert (i > 0) : "Overflow on the first plane of voxel???";
                    planesData[i] = planesData[i - 1];
                } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
                    final int srcZ = dims.getZ() - (((planeOffset + i) - dims.getZ()) + 1);
                    planesData[i] = loadPlaneData(srcZ);
                }
            }
        }
    }

    /**
     * Load specified planes from dataset to voxel of specified dimensions.
     * This overload uses the loadPlaneData function to read src data.
     *
     * @param voxelDim   Single voxel dimensions.
     * @param planeRange Range of planes to load voxels from.
     * @return Voxel data arranged in arrays.
     */
    protected int[][] loadVoxelsImplByLoadPlaneData(final V3i voxelDim, final Range<Integer> planeRange) throws IOException {
        final int[][] voxels = allocateVoxelArray(voxelDim, planeRange);
        int voxelIndex = 0;

        final int[][] planesData = new int[voxelDim.getZ()][0];

        for (int voxelZOffset = planeRange.getFrom(); voxelZOffset < planeRange.getTo(); voxelZOffset += voxelDim.getZ()) {

            preloadPlanesData(planesData, voxelZOffset, voxelDim.getZ());

            for (int voxelYOffset = 0; voxelYOffset < dims.getY(); voxelYOffset += voxelDim.getY()) {
                for (int voxelXOffset = 0; voxelXOffset < dims.getX(); voxelXOffset += voxelDim.getX()) {
                    loadVoxel(voxels[voxelIndex++], planesData, voxelXOffset, voxelYOffset, voxelDim);
                }
            }
        }
        return voxels;
    }

    public void setWorkerCount(final int threadCount) {
        this.threadCount = threadCount;
    }

}
