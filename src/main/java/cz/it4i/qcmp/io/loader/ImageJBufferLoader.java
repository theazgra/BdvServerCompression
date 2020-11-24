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

    private int getBufferIndex(final int timepoint, final int plane) {
        return (timepoint * bufferInputData.getDimensions().getPlaneCount()) + plane;
    }

    @Override
    public int[] loadPlaneData(final int timepoint, final int plane) {
        final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(getBufferIndex(timepoint, plane));
        return TypeConverter.shortArrayToIntArray(srcBuffer);
    }

    @Override
    protected int valueAt(final int timepoint, final int plane, final int x, final int y, final int sourceWidth) {
        return TypeConverter.shortToInt(
                ((short[]) bufferInputData.getPixelBuffer(getBufferIndex(timepoint, plane)))[Block.index(x, y, sourceWidth)]);
    }

    @Override
    public int[] loadPlanesU16Data(final int timepoint, final int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneData(timepoint, planes[0]);
        } else if (planes.length == dims.getPlaneCount()) {
            return loadAllPlanesU16Data(timepoint);
        }
        final int planePixelCount = dims.getNumberOfElementsInDimension(2);
        final int totalValueCount = Math.multiplyExact(planePixelCount, planes.length);

        Arrays.sort(planes);

        final int[] destBuffer = new int[totalValueCount];
        int destOffset = 0;
        for (final int planeIndex : planes) {
            final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(getBufferIndex(timepoint, planeIndex));
            copyShortArrayIntoBuffer(srcBuffer, destBuffer, destOffset, planePixelCount);
            destOffset += planePixelCount;
        }
        return destBuffer;
    }

    @Override
    public int[] loadAllPlanesU16Data(final int timepoint) throws IOException {
        final int planePixelCount = dims.getNumberOfElementsInDimension(2);
        final int totalValueCount = dims.getNumberOfElementsInDimension(3);

        final int[] destBuffer = new int[totalValueCount];
        int destOffset = 0;
        for (int planeIndex = 0; planeIndex < dims.getPlaneCount(); planeIndex++) {
            final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(getBufferIndex(timepoint, planeIndex));
            copyShortArrayIntoBuffer(srcBuffer, destBuffer, destOffset, planePixelCount);
            destOffset += planePixelCount;
        }
        return destBuffer;
    }

    @Override
    public int[][] loadRowVectors(final int timepoint, final int vectorSize, final Range<Integer> planeRange) {
        return loadRowVectorsImplByValueAt(timepoint, vectorSize, planeRange);
    }

    @Override
    public int[][] loadBlocks(final int timepoint, final V2i blockDim, final Range<Integer> planeRange) {
        return loadBlocksImplByValueAt(timepoint, blockDim, planeRange);
    }

    @Override
    public int[][] loadVoxels(final int timepoint, final V3i voxelDim, final Range<Integer> planeRange) {
        return loadVoxelsImplByValueAt(timepoint, voxelDim, planeRange);
    }
}

