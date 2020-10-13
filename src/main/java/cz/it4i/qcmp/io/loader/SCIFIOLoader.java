package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.ScifioWrapper;
import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.FileInputData;
import cz.it4i.qcmp.utilities.TypeConverter;
import io.scif.FormatException;
import io.scif.Reader;

import java.io.IOException;
import java.util.Arrays;

public final class SCIFIOLoader extends BasicLoader implements IPlaneLoader {
    private final FileInputData inputDataInfo;
    private final Reader reader;

    /**
     * Create SCIFIO reader from input file.
     *
     * @param inputDataInfo Input file info.
     * @throws IOException     When fails to create SCIFIO reader.
     * @throws FormatException When fails to create SCIFIO reader.
     */
    public SCIFIOLoader(final FileInputData inputDataInfo) throws IOException, FormatException {
        super(inputDataInfo.getDimensions());
        this.inputDataInfo = inputDataInfo;
        this.reader = ScifioWrapper.getReader(this.inputDataInfo.getFilePath());
    }

    @Override
    protected int valueAt(final int plane, final int x, final int y, final int width) {
        new Exception().printStackTrace(System.err);
        assert (false) : "SCIFIOLoader shouldn't use valueAt impl methods!";
        return -1;
    }

    @Override
    public int[] loadPlaneData(final int plane) throws IOException {
        final byte[] planeBytes;
        try {
            planeBytes = reader.openPlane(0, plane).getBytes();
        } catch (final FormatException e) {
            throw new IOException("Unable to open plane with the reader. " + e.getMessage());
        }
        return TypeConverter.unsignedShortBytesToIntArray(planeBytes);
    }

    @Override
    public int[] loadPlanesU16Data(final int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneData(planes[0]);
        }

        final int planeValueCount = inputDataInfo.getDimensions().getX() * inputDataInfo.getDimensions().getY();
        final long planeDataSize = 2 * (long) planeValueCount;

        final long totalValueCount = (long) planeValueCount * planes.length;

        if (totalValueCount > (long) Integer.MAX_VALUE) {
            throw new IOException("Integer count is too big.");
        }

        final int[] values = new int[(int) totalValueCount];
        Arrays.sort(planes);

        byte[] planeBytes;
        for (int i = 0; i < planes.length; i++) {
            final int plane = planes[i];

            try {
                planeBytes = reader.openPlane(0, plane).getBytes();
            } catch (final FormatException e) {
                throw new IOException("Unable to open plane.");
            }
            if (planeBytes.length != planeDataSize) {
                throw new IOException("Bad byte count read from plane.");
            }

            TypeConverter.unsignedShortBytesToIntArray(planeBytes, values, (i * planeValueCount));
        }
        return values;
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        final V3i imageDims = inputDataInfo.getDimensions();
        final long planePixelCount = (long) imageDims.getX() * (long) imageDims.getY();
        final long dataSize = planePixelCount * (long) imageDims.getZ();

        if (dataSize > (long) Integer.MAX_VALUE) {
            throw new IOException("FileSize is too big.");
        }

        final int[] values = new int[(int) dataSize];
        byte[] planeBytes;
        for (int plane = 0; plane < imageDims.getZ(); plane++) {
            try {
                planeBytes = reader.openPlane(0, plane).getBytes();
            } catch (final FormatException e) {
                throw new IOException("Unable to open plane.");
            }
            if (planeBytes.length != 2 * planePixelCount) {
                throw new IOException("Bad byte count read from plane.");
            }
            TypeConverter.unsignedShortBytesToIntArray(planeBytes, values, (int) (plane * planePixelCount));
        }

        return values;
    }

    @Override
    public int[][] loadRowVectors(final int vectorSize, final Range<Integer> planeRange) throws IOException {
        return loadRowVectorsImplByLoadPlaneData(vectorSize, planeRange);
    }

    @Override
    public int[][] loadBlocks(final V2i blockDim, final Range<Integer> planeRange) throws IOException {
        return loadBlocksImplByLoadPlaneData(blockDim, planeRange);
    }

    @Override
    public int[][] loadVoxels(final V3i voxelDim, final Range<Integer> planeRange) throws IOException {
        return loadVoxelsImplByLoadPlaneData(voxelDim, planeRange);
    }
}
