package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Block;
import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.BufferInputData;
import cz.it4i.qcmp.io.InputData;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.IOException;
import java.util.Arrays;

public final class ImageJBufferLoader extends BasicLoader implements IPlaneLoader {
    private final BufferInputData bufferInputData;

    public ImageJBufferLoader(final BufferInputData bufferDataInfo) {
        super(bufferDataInfo.getDimensions());
        this.bufferInputData = bufferDataInfo;
        // FIXME: Support more pixel types.
        assert (this.bufferInputData.getPixelType() == InputData.PixelType.Gray16);
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
    protected int valueAt(int plane, int x, int y, int width) {
        return TypeConverter.shortToInt(((short[]) bufferInputData.getPixelBuffer(plane))[Block.index(x, y, width)]);
    }

    @Override
    public int[] loadPlanesU16Data(final int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneData(planes[0]);
        } else if (planes.length == bufferInputData.getDimensions().getZ()) { // Maybe?
            return loadAllPlanesU16Data();
        }
        final int planePixelCount =
                bufferInputData.getDimensions().getX() * bufferInputData.getDimensions().getY();
        final long totalValueCount = (long) planePixelCount * (long) planes.length;

        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Unable to load image data for planes, file size is too big.");
        }

        Arrays.sort(planes);

        final int[] destBuffer = new int[(int) totalValueCount];
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
        final V3i imageDims = bufferInputData.getDimensions();
        final long totalValueCount = imageDims.multiplyTogether();
        final int planePixelCount = imageDims.getX() * imageDims.getY();

        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Unable to load all image data, file size is too big.");
        }

        final int[] destBuffer = new int[(int) totalValueCount];
        int destOffset = 0;
        for (int planeIndex = 0; planeIndex < imageDims.getZ(); planeIndex++) {
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

