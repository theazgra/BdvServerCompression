package azgracompress.io.loader;

import azgracompress.data.V3i;
import azgracompress.data.Voxel;

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

    protected abstract int valueAt(final int plane, final int offset);


    /**
     * Load dataset into voxel data.
     *
     * @param voxelDim Single voxel dimensions.
     * @return Voxel data arranged in arrays.
     * @throws IOException When fails to load plane data.
     */
    protected int[][] loadVoxelsImplByLoadPlaneData(final V3i voxelDim) throws IOException {
        final Voxel voxel = new Voxel(voxelDim);
        final int xVoxelCount = (int) Math.ceil((double) dims.getX() / (double) voxelDim.getX());
        final int yVoxelCount = (int) Math.ceil((double) dims.getY() / (double) voxelDim.getY());


        int[][] voxels = new int[Voxel.calculateRequiredVoxelCount(dims, voxelDim)][(int) voxelDim.multiplyTogether()];

        final int workerCount = 4;
        final int workSize = dims.getZ() / workerCount;
        Thread[] workers = new Thread[workerCount];

        final int dimX = dims.getX();
        final int dimY = dims.getY();
        final int voxelDimX = voxelDim.getX();
        final int voxelDimY = voxelDim.getY();
        final int voxelDimZ = voxelDim.getZ();

        for (int wId = 0; wId < workerCount; wId++) {
            final int fromZ = wId * workSize;
            final int toZ = (wId == workerCount - 1) ? dims.getZ() : (workSize + (wId * workSize));
            System.out.printf("%d-%d\n", fromZ, toZ);
            workers[wId] = new Thread(() -> {
                int dstZ, dstY, dstX, voxelX, voxelY, voxelZ, voxelIndex;
                int[] planeData;


                for (int srcZ = fromZ; srcZ < toZ; srcZ++) {
                    try {
                        planeData = loadPlaneData(srcZ);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    dstZ = srcZ / voxelDimZ;
                    voxelZ = srcZ - (dstZ * voxelDimZ);


                    for (int srcY = 0; srcY < dimY; srcY++) {
                        dstY = srcY / voxelDimY;
                        voxelY = srcY - (dstY * voxelDimY);

                        for (int srcX = 0; srcX < dimX; srcX++) {
                            dstX = srcX / voxelDimX;
                            voxelX = srcX - (dstX * voxelDimX);
                            voxelIndex = (dstZ * (xVoxelCount * yVoxelCount)) + (dstY * xVoxelCount) + dstX;

                            voxels[voxelIndex][voxel.dataIndex(voxelX, voxelY, voxelZ, voxelDim)] = planeData[(srcY * dimX) + srcX];
                        }
                    }
                }
            });
            workers[wId].start();
        }
        try {
            for (int wId = 0; wId < workerCount; wId++) {
                workers[wId].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return voxels;
    }

    /**
     * Load dataset into voxel data.
     *
     * @param voxelDim Single voxel dimensions.
     * @return Voxel data arranged in arrays.
     * @throws IOException When fails to load plane data.
     */
    protected int[][] loadVoxelsImplByValueAt(final V3i voxelDim) throws IOException {
        final Voxel voxel = new Voxel(voxelDim);
        final int xVoxelCount = (int) Math.ceil((double) dims.getX() / (double) voxelDim.getX());
        final int yVoxelCount = (int) Math.ceil((double) dims.getY() / (double) voxelDim.getY());


        int[][] voxels = new int[Voxel.calculateRequiredVoxelCount(dims, voxelDim)][(int) voxelDim.multiplyTogether()];

        final int workerCount = 4;
        final int workSize = dims.getZ() / workerCount;
        Thread[] workers = new Thread[workerCount];

        final int dimX = dims.getX();
        final int dimY = dims.getY();
        final int voxelDimX = voxelDim.getX();
        final int voxelDimY = voxelDim.getY();
        final int voxelDimZ = voxelDim.getZ();

        // TODO(Moravec): Try to simplify the math inside, which is slowing the process.

        for (int wId = 0; wId < workerCount; wId++) {
            final int fromZ = wId * workSize;
            final int toZ = (wId == workerCount - 1) ? dims.getZ() : (workSize + (wId * workSize));
//            System.out.printf("%d-%d\n", fromZ, toZ);
            workers[wId] = new Thread(() -> {
                int dstZ, dstY, dstX, voxelX, voxelY, voxelZ, voxelIndex;

                for (int srcZ = fromZ; srcZ < toZ; srcZ++) {
                    dstZ = srcZ / voxelDimZ;
                    voxelZ = srcZ - (dstZ * voxelDimZ);

                    for (int srcY = 0; srcY < dimY; srcY++) {
                        dstY = srcY / voxelDimY;
                        voxelY = srcY - (dstY * voxelDimY);

                        for (int srcX = 0; srcX < dimX; srcX++) {
                            dstX = srcX / voxelDimX;
                            voxelX = srcX - (dstX * voxelDimX);
                            voxelIndex = (dstZ * (xVoxelCount * yVoxelCount)) + (dstY * xVoxelCount) + dstX;

                            voxels[voxelIndex][voxel.dataIndex(voxelX, voxelY, voxelZ, voxelDim)] = valueAt(srcZ, (srcY * dimX) + srcX);
                        }
                    }
                }
            });
            workers[wId].start();
        }
        try {
            for (int wId = 0; wId < workerCount; wId++) {
                workers[wId].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return voxels;
    }

}
