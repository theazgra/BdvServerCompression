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

public final class RawDataLoader extends GenericLoader implements IPlaneLoader {
    private final FileInputData inputDataInfo;

    private interface StorePlaneDataCallback {
        void store(final int planeOffset, final int[] planeData);
    }

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
    public int[] loadPlaneData(final int timepoint, final int plane) throws IOException {
        final byte[] buffer;

        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath())) {
            final long planeSize = dims.getNumberOfElementsInDimension(2) * 2;
            final long expectedFileSize = dims.getDataSize();
            final long fileSize = fileStream.getChannel().size();


            if (expectedFileSize != fileSize) {
                throw new IOException(
                        "File specified by `rawFile` doesn't contains raw data for image of dimensions " + dims.toString());
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

    private void loadPlanesU16DataImpl(final int[] planes, final StorePlaneDataCallback storeCallback) throws IOException {
        if (planes.length < 1) {
            return;
        } else if (planes.length == 1) {
            storeCallback.store(0, loadPlaneData(0, planes[0]));
        }

        final int planeValueCount = dims.getNumberOfElementsInDimension(2);
        final int planeDataSize = 2 * planeValueCount;

        final byte[] planeBuffer = new byte[planeDataSize];

        Arrays.sort(planes);
        int planeOffset = 0;
        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath())) {
            int lastIndex = 0;
            for (final int planeIndex : planes) {
                // Skip specific number of bytes to get to the next plane.
                final int requestedSkip = (planeIndex == 0) ? 0 : ((planeIndex - lastIndex) - 1) * (int) planeDataSize;
                lastIndex = planeIndex;

                final long actualSkip = fileStream.skip(requestedSkip);
                if (requestedSkip != actualSkip) {
                    throw new IOException("Skip operation failed.");
                }

                int toRead = planeDataSize;
                while (toRead > 0) {
                    final int read = fileStream.read(planeBuffer, planeDataSize - toRead, toRead);
                    assert (read > 0);
                    toRead -= read;
                }

                storeCallback.store(planeOffset, TypeConverter.unsignedShortBytesToIntArray(planeBuffer));
                ++planeOffset;
            }
        }
    }

    @Override
    public int[][] loadPlanesU16DataTo2dArray(final int[] planes) throws IOException {
        final int[][] data = new int[planes.length][];

        loadPlanesU16DataImpl(planes, (index, planeData) -> {
            data[index] = planeData;
        });

        return data;
    }

    @Override
    public int[] loadPlanesU16Data(final int timepoint, final int[] planes) throws IOException {
        final int planeValueCount = dims.getNumberOfElementsInDimension(2);
        final int totalValueCount = dims.getNumberOfElementsInDimension(3);

        final int[] data = new int[totalValueCount];

        loadPlanesU16DataImpl(planes, (index, planeData) -> {
            System.arraycopy(planeData, 0, data, (index * planeValueCount), planeValueCount);
        });

        return data;
    }

    @Override
    public int[] loadAllPlanesU16Data(final int timepoint) throws IOException {
        final int dataElementCount = dims.getNumberOfElementsInDimension(3);


        final int[] values = new int[(int) dataElementCount];

        // TODO(Moravec): dis.readUnsignedShort() should be replaced with .read()!
        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath());
             final DataInputStream dis = new DataInputStream(new BufferedInputStream(fileStream, 8192))) {

            for (int i = 0; i < (int) dataElementCount; i++) {
                values[i] = dis.readUnsignedShort();
            }
        }

        return values;
    }

    public int[][] loadAllPlanesTo2DArray() throws IOException {
        final int planePixelCount = dims.getNumberOfElementsInDimension(2);
        final int planeDataSize = planePixelCount * 2;
        final int[][] result = new int[dims.getPlaneCount()][];

        final byte[] planeBuffer = new byte[planeDataSize];
        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath())) {

            for (int plane = 0; plane < dims.getPlaneCount(); plane++) {
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
    public int[][] loadRowVectors(final int timepoint, final int vectorSize, final Range<Integer> planeRange) throws IOException {
        return loadRowVectorsImplByLoadPlaneData(timepoint, vectorSize, planeRange);
    }

    @Override
    public int[][] loadBlocks(final int timepoint, final V2i blockDim, final Range<Integer> planeRange) throws IOException {
        return loadBlocksImplByLoadPlaneData(timepoint, blockDim, planeRange);
    }

    @Override
    public int[][] loadVoxels(final int timepoint, final V3i voxelDim, final Range<Integer> planeRange) throws IOException {
        return loadVoxelsImplByLoadPlaneData(voxelDim, timepoint, planeRange);
    }


}
