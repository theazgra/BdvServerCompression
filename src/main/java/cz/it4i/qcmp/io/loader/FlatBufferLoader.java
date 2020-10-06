package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.FlatBufferInputData;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.IOException;
import java.util.Arrays;

/**
 * This loader is used when the entire dataset is stored in single buffer (array).
 */
public class FlatBufferLoader extends BasicLoader implements IPlaneLoader {
    /**
     * Flat buffer information.
     */
    private final FlatBufferInputData bufferInputData;

    /**
     * Pixel count in single plane.
     */
    private final int planePixelCount;

    public FlatBufferLoader(final FlatBufferInputData bufferDataInfo) {
        super(bufferDataInfo.getDimensions());
        this.bufferInputData = bufferDataInfo;
        planePixelCount = dims.getX() * dims.getY();
    }

    @Override
    public boolean supportParallelLoading() {
        return true;
    }

    private void copyShortArrayIntoBuffer(final short[] srcArray,
                                          final int srcOffset,
                                          final int[] destBuffer,
                                          final int destOffset,
                                          final int copyLen) {
        for (int i = 0; i < copyLen; i++) {
            destBuffer[destOffset + i] = TypeConverter.shortToInt(srcArray[srcOffset + i]);
        }
    }

    @Override
    protected int valueAt(final int plane, final int offset) {
        return TypeConverter.shortToInt(((short[]) bufferInputData.getPixelBuffer())[(plane * planePixelCount) + offset]);
    }

    @Override
    public int[] loadPlaneData(final int plane) throws IOException {
        final short[] flatBuffer = ((short[]) bufferInputData.getPixelBuffer());
        final int offset = plane * planePixelCount;
        final int[] planeData = new int[planePixelCount];
        for (int i = 0; i < planePixelCount; i++) {
            planeData[i] = TypeConverter.shortToInt(flatBuffer[offset + i]);
        }
        return planeData;
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
        final long totalValueCount = (long) planePixelCount * (long) planes.length;
        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Unable to load image data for planes, file size is too big.");
        }

        Arrays.sort(planes);

        final short[] flatBuffer = (short[]) bufferInputData.getPixelBuffer();
        final int[] destBuffer = new int[(int) totalValueCount];
        int destOffset = 0;
        for (final int planeIndex : planes) {
            final int planeOffset = planeIndex * planePixelCount;
            copyShortArrayIntoBuffer(flatBuffer, planeOffset, destBuffer, destOffset, planePixelCount);
            destOffset += planePixelCount;
        }
        return destBuffer;
    }

    @Override
    public int[] loadAllPlanesU16Data() {
        final short[] flatBuffer = (short[]) bufferInputData.getPixelBuffer();
        return TypeConverter.shortArrayToIntArray(flatBuffer);
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
