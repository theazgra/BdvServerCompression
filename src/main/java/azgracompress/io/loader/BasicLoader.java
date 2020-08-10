package azgracompress.io.loader;

import azgracompress.data.*;

import java.io.IOException;

public abstract class BasicLoader {
    protected final V3i dims;
    protected int threadCount = 1;

    protected BasicLoader(final V3i datasetDims) {
        this.dims = datasetDims;
    }

    public V3i getImageDimensions() {
        return dims;
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
        final int vectorCount = dims.getZ() * dims.getY() * rowVectorCount;

        int[][] rowVectors = new int[vectorCount][vectorSize];

        int vectorIndex = 0;
        int baseX;

        for (int plane = planeRange.getFrom(); plane < planeRange.getTo(); plane++) {
            final int[] planeData = loadPlaneData(plane);
            for (int row = 0; row < dims.getY(); row++) {
                for (int rowVectorIndex = 0; rowVectorIndex < rowVectorCount; rowVectorIndex++) {
                    // Copy single vector.
                    baseX = rowVectorIndex * vectorSize;

                    for (int vectorX = 0; vectorX < vectorSize; vectorX++) {
                        if (baseX + vectorX >= dims.getY())
                            break;
                        rowVectors[vectorIndex][vectorX] = planeData[Chunk2D.index((baseX + vectorX), row, dims.getY())];
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

        int[][] rowVectors = new int[vectorCount][vectorSize];

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
                        rowVectors[vectorIndex][vectorX] = valueAt(plane, Chunk2D.index((baseX + vectorX), row, dims.getY()));
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
        final int blockCount = planeCount * Chunk2D.calculateRequiredChunkCount(dims.toV2i(), blockDim);

        int[][] blocks = new int[blockCount][blockSize];

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
        final int blockCount = planeCount * Chunk2D.calculateRequiredChunkCount(dims.toV2i(), blockDim);

        int[][] blocks = new int[blockCount][blockSize];

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
            if (srcY >= dims.getY())
                break;
            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;
                if (srcX >= dims.getX())
                    break;
                block[Chunk2D.index(x, y, blockDim.getY())] = valueAt(planeIndex, Chunk2D.index(srcX, srcY, dims.getY()));
            }
        }
    }

    private void loadBlock(final int[] block, final int[] planeData, final int blockXOffset, final int blockYOffset, final V2i blockDim) {
        int srcX, srcY;
        for (int y = 0; y < blockDim.getY(); y++) {
            srcY = blockYOffset + y;
            if (srcY >= dims.getY())
                break;
            for (int x = 0; x < blockDim.getX(); x++) {
                srcX = blockXOffset + x;
                if (srcX >= dims.getX())
                    break;

                block[Chunk2D.index(x, y, blockDim.getY())] = planeData[Chunk2D.index(srcX, srcY, dims.getY())];
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
        final Voxel dstVoxel = new Voxel(voxelDim);
        final int rangeSize = planeRange.getTo() - planeRange.getFrom();
        final V3i srcVoxel = new V3i(dims.getX(), dims.getY(), rangeSize);
        final int xVoxelCount = (int) Math.ceil((double) dims.getX() / (double) voxelDim.getX());
        final int yVoxelCount = (int) Math.ceil((double) dims.getY() / (double) voxelDim.getY());

        final int voxelIndexOffset = -((planeRange.getFrom() / voxelDim.getZ()) * (xVoxelCount * yVoxelCount));
        int[][] voxels = new int[Voxel.calculateRequiredVoxelCount(srcVoxel, voxelDim)][(int) voxelDim.multiplyTogether()];

        final int dimX = dims.getX();
        final int dimY = dims.getY();
        final int dimZ = planeRange.getTo();
        final int voxelDimX = voxelDim.getX();
        final int voxelDimY = voxelDim.getY();
        final int voxelDimZ = voxelDim.getZ();

        int dstZ, dstY, dstX, voxelX, voxelY, voxelZ, voxelIndex;
        int[] planeData;

        for (int srcZ = planeRange.getFrom(); srcZ < dimZ; srcZ++) {
            planeData = loadPlaneData(srcZ);
            dstZ = srcZ / voxelDimZ;
            voxelZ = srcZ - (dstZ * voxelDimZ);


            for (int srcY = 0; srcY < dimY; srcY++) {
                dstY = srcY / voxelDimY;
                voxelY = srcY - (dstY * voxelDimY);

                for (int srcX = 0; srcX < dimX; srcX++) {
                    dstX = srcX / voxelDimX;
                    voxelX = srcX - (dstX * voxelDimX);
                    voxelIndex = voxelIndexOffset + ((dstZ * (xVoxelCount * yVoxelCount)) + (dstY * xVoxelCount) + dstX);

                    voxels[voxelIndex][dstVoxel.dataIndex(voxelX, voxelY, voxelZ, voxelDim)] = planeData[(srcY * dimX) + srcX];
                }
            }
        }

        return voxels;
    }

    /**
     * Load specified planes from dataset to voxel of specified dimensions.
     * This overload uses the valueAt function to read src data.
     *
     * @param voxelDim   Single voxel dimensions.
     * @param planeRange Range of planes to load voxels from.
     * @return Voxel data arranged in arrays.
     * @throws IOException When fails to load plane data.
     */
    protected int[][] loadVoxelsImplByValueAt(final V3i voxelDim,
                                              final Range<Integer> planeRange) throws IOException {

        // TODO(Moravec): Improve performance of loading.
        final Voxel dstVoxel = new Voxel(voxelDim);
        final int rangeSize = planeRange.getTo() - planeRange.getFrom();
        final V3i srcVoxel = new V3i(dims.getX(), dims.getY(), rangeSize);
        final int xVoxelCount = (int) Math.ceil((double) dims.getX() / (double) voxelDim.getX());
        final int yVoxelCount = (int) Math.ceil((double) dims.getY() / (double) voxelDim.getY());

        // NOTE(Moravec):   We need voxelIndexOffset in case that planeRange is not the whole dataset.
        //                  voxelIndex which is calculated inside the loop doesn't know that we are loading
        //                  only some voxel layer. So we need to set the offset and start filling voxel data from the start.
        final int voxelIndexOffset = -((planeRange.getFrom() / voxelDim.getZ()) * (xVoxelCount * yVoxelCount));
        int[][] voxels = new int[Voxel.calculateRequiredVoxelCount(srcVoxel, voxelDim)][(int) voxelDim.multiplyTogether()];

        final int workSize = rangeSize / threadCount;
        final Thread[] threads = new Thread[threadCount];

        for (int wId = 0; wId < threadCount; wId++) {
            final int fromZ = wId * workSize;
            final int toZ = (wId == threadCount - 1) ? rangeSize : (workSize + (wId * workSize));

            threads[wId] = new Thread(() -> {
                final int dimX = dims.getX();
                final int dimY = dims.getY();
                final int zBase = planeRange.getFrom();
                final int voxelDimX = voxelDim.getX();
                final int voxelDimY = voxelDim.getY();
                final int voxelDimZ = voxelDim.getZ();

                int srcZ, dstZ, dstY, dstX, voxelX, voxelY, voxelZ, voxelIndex;

                for (int zOffset = fromZ; zOffset < toZ; zOffset++) {
                    srcZ = zBase + zOffset;
                    dstZ = srcZ / voxelDimZ;
                    voxelZ = srcZ - (dstZ * voxelDimZ);

                    for (int srcY = 0; srcY < dimY; srcY++) {
                        dstY = srcY / voxelDimY;
                        voxelY = srcY - (dstY * voxelDimY);

                        for (int srcX = 0; srcX < dimX; srcX++) {
                            dstX = srcX / voxelDimX;
                            voxelX = srcX - (dstX * voxelDimX);
                            voxelIndex = voxelIndexOffset + ((dstZ * (xVoxelCount * yVoxelCount)) + (dstY * xVoxelCount) + dstX);
                            voxels[voxelIndex][dstVoxel.dataIndex(voxelX, voxelY, voxelZ, voxelDim)] = valueAt(srcZ, (srcY * dimX) + srcX);

                        }
                    }
                }
            });
            threads[wId].start();
        }
        try {
            for (int wId = 0; wId < threadCount; wId++) {
                threads[wId].join();
            }
        } catch (InterruptedException e) {
            throw new IOException("threads[wId].join() failed.", e);
        }

        return voxels;
    }

    public void setWorkerCount(final int threadCount) {
        this.threadCount = threadCount;
    }

}
