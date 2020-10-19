package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.FileInputData;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public final class RawDataLoader extends BasicLoader implements IPlaneLoader {
    private final FileInputData inputDataInfo;

    public RawDataLoader(final FileInputData inputDataInfo) {
        super(inputDataInfo.getDimensions());
        this.inputDataInfo = inputDataInfo;
    }

    @Override
    protected int valueAt(final int plane, final int x, final int y, final int width) {
        new Exception().printStackTrace(System.err);
        assert (false) : "RawDataLoader shouldn't use valueAt impl methods!";
        return -1;
    }

    @Override
    public int[] loadPlaneData(final int plane) throws IOException {
        final byte[] buffer;

        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath())) {
            final long planeSize = (long) dims.getX() * (long) dims.getY() * 2;
            final long expectedFileSize = planeSize * dims.getZ();
            final long fileSize = fileStream.getChannel().size();

            if (expectedFileSize != fileSize) {
                throw new IOException(
                        "File specified by `rawFile` doesn't contains raw data for image of dimensions " +
                                "`rawDataDimension`");
            }

            final long planeOffset = plane * planeSize;

            buffer = new byte[(int) planeSize];
            if (fileStream.skip(planeOffset) != planeOffset) {
                throw new IOException("Failed to skip.");
            }

            int toRead = (int) planeSize;
            while (toRead > 0) {
                final int read = fileStream.read(buffer, (int) planeSize - toRead, toRead);
                if (read < 0) {
                    throw new IOException("Read wrong number of bytes.");
                }
                toRead -= read;
            }
        }
        return TypeConverter.unsignedShortBytesToIntArray(buffer);
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

        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath());
             final DataInputStream dis = new DataInputStream(new BufferedInputStream(fileStream, 8192))) {

            int lastIndex = 0;
            int valIndex = 0;

            for (final int planeIndex : planes) {
                // Skip specific number of bytes to get to the next plane.
                final int requestedSkip = (planeIndex == 0) ? 0 : ((planeIndex - lastIndex) - 1) * (int) planeDataSize;
                lastIndex = planeIndex;

                final int actualSkip = dis.skipBytes(requestedSkip);
                if (requestedSkip != actualSkip) {
                    throw new IOException("Skip operation failed.");
                }

                for (int i = 0; i < planeValueCount; i++) {
                    values[valIndex++] = dis.readUnsignedShort();
                }

            }
        }

        return values;
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        final V3i imageDims = inputDataInfo.getDimensions();
        final long dataSize = (long) imageDims.getX() * (long) imageDims.getY() * (long) imageDims.getZ();

        if (dataSize > (long) Integer.MAX_VALUE) {
            throw new IOException("RawFile size is too big.");
        }

        final int[] values = new int[(int) dataSize];

        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath());
             final DataInputStream dis = new DataInputStream(new BufferedInputStream(fileStream, 8192))) {

            for (int i = 0; i < (int) dataSize; i++) {
                values[i] = dis.readUnsignedShort();
            }
        }

        return values;
    }

    public int[][] loadAllPlanesTo2DArray() throws IOException {
        final V3i imageDims = inputDataInfo.getDimensions();
        final int planePixelCount = imageDims.getX() * imageDims.getY();
        final int planeDataSize = planePixelCount * 2;
        final int[][] result = new int[imageDims.getZ()][];

        final byte[] planeBuffer = new byte[planeDataSize];
        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath())) {

            for (int plane = 0; plane < imageDims.getZ(); plane++) {
                int toRead = planeDataSize;
                while (toRead > 0) {
                    final int read = fileStream.read(planeBuffer, planeDataSize - toRead, toRead);
                    assert (read > 0);
                    toRead -= read;
                }
                result[plane] = TypeConverter.unsignedShortBytesToIntArray(planeBuffer);
            }
        }
        return result;
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
