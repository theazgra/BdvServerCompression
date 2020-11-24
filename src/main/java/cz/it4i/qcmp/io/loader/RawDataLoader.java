package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.FileInputData;
import cz.it4i.qcmp.utilities.TypeConverter;

import java.io.*;
import java.util.Arrays;

public final class RawDataLoader extends GenericLoader implements IPlaneLoader {
    private final int BYTES_PER_ELEMENT = 2;
    private final FileInputData inputDataInfo;
    private final int planeDataSize;

    private interface StorePlaneDataCallback {
        void store(final int planeOffset, final int[] planeData);
    }

    public RawDataLoader(final FileInputData inputDataInfo) {
        super(inputDataInfo.getDimensions());
        this.inputDataInfo = inputDataInfo;
        planeDataSize = Math.multiplyExact(inputDataInfo.getDimensions().getNumberOfElementsInDimension(2), 2);
    }

    @Override
    protected int valueAt(final int timepoint, final int plane, final int x, final int y, final int sourceWidth) {
        new Exception().printStackTrace(System.err);
        assert (false) : "RawDataLoader shouldn't use valueAt impl methods!";
        return -1;
    }

    /**
     * Calculate offset in the file to read plane at specified timepoint and plane index.
     *
     * @param timepoint Zero based timepoint.
     * @param plane     Zero based plane index.
     * @return Offset in the file.
     */
    private int calculatePlaneDataOffset(final int timepoint, final int plane) {
        return (BYTES_PER_ELEMENT * timepoint * dims.getNumberOfElementsInDimension(3)) + (plane * planeDataSize);
    }


    @Override
    public int[] loadPlaneData(final int timepoint, final int plane) throws IOException {
        final byte[] buffer;
        final long expectedFileSize = dims.getDataSize();

        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath())) {
            if (expectedFileSize != fileStream.getChannel().size()) {
                throw new IOException("Specified RAW file dimensions '" + dims + "' are nor correct.");
            }

            final long planeOffset = calculatePlaneDataOffset(timepoint, plane);

            buffer = new byte[(int) planeDataSize];
            forceSkip(fileStream, planeOffset);

            int toRead = planeDataSize;
            while (toRead > 0) {
                final int read = fileStream.read(buffer, planeDataSize - toRead, toRead);
                if (read < 0) {
                    throw new IOException("Read wrong number of bytes.");
                }
                toRead -= read;
            }
        }
        return TypeConverter.unsignedShortBytesToIntArray(buffer);
    }

    /**
     * Force skip requested number of bytes in file stream.
     *
     * @param fileStream Stream to skip in.
     * @param byteCount  Number of bytes to skip.
     * @throws IOException when skip fails.
     */
    private void forceSkip(final InputStream fileStream, final long byteCount) throws IOException {
        if (fileStream.skip(byteCount) != byteCount) {
            throw new IOException("Skip operation failed. Didn't skip requested number of bytes.");
        }
    }

    private void loadPlanesU16DataImpl(final int timepoint,
                                       final int[] planes,
                                       final StorePlaneDataCallback storeCallback) throws IOException {
        if (planes.length < 1) {
            return;
        } else if (planes.length == 1) {
            storeCallback.store(0, loadPlaneData(timepoint, planes[0]));
        }
        Arrays.sort(planes);

        final byte[] planeBuffer = new byte[planeDataSize];

        int callbackPlaneOffset = 0;
        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath())) {

            // Skip to requested timepoint.
            forceSkip(fileStream, calculatePlaneDataOffset(timepoint, 0));

            int lastIndex = 0;
            for (final int planeIndex : planes) {
                // Skip specific number of bytes to get to the next plane.
                final int requestedSkip = (planeIndex == 0) ? 0 : ((planeIndex - lastIndex) - 1) * planeDataSize;
                lastIndex = planeIndex;

                forceSkip(fileStream, requestedSkip);

                int toRead = planeDataSize;
                while (toRead > 0) {
                    final int read = fileStream.read(planeBuffer, planeDataSize - toRead, toRead);
                    assert (read > 0);
                    toRead -= read;
                }

                storeCallback.store(callbackPlaneOffset, TypeConverter.unsignedShortBytesToIntArray(planeBuffer));
                ++callbackPlaneOffset;
            }
        }
    }

    @Override
    public int[][] loadPlanesU16DataTo2dArray(final int timepoint, final int[] planes) throws IOException {
        final int[][] data = new int[planes.length][];

        loadPlanesU16DataImpl(timepoint, planes, (index, planeData) -> {
            data[index] = planeData;
        });

        return data;
    }

    @Override
    public int[] loadPlanesU16Data(final int timepoint, final int[] planes) throws IOException {
        final int planeValueCount = dims.getNumberOfElementsInDimension(2);
        final int totalValueCount = dims.getNumberOfElementsInDimension(3);

        final int[] data = new int[totalValueCount];

        loadPlanesU16DataImpl(timepoint, planes, (index, planeData) -> {
            System.arraycopy(planeData, 0, data, (index * planeValueCount), planeValueCount);
        });

        return data;
    }

    @Override
    public int[] loadAllPlanesU16Data(final int timepoint) throws IOException {
        final int dataElementCount = dims.getNumberOfElementsInDimension(3);


        final int[] values = new int[(int) dataElementCount];

        // NOTE(Moravec): Should dis.readUnsignedShort() be replaced with .read() ?
        try (final FileInputStream fileStream = new FileInputStream(inputDataInfo.getFilePath());
             final DataInputStream dis = new DataInputStream(new BufferedInputStream(fileStream, 8192))) {

            forceSkip(dis, calculatePlaneDataOffset(timepoint, 0));

            for (int i = 0; i < dataElementCount; i++) {
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
