package azgracompress.io.loader;

import azgracompress.data.*;

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

    protected int valueAt(final int plane, final int offset) {
        new Exception().printStackTrace(System.err);
        assert (false) : "Unimplemented overload of BasicLoader::valueAt()";
        return Integer.MIN_VALUE;
    }

    protected int[][] loadRowVectorsImplByLoadPlaneData(final int vectorSize, final Range<Integer> planeRange) throws IOException {
        final int rowVectorCount = (int) Math.ceil((double) dims.getX() / (double) vectorSize);
        final int planeCount = planeRange.getTo() - planeRange.getFrom();
        final int vectorCount = planeCount * dims.getY() * rowVectorCount;
        final int[][] rowVectors = new int[vectorCount][vectorSize];

        int vectorIndex = 0;
        int baseX;

        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            final int[] planeData = loadPlaneData(plane);
            for (int row = 0; row < dims.getY(); row++) {
                for (int rowVectorIndex = 0; rowVectorIndex < rowVectorCount; rowVectorIndex++) {
                    // Copy single vector.
                    baseX = rowVectorIndex * vectorSize;

                    for (int vectorX = 0; vectorX < vectorSize; vectorX++) {
                        if ((baseX + vectorX) >= dims.getY())
                            break;
                        rowVectors[vectorIndex][vectorX] = planeData[Block.index((baseX + vectorX), row, dims.getX())];
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
        int baseX;

        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            for (int row = 0; row < dims.getY(); row++) {
                for (int rowVectorIndex = 0; rowVectorIndex < rowVectorCount; rowVectorIndex++) {
                    // Copy single vector.
                    baseX = rowVectorIndex * vectorSize;

                    for (int vectorX = 0; vectorX < vectorSize; vectorX++) {
                        if (baseX + vectorX >= dims.getY())
                            break;
                        rowVectors[vectorIndex][vectorX] = valueAt(plane, Block.index((baseX + vectorX), row, dims.getY()));
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

                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                    break;

                if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
                    final int srcRow = dims.getY() - 1;
                    final int dstOffset = y * blockDim.getX();
                    for (int x = 0; x < blockDim.getX(); x++) {
                        srcX = (blockXOffset + x);
                        if (srcX >= dims.getX())
                            srcX = dims.getX() - 1;
                        block[dstOffset + x] = valueAt(planeIndex, Block.index(srcX, srcRow, dims.getX()));
                    }
                    continue;
                } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
                    final int srcRow = dims.getY() - ((srcY - dims.getY()) + 1);
                    final int dstOffset = y * blockDim.getX();
                    for (int x = 0; x < blockDim.getX(); x++) {
                        srcX = (blockXOffset + x);
                        if (srcX >= dims.getX())
                            srcX = dims.getX() - 1;
                        block[dstOffset + x] = valueAt(planeIndex, Block.index(srcX, srcRow, dims.getX()));
                    }
                    continue;
                }
            }

            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;

                // Column overflow.
                if (srcX >= dims.getX()) {

                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                        break;
                    if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
                        block[Block.index(x, y, blockDim.getX())] = valueAt(planeIndex, Block.index(dims.getX() - 1, srcY, dims.getX()));
                        continue;
                    } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {

                        block[Block.index(x, y, blockDim.getX())] =
                                valueAt(planeIndex, Block.index(dims.getX() - ((srcX - dims.getX()) + 1), srcY, dims.getX()));
                        continue;
                    }
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

                if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                    break;

                if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
                    final int srcRow = dims.getY() - 1;
                    final int dstOffset = y * blockDim.getX();
                    for (int x = 0; x < blockDim.getX(); x++) {
                        srcX = (blockXOffset + x);
                        if (srcX >= dims.getX())
                            srcX = dims.getX() - 1;
                        block[dstOffset + x] = planeData[Block.index(srcX, srcRow, dims.getX())];
                    }
                    continue;
                } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {
                    final int srcRow = dims.getY() - ((srcY - dims.getY()) + 1);
                    final int dstOffset = y * blockDim.getX();
                    for (int x = 0; x < blockDim.getX(); x++) {
                        srcX = (blockXOffset + x);
                        if (srcX >= dims.getX())
                            srcX = dims.getX() - 1;
                        block[dstOffset + x] = planeData[Block.index(srcX, srcRow, dims.getX())];
                    }
                    continue;
                }
            }
            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;
                // Column overflow.
                if (srcX >= dims.getX()) {

                    if (wrappingStrategy == DataWrappingStrategy.LeaveBlank)
                        break;
                    if (wrappingStrategy == DataWrappingStrategy.ClampToEdge) {
                        block[Block.index(x, y, blockDim.getX())] = planeData[Block.index(dims.getX() - 1, srcY, dims.getX())];
                        continue;
                    } else if (wrappingStrategy == DataWrappingStrategy.MirroredRepeat) {

                        block[Block.index(x, y, blockDim.getX())] =
                                planeData[Block.index(dims.getX() - ((srcX - dims.getX()) + 1), srcY, dims.getX())];
                        continue;
                    }
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
                break;
            }
            for (int y = 0; y < voxelDim.getY(); y++) {
                srcY = voxelYOffset + y;
                if (srcY >= dims.getY()) {
                    // Handle row overflow.
                    break;
                }
                for (int x = 0; x < voxelDim.getX(); x++) {
                    srcX = voxelXOffset + x;
                    if (srcX >= dims.getX()) {
                        // Handle column overflow
                        break;
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
            for (int y = 0; y < voxelDim.getY(); y++) {
                srcY = voxelYOffset + y;
                if (srcY >= dims.getY()) {
                    // Handle row overflow.
                    break;
                }
                for (int x = 0; x < voxelDim.getX(); x++) {
                    srcX = voxelXOffset + x;
                    if (srcX >= dims.getX()) {
                        // Handle column overflow
                        break;
                    }

                    voxel[Voxel.dataIndex(x, y, z, voxelDim)] = planesData[z][Block.index(srcX, srcY, dims.getX())];
                }
            }
        }
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
        final int voxelElementCount = (int) voxelDim.multiplyTogether();
        final int rangeSize = planeRange.getTo() - planeRange.getFrom();
        final V3i srcVoxel = new V3i(dims.getX(), dims.getY(), rangeSize);
        final int voxelCount = Voxel.calculateRequiredVoxelCount(srcVoxel, voxelDim);
        final int[][] voxels = new int[voxelCount][voxelElementCount];
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
            else
                planesData[i] = new int[dims.toV2i().multiplyTogether()];
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
        final int voxelElementCount = (int) voxelDim.multiplyTogether();
        final int rangeSize = planeRange.getTo() - planeRange.getFrom();
        final V3i srcVoxel = new V3i(dims.getX(), dims.getY(), rangeSize);
        final int voxelCount = Voxel.calculateRequiredVoxelCount(srcVoxel, voxelDim);


        final int[][] voxels = new int[voxelCount][voxelElementCount];
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
