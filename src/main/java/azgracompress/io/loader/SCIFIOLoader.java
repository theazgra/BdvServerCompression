package azgracompress.io.loader;

import azgracompress.ScifioWrapper;
import azgracompress.data.Range;
import azgracompress.data.V2i;
import azgracompress.data.V3i;
import azgracompress.io.FileInputData;
import azgracompress.utilities.TypeConverter;
import io.scif.FormatException;
import io.scif.Reader;

import java.io.IOException;
import java.util.Arrays;

public final class SCIFIOLoader extends BasicLoader implements IPlaneLoader {
    private final FileInputData inputDataInfo;
    private final Reader reader;

    // Current plane buffer
    private int currentPlaneIndex = -1;
    private int[] currentPlaneData;

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

//    @Override
//    protected int valueAt(int plane, int offset) {
//        // TODO(Moravec): Measure if caching the current plane byte buffer make any sense.
//        if (plane != currentPlaneIndex) {
//            currentPlaneIndex = plane;
//            try {
//                currentPlaneData = TypeConverter.unsignedShortBytesToIntArray(reader.openPlane(0, currentPlaneIndex).getBytes());
//            } catch (FormatException e) {
//                System.err.println(e.toString());
//                e.printStackTrace();
//                assert (false) : "FormatException in SCIFIOLoader::valueAt()";
//            } catch (IOException e) {
//                System.err.println(e.toString());
//                e.printStackTrace();
//                assert (false) : "IOException in SCIFIOLoader::valueAt()";
//            }
//        }
//        return currentPlaneData[offset];
//    }

    @Override
    public int[] loadPlaneData(final int plane) throws IOException {
        byte[] planeBytes;
        try {
            planeBytes = reader.openPlane(0, plane).getBytes();
        } catch (FormatException e) {
            throw new IOException("Unable to open plane with the reader. " + e.getMessage());
        }
        return TypeConverter.unsignedShortBytesToIntArray(planeBytes);
    }

    @Override
    public int[] loadPlanesU16Data(int[] planes) throws IOException {
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

        int[] values = new int[(int) totalValueCount];
        Arrays.sort(planes);

        byte[] planeBytes;
        for (int i = 0; i < planes.length; i++) {
            final int plane = planes[i];

            try {
                planeBytes = reader.openPlane(0, plane).getBytes();
            } catch (FormatException e) {
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

        int[] values = new int[(int) dataSize];
        byte[] planeBytes;
        for (int plane = 0; plane < imageDims.getZ(); plane++) {
            try {
                planeBytes = reader.openPlane(0, plane).getBytes();
            } catch (FormatException e) {
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
    public int[][] loadBlocks(V2i blockDim, Range<Integer> planeRange) throws IOException {
        return loadBlocksImplLoadPlaneData(blockDim, planeRange);
    }

    @Override
    public int[][] loadVoxels(final V3i voxelDim, final Range<Integer> planeRange) throws IOException {
        return loadVoxelsImplByLoadPlaneData(voxelDim, planeRange);
    }
}
