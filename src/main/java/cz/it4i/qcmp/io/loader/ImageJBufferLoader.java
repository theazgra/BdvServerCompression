package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Block;
import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.BufferInputData;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.IOException;
import java.util.Arrays;

public final class ImageJBufferLoader extends GenericLoader implements IPlaneLoader {
    private final BufferInputData bufferInputData;

    public ImageJBufferLoader(final BufferInputData bufferDataInfo) {
        super(bufferDataInfo.getDimensions());
        this.bufferInputData = bufferDataInfo;
    }

    @Override
    public boolean supportParallelLoading() {
        return true;
    }

    private void copyShortArrayIntoBuffer(final short[] srcArray, final int[] destBuffer, final int destOffset, final int copyLen) {
        for (int i = 0; i < copyLen; i++) {
            destBuffer[destOffset + i] = TypeConverter.shortToInt(srcArray[i]);
        }
    }

    @Override
    public int[] loadPlaneData(final int plane) {
        final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(plane);
        return TypeConverter.shortArrayToIntArray(srcBuffer);
    }

    @Override
    protected int valueAt(final int plane, final int x, final int y, final int width) {
        return TypeConverter.shortToInt(((short[]) bufferInputData.getPixelBuffer(plane))[Block.index(x, y, width)]);
    }

    @Override
    public int[] loadPlanesU16Data(final int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneData(planes[0]);
        } else if (planes.length == dims.getPlaneCount()) {
            return loadAllPlanesU16Data();
        }
        final int planePixelCount = dims.getNumberOfElementsInDimension(2);
        final int totalValueCount = Math.multiplyExact(planePixelCount, planes.length);

        Arrays.sort(planes);

        final int[] destBuffer = new int[totalValueCount];
        int destOffset = 0;
        for (final int planeIndex : planes) {
            final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(planeIndex);
            copyShortArrayIntoBuffer(srcBuffer, destBuffer, destOffset, planePixelCount);
            destOffset += planePixelCount;
        }
        return destBuffer;
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        final int planePixelCount = dims.getNumberOfElementsInDimension(2);
        final int totalValueCount = dims.getNumberOfElementsInDimension(3);

        final int[] destBuffer = new int[totalValueCount];
        int destOffset = 0;
        for (int planeIndex = 0; planeIndex < dims.getPlaneCount(); planeIndex++) {
            final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(planeIndex);
            copyShortArrayIntoBuffer(srcBuffer, destBuffer, destOffset, planePixelCount);
            destOffset += planePixelCount;
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

