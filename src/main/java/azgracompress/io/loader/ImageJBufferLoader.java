package azgracompress.io.loader;

import azgracompress.data.Chunk3D;
import azgracompress.data.ImageU16;
import azgracompress.data.V3i;
import azgracompress.io.BufferInputData;
import azgracompress.io.InputData;
import azgracompress.utilities.TypeConverter;

import java.io.IOException;
import java.util.Arrays;

public class ImageJBufferLoader implements IPlaneLoader {
    private final BufferInputData bufferInputData;

    public ImageJBufferLoader(BufferInputData bufferDataInfo) {
        this.bufferInputData = bufferDataInfo;
        assert (this.bufferInputData.getPixelType() == InputData.PixelType.Gray16);
    }

    private int[] copyShortArray(short[] srcArray, int copyLen) {
        int[] destArray = new int[copyLen];
        for (int i = 0; i < copyLen; i++) {
            destArray[i] = TypeConverter.shortToInt(srcArray[i]);
        }
        return destArray;
    }

    private void copyShortArrayIntoBuffer(short[] srcArray, int[] destBuffer, int destOffset, int copyLen) {
        for (int i = 0; i < copyLen; i++) {
            destBuffer[destOffset + i] = TypeConverter.shortToInt(srcArray[i]);
        }
    }

    @Override
    public ImageU16 loadPlaneU16(int plane) throws IOException {
        assert (plane >= 0);
        switch (bufferInputData.getPixelType()) {
            case Gray16: {
                final int planePixelCount = bufferInputData.getDimensions().getX() * bufferInputData.getDimensions().getY();
                final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(plane);
                return new ImageU16(bufferInputData.getDimensions().getX(),
                                    bufferInputData.getDimensions().getY(),
                                    copyShortArray(srcBuffer, planePixelCount));
            }
            default:
                throw new IOException("Unable to load unsupported pixel type.");
        }
    }

