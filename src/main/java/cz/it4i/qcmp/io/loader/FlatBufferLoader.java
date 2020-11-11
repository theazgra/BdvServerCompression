package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Block;
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
public class FlatBufferLoader extends GenericLoader implements IPlaneLoader {
    /**
     * Flat buffer information.
     */
    private final FlatBufferInputData bufferInputData;

    /**
     * Pixel count in single plane.
     */
    private final int planePixelCount;

    private final int stackPixelCount;

    public FlatBufferLoader(final FlatBufferInputData bufferDataInfo) {
        super(bufferDataInfo.getDimensions());
        this.bufferInputData = bufferDataInfo;
        planePixelCount = dims.getNumberOfElementsInDimension(2);
        stackPixelCount = dims.getNumberOfElementsInDimension(3);
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

    /**
     * Calculate offset in the flat buffer to read plane at specified timepoint and plane index.
     *
     * @param timepoint Zero based timepoint.
     * @param plane     Zero based plane index.
     * @return Offset in the flat buffer..
     */
    private int calculateDataOffset(final int timepoint, final int plane) {
        return (timepoint * stackPixelCount) + (plane * planePixelCount);
    }

    @Override
    protected int valueAt(final int timepoint, final int plane, final int x, final int y, final int width) {
        //                     BaseOffset                            + PlaneOffset
        final int dataOffset = calculateDataOffset(timepoint, plane) + Block.index(x, y, width);
        return TypeConverter.shortToInt(((short[]) bufferInputData.getPixelBuffer())[dataOffset]);
    }

    @Override
    public int[] loadPlaneData(final int timepoint, final int plane) {
        final short[] flatBuffer = ((short[]) bufferInputData.getPixelBuffer());
        final int baseOffset = calculateDataOffset(timepoint, plane);
        final int[] planeData = new int[planePixelCount];
        for (int i = 0; i < planePixelCount; i++) {
            planeData[i] = TypeConverter.shortToInt(flatBuffer[baseOffset + i]);
        }
        return planeData;
    }

    @Override
    public int[] loadPlanesU16Data(final int timepoint, final int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneData(timepoint, planes[0]);
        } else if (planes.length == bufferInputData.getDimensions().getPlaneCount()) {
            return loadAllPlanesU16Data(timepoint);
        }

        Arrays.sort(planes);
        final int totalValueCount = Math.multiplyExact(planePixelCount, planes.length);


        final short[] flatBuffer = (short[]) bufferInputData.getPixelBuffer();
        final int[] destBuffer = new int[totalValueCount];
        int destOffset = 0;
        for (final int planeIndex : planes) {
            final int baseOffset = calculateDataOffset(timepoint, planeIndex);
            copyShortArrayIntoBuffer(flatBuffer, baseOffset, destBuffer, destOffset, planePixelCount);
            destOffset += planePixelCount;
        }
        return destBuffer;
    }

    @Override
    public int[] loadAllPlanesU16Data(final int timepoint) {
        final short[] flatBuffer = (short[]) bufferInputData.getPixelBuffer();

        final int baseOffset = calculateDataOffset(timepoint, 0);
        final int[] result = new int[stackPixelCount];
        for (int i = 0; i < stackPixelCount; i++) {
            result[i] = TypeConverter.shortToInt(flatBuffer[baseOffset + i]);
        }
        return result;
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
