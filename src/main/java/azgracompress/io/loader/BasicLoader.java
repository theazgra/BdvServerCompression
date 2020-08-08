package azgracompress.io.loader;

import azgracompress.data.Range;
import azgracompress.data.V3i;
import azgracompress.data.Voxel;

import java.io.IOException;

public abstract class BasicLoader {
    protected final V3i dims;
    protected int threadCount = 1;

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

    protected int valueAt(final int plane, final int offset) {
        assert (false) : "Unimplemented overload of BasicLoader::valueAt()";
        return Integer.MIN_VALUE;
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
