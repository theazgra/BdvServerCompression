package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.*;

import java.io.IOException;

abstract class GenericLoader {
    protected final HyperStackDimensions dims;
    protected int threadCount = 1;

    private DataWrappingStrategy wrappingStrategy = DataWrappingStrategy.MirroredRepeat;

    protected GenericLoader(final HyperStackDimensions datasetDims) {
        this.dims = datasetDims;
    }

    public HyperStackDimensions getDatasetDimensions() {
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

    protected abstract int valueAt(final int plane, final int x, final int y, final int width);

    /**
     * Wrap column (x) index based on specified wrapping strategy.
     *
     * @param x Column index.
     * @return Wrapped index.
     */
    private int wrapColumnIndex(final int x) {
        if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
            return dims.getWidth() - 1;
        } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
            return (dims.getWidth() - (1 + (x % dims.getWidth())));
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
            return dims.getHeight() - 1;
        } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
            return (dims.getHeight() - (1 + (y % dims.getHeight())));
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
            return dims.getPlaneCount() - 1;
        } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
            return (dims.getPlaneCount() - (1 + (z % dims.getPlaneCount())));
        }
        return z;
    }

    protected int[][] loadRowVectorsImplByLoadPlaneData(final int vectorSize, final Range<Integer> planeRange) throws IOException {
        final int rowVectorCount = (int) Math.ceil((double) dims.getWidth() / (double) vectorSize);
        final int planeCount = planeRange.getTo() - planeRange.getFrom();
        final int vectorCount = planeCount * dims.getHeight() * rowVectorCount;
        final int[][] rowVectors = new int[vectorCount][vectorSize];

        int vectorIndex = 0;
        int baseX, srcX;

        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            final int[] planeData = loadPlaneData(plane);
            for (int row = 0; row < dims.getHeight(); row++) {
                for (int rowVectorIndex = 0; rowVectorIndex < rowVectorCount; rowVectorIndex++) {
                    // Copy single vector.
                    baseX = rowVectorIndex * vectorSize;

                    for (int vectorX = 0; vectorX < vectorSize; vectorX++) {
                        srcX = baseX + vectorX;
                        if (srcX >= dims.getWidth()) {
                            if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                                break;
                            srcX = wrapColumnIndex(srcX);
                        }
                        rowVectors[vectorIndex][vectorX] = planeData[Block.index(srcX, row, dims.getWidth())];
                    }
                    ++vectorIndex;
                }
            }
        }
        return rowVectors;
    }

    protected int[][] loadRowVectorsImplByValueAt(final int vectorSize, final Range<Integer> planeRange) {
        final int rowVectorCount = (int) Math.ceil((double) dims.getWidth() / (double) vectorSize);
        final int vectorCount = dims.getPlaneCount() * dims.getHeight() * rowVectorCount;

        final int[][] rowVectors = new int[vectorCount][vectorSize];

        int vectorIndex = 0;
        int baseX, srcX;

        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            for (int row = 0; row < dims.getHeight(); row++) {
                for (int rowVectorIndex = 0; rowVectorIndex < rowVectorCount; rowVectorIndex++) {
                    // Copy single vector.
                    baseX = rowVectorIndex * vectorSize;

                    for (int vectorX = 0; vectorX < vectorSize; vectorX++) {
                        srcX = (baseX + vectorX);
                        if (srcX >= dims.getWidth()) {
                            if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                                break;
                            srcX = wrapColumnIndex(srcX);
                        }

                        // TODO(Moravec): dims.getHeight() should probably be dims.getWidth()! Check this!
                        rowVectors[vectorIndex][vectorX] = valueAt(plane, srcX, row, dims.getHeight());
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
        final int blockCount = planeCount * Block.calculateRequiredChunkCount(dims.getPlaneDimensions(), blockDim);

        final int[][] blocks = new int[blockCount][blockSize];

        int blockIndex = 0;
        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            final int[] planeData = loadPlaneData(plane);
            for (int blockYOffset = 0; blockYOffset < dims.getHeight(); blockYOffset += blockDim.getY()) {
                for (int blockXOffset = 0; blockXOffset < dims.getWidth(); blockXOffset += blockDim.getX()) {
                    loadBlock(blocks[blockIndex++], planeData, blockXOffset, blockYOffset, blockDim);
                }
            }
        }
        return blocks;
    }

    protected int[][] loadBlocksImplByValueAt(final V2i blockDim, final Range<Integer> planeRange) {
        final int blockSize = blockDim.multiplyTogether();
        final int planeCount = planeRange.getTo() - planeRange.getFrom();
        final int blockCount = planeCount * Block.calculateRequiredChunkCount(dims.getPlaneDimensions(), blockDim);

        final int[][] blocks = new int[blockCount][blockSize];

        int blockIndex = 0;
        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            for (int blockYOffset = 0; blockYOffset < dims.getHeight(); blockYOffset += blockDim.getY()) {
                for (int blockXOffset = 0; blockXOffset < dims.getWidth(); blockXOffset += blockDim.getX()) {
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
            if (srcY >= dims.getHeight()) {
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    break;
                }
                srcY = wrapRowIndex(srcY);
            }

            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;

                // Column overflow.
                if (srcX >= dims.getWidth()) {
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcX = wrapColumnIndex(srcX);
                }

                block[Block.index(x, y, blockDim.getX())] = valueAt(planeIndex, srcX, srcY, dims.getWidth());
            }
        }
    }

    private void loadBlock(final int[] block, final int[] planeData, final int blockXOffset, final int blockYOffset, final V2i blockDim) {
        int srcX, srcY;
        for (int y = 0; y < blockDim.getY(); y++) {
            srcY = blockYOffset + y;
            // Row overflow
            if (srcY >= dims.getHeight()) {
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    break;
                }
                srcY = wrapRowIndex(srcY);
            }
            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;
                // Column overflow.
                if (srcX >= dims.getWidth()) {
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcX = wrapColumnIndex(srcX);
                }

                block[Block.index(x, y, blockDim.getX())] = planeData[Block.index(srcX, srcY, dims.getWidth())];
            }
        }
    }

    private void loadVoxel(final int[] voxel, final int voxelXOffset, final int voxelYOffset, final int voxelZOffset, final V3i voxelDim) {
        int srcX, srcY, srcZ;
        for (int z = 0; z < voxelDim.getZ(); z++) {
            srcZ = voxelZOffset + z;

            if (srcZ >= dims.getPlaneCount()) {
                // Handle plane overflow.
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    break;
                }
                srcZ = wrapPlaneIndex(srcZ);
            }

            for (int y = 0; y < voxelDim.getY(); y++) {
                srcY = voxelYOffset + y;

                if (srcY >= dims.getHeight()) {
                    // Handle row overflow.
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcY = wrapRowIndex(srcY);
                }

                for (int x = 0; x < voxelDim.getX(); x++) {
                    srcX = voxelXOffset + x;

                    if (srcX >= dims.getWidth()) {
                        // Handle column overflow.
                        if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                            break;
                        }
                        srcX = wrapColumnIndex(srcX);
                    }

                    voxel[Voxel.dataIndex(x, y, z, voxelDim)] = valueAt(srcZ, srcX, srcY, dims.getWidth());
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

                if (srcY >= dims.getHeight()) {
                    // Handle row overflow.
                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                        break;
                    }
                    srcY = wrapRowIndex(srcY);
                }

                for (int x = 0; x < voxelDim.getX(); x++) {
                    srcX = voxelXOffset + x;

                    if (srcX >= dims.getWidth()) {
                        // Handle column overflow.
                        if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                            break;
                        }
                        srcX = wrapColumnIndex(srcX);
                    }

                    voxel[Voxel.dataIndex(x, y, z, voxelDim)] = planesData[z][Block.index(srcX, srcY, dims.getWidth())];
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
        final int voxelElementCount = voxelDim.multiplyTogether();
        final int rangeSize = planeRange.getTo() - planeRange.getFrom();
        final V3i srcVoxel = new V3i(dims.getWidth(), dims.getHeight(), rangeSize);
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
            for (int voxelYOffset = 0; voxelYOffset < dims.getHeight(); voxelYOffset += voxelDim.getY()) {
                for (int voxelXOffset = 0; voxelXOffset < dims.getWidth(); voxelXOffset += voxelDim.getX()) {
                    loadVoxel(voxels[voxelIndex++], voxelXOffset, voxelYOffset, voxelZOffset, voxelDim);
                }
            }
        }
        return voxels;
    }

    private void preloadPlanesData(final int[][] planesData, final int planeOffset, final int count) throws IOException {
        for (int i = 0; i < count; i++) {
            if (planeOffset + i < dims.getPlaneCount())
                planesData[i] = loadPlaneData(planeOffset + i);
            else {
                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank) {
                    planesData[i] = new int[dims.getWidth() * dims.getHeight()];
                } else if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
                    // assert (i > 0) : "Overflow on the first plane of voxel???";
                    planesData[i] = planesData[i - 1];
                } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
                    final int srcZ = dims.getPlaneCount() - (((planeOffset + i) - dims.getPlaneCount()) + 1);
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

            for (int voxelYOffset = 0; voxelYOffset < dims.getHeight(); voxelYOffset += voxelDim.getY()) {
                for (int voxelXOffset = 0; voxelXOffset < dims.getWidth(); voxelXOffset += voxelDim.getX()) {
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
