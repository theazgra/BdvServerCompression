package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.CallbackInputData;

import java.io.IOException;
import java.util.Arrays;

public class CallbackLoader extends BasicLoader implements IPlaneLoader {

    private final CallbackInputData callbackInputData;
    private final CallbackInputData.LoadCallback pixelLoad;

    public CallbackLoader(final CallbackInputData callbackInputData) {
        super(callbackInputData.getDimensions());
        this.callbackInputData = callbackInputData;
        this.pixelLoad = callbackInputData.getPixelLoadCallback();
    }

    @Override
    protected int valueAt(final int plane, final int x, final int y, final int width) {
        return pixelLoad.getValueAt(x, y, plane);
    }


    @Override
    public int[] loadPlaneData(final int plane) {
        final int planePixelCount = dims.getX() * dims.getY();
        final int[] planeData = new int[planePixelCount];

        int index = 0;
        for (int y = 0; y < dims.getY(); y++) {
            for (int x = 0; x < dims.getX(); x++) {
                planeData[index++] = pixelLoad.getValueAt(x, y, plane);
            }
        }
        return planeData;
    }

    @Override
    public int[] loadPlanesU16Data(final int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneData(planes[0]);
        } else if (planes.length == dims.getZ()) {
            return loadAllPlanesU16Data();
        }
        final int planePixelCount = dims.getX() * dims.getY();
        final long totalValueCount = (long) planePixelCount * (long) planes.length;
        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Unable to load image data for planes, file size is too big.");
        }

        Arrays.sort(planes);

        final int[] destBuffer = new int[(int) totalValueCount];
        int index = 0;
        for (final int plane : planes) {
            for (int y = 0; y < dims.getY(); y++) {
                for (int x = 0; x < dims.getX(); x++) {
                    destBuffer[index++] = pixelLoad.getValueAt(x, y, plane);
                }
            }
        }
        return destBuffer;
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        final long totalValueCount = dims.multiplyTogether();
        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Unable to load all image data, file size is too big.");
        }

        final int[] destBuffer = new int[(int) totalValueCount];
        int index = 0;
        for (int z = 0; z < dims.getZ(); z++) {
            for (int y = 0; y < dims.getY(); y++) {
                for (int x = 0; x < dims.getX(); x++) {
                    destBuffer[index++] = pixelLoad.getValueAt(x, y, z);
                }
            }
        }

        return destBuffer;
    }

    @Override
    public int[][] loadRowVectors(final int vectorSize, final Range<Integer> planeRange) {
        return loadRowVectorsImplByValueAt(vectorSize, planeRange);
    }

    @Override
    public int[][] loadBlocks(final V2i blockDim, final Range<Integer> planeRange) {
        return loadBlocksImplByValueAt(blockDim, planeRange);
    }

    @Override
    public int[][] loadVoxels(final V3i voxelDim, final Range<Integer> planeRange) {
        return loadVoxelsImplByValueAt(voxelDim, planeRange);
    }
}
