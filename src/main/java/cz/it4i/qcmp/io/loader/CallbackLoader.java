package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.CallbackInputData;

import java.io.IOException;
import java.util.Arrays;

public class CallbackLoader extends GenericLoader implements IPlaneLoader {

    private final CallbackInputData callbackInputData;
    private final CallbackInputData.LoadCallback pixelLoad;

    public CallbackLoader(final CallbackInputData callbackInputData) {
        super(callbackInputData.getDimensions());
        this.callbackInputData = callbackInputData;
        this.pixelLoad = callbackInputData.getPixelLoadCallback();
    }

    @Override
    protected int valueAt(final int timepoint, final int plane, final int x, final int y, final int width) {
        return pixelLoad.getValueAt(x, y, plane, timepoint);
    }


    @Override
    public int[] loadPlaneData(final int timepoint, final int plane) {
        final int planePixelCount = dims.getNumberOfElementsInDimension(2);
        final int[] planeData = new int[planePixelCount];

        int index = 0;
        for (int y = 0; y < dims.getHeight(); y++) {
            for (int x = 0; x < dims.getWidth(); x++) {
                planeData[index++] = pixelLoad.getValueAt(x, y, plane, timepoint);
            }
        }
        return planeData;
    }

    @Override
    public int[] loadPlanesU16Data(final int timepoint, final int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneData(0, planes[0]);
        } else if (planes.length == dims.getPlaneCount()) {
            return loadAllPlanesU16Data(0);
        }
        final int planePixelCount = dims.getNumberOfElementsInDimension(2);
        final long totalValueCount = (long) planePixelCount * (long) planes.length;
        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Unable to load image data for planes, file size is too big.");
        }

        Arrays.sort(planes);

        final int[] destBuffer = new int[(int) totalValueCount];
        int index = 0;
        for (final int plane : planes) {
            for (int y = 0; y < dims.getHeight(); y++) {
                for (int x = 0; x < dims.getWidth(); x++) {
                    destBuffer[index++] = pixelLoad.getValueAt(x, y, plane, timepoint);
                }
            }
        }
        return destBuffer;
    }

    @Override
    public int[] loadAllPlanesU16Data(final int timepoint) {
        final long totalValueCount = dims.getNumberOfElementsInDimension(3);

        final int[] destBuffer = new int[(int) totalValueCount];
        int index = 0;
        for (int z = 0; z < dims.getPlaneCount(); z++) {
            for (int y = 0; y < dims.getHeight(); y++) {
                for (int x = 0; x < dims.getWidth(); x++) {
                    destBuffer[index++] = pixelLoad.getValueAt(x, y, z, timepoint);
                }
            }
        }

        return destBuffer;
    }

    @Override
    public int[][] loadRowVectors(final int timepoint, final int vectorSize, final Range<Integer> planeRange) {
        return loadRowVectorsImplByValueAt(vectorSize, planeRange);
    }

    @Override
    public int[][] loadBlocks(final int timepoint, final V2i blockDim, final Range<Integer> planeRange) {
        return loadBlocksImplByValueAt(blockDim, planeRange);
    }

    @Override
    public int[][] loadVoxels(final int timepoint, final V3i voxelDim, final Range<Integer> planeRange) {
        return loadVoxelsImplByValueAt(voxelDim, planeRange);
    }
}