    @Override
    public int[] loadPlanesU16Data(int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneU16(planes[0]).getData();
        } else if (planes.length == bufferInputData.getDimensions().getZ()) { // Maybe?
            return loadAllPlanesU16Data();
        }
        switch (bufferInputData.getPixelType()) {
            case Gray16: {
                final int planePixelCount = bufferInputData.getDimensions().getX() * bufferInputData.getDimensions().getY();
                final long totalValueCount = (long) planePixelCount * (long) planes.length;

                if (totalValueCount > (long) Integer.MAX_VALUE) {
                    throw new IOException("Unable to load image data for planes, file size is too big.");
                }

                Arrays.sort(planes);

                int[] destBuffer = new int[(int) totalValueCount];
                int destOffset = 0;
                for (final int planeIndex : planes) {
                    final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(planeIndex);
                    copyShortArrayIntoBuffer(srcBuffer, destBuffer, destOffset, planePixelCount);
                    destOffset += planePixelCount;
                }
                return destBuffer;
            }
            default:
                throw new IOException("Unable to load unsupported pixel type.");
        }
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        switch (bufferInputData.getPixelType()) {
            case Gray16: {
                final V3i imageDims = bufferInputData.getDimensions();
                final long totalValueCount = imageDims.multiplyTogether();
                final int planePixelCount = imageDims.getX() * imageDims.getY();

                if (totalValueCount > (long) Integer.MAX_VALUE) {
                    throw new IOException("Unable to load all image data, file size is too big.");
                }

                int[] destBuffer = new int[(int) totalValueCount];
                int destOffset = 0;
                for (int planeIndex = 0; planeIndex < imageDims.getZ(); planeIndex++) {
                    final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(planeIndex);
                    copyShortArrayIntoBuffer(srcBuffer, destBuffer, destOffset, planePixelCount);
                    destOffset += planePixelCount;
                }
                return destBuffer;
            }
            default:
                throw new IOException("Unable to load unsupported pixel type.");
        }
    }

    @Override
    public int[][] loadVoxels(V3i voxelDim) throws IOException {

        switch (bufferInputData.getPixelType()) {
            case Gray16: {
                final V3i dims = bufferInputData.getDimensions();
                final int voxelSize = (int) voxelDim.multiplyTogether();
                final int voxelCount = Chunk3D.calculateRequiredChunkCount(bufferInputData.getDimensions(), voxelDim);

                int[][] voxels = new int[voxelCount][voxelSize];
                int voxelIndex = 0;

                for (int chunkZOffset = 0; chunkZOffset < dims.getZ(); chunkZOffset += voxelDim.getZ()) {
                    for (int chunkYOffset = 0; chunkYOffset < dims.getY(); chunkYOffset += voxelDim.getY()) {
                        for (int chunkXOffset = 0; chunkXOffset < dims.getX(); chunkXOffset += voxelDim.getX()) {
                            copyDataToVector(voxels[voxelIndex++], voxelDim, chunkXOffset, chunkYOffset, chunkZOffset);
                        }
                    }
                }
                return voxels;
            }
            default:
                throw new IOException("Unable to load unsupported pixel type.");
        }
    }

    private int readDataAt(final int x, final int y, final int z) {

        final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(z);
        return srcBuffer[(y * bufferInputData.getDimensions().getX()) + x];
    }

    /**
     * Copy this chunk data to chunk vector.
     *
     * @param vector       Chunk vector.
     * @param qVectorDims  Dimensions of the vector.
     * @param chunkXOffset Chunk X offset
     * @param chunkYOffset Chunk Y offset.
     * @param chunkZOffset Chunk Z offset.
     */
    private void copyDataToVector(int[] vector,
                                  final V3i qVectorDims,
                                  final int chunkXOffset,
                                  final int chunkYOffset,
                                  final int chunkZOffset) {
        int srcZ;
        for (int z = 0; z < qVectorDims.getZ(); z++) {
            srcZ = chunkZOffset + z;
            fillVoxelFromPlane(vector, qVectorDims, srcZ, z, chunkXOffset, chunkYOffset);
        }
    }

    private void fillVoxelFromPlane(int[] vector,
                                    final V3i qVectorDims,
                                    final int srcZ,
                                    final int z,
                                    final int chunkXOffset,
                                    final int chunkYOffset) {

        if (srcZ >= bufferInputData.getDimensions().getZ())
            return;

        final short[] srcBuffer = (short[]) bufferInputData.getPixelBuffer(srcZ);
        final int width = bufferInputData.getDimensions().getX();

        int srcX, srcY;
        for (int y = 0; y < qVectorDims.getY(); y++) {
            srcY = chunkYOffset + y;
            for (int x = 0; x < qVectorDims.getX(); x++) {
                srcX = chunkXOffset + x;
                final int dstIndex = index(x, y, z, qVectorDims);
                vector[dstIndex] = isInside(srcX, srcY, srcZ) ? srcBuffer[(srcY * width) + srcX] : 0;
            }
        }
    }

    private boolean isInside(final int x, final int y, final int z) {
        return (((x >= 0) && (x < bufferInputData.getDimensions().getX())) && (y >= 0) &&
                (y < bufferInputData.getDimensions().getY()) && (z >= 0) &&
                (z < bufferInputData.getDimensions().getZ()));
    }

    /**
     * Calculate the index inside data array.
     *
     * @param x Zero based x coordinate.
     * @param y Zero based y coordinate.
     * @param z Zero based z coordinate.
     * @return Index inside data array.
     */
    private int index(final int x, final int y, final int z) {
        return index(x, y, z, bufferInputData.getDimensions());
    }

    /**
     * Calculate the index inside array of dimensions specified by chunkDims.
     *
     * @param x         Zero based x coordinate.
     * @param y         Zero based y coordinate.
     * @param z         Zero based z coordinate.
     * @param chunkDims Chunk dimensions.
     * @return Index inside chunk dimension data array.
     */
    private int index(final int x, final int y, final int z, final V3i chunkDims) {
        assert (x >= 0 && x < chunkDims.getX()) : "Index X out of bounds.";
        assert (y >= 0 && y < chunkDims.getY()) : "Index Y out of bounds.";
        assert (z >= 0 && z < chunkDims.getZ()) : "Index Z out of bounds.";

        // NOTE(Moravec): Description of the following calculation
        //               plane index      *        plane pixel count
        //                    |                            |
        //                    V                            V
        // planeOffset = chunkDims.getZ() * (chunkDims.getX() * chunkDims.getY())

        //           row *  pixels in row
        //             |         |
        //             V         V
        // rowOffset = y * chunkDims.getX();

        //          column
        //             |
        //             V
        // colOffset = x;

        return (z * (chunkDims.getX() * chunkDims.getY())) + (y * chunkDims.getX()) + x;
    }
}

