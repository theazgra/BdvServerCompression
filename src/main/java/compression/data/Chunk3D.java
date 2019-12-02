package compression.data;

import compression.utilities.Utils;

import java.util.ArrayList;

public class Chunk3D {
    private final int[] data;
    private final int xSize;
    private final int ySize;
    private final int zSize;

    private long xOffset = 0;
    private long yOffset = 0;
    private long zOffset = 0;

    public Chunk3D(final V3i dims, final int[] data) {
        this.xSize = (int) dims.getX();
        this.ySize = (int) dims.getY();
        this.zSize = (int) dims.getZ();
        this.data = data;
    }

    public Chunk3D(final V3i dims, final short[] data) {
        this(dims, Utils.convertShortArrayToIntArray(data));
    }

    public Chunk3D(final V3i dims, final V3l offset, final int[] data) {
        this(dims, data);
        this.xOffset = offset.getX();
        this.yOffset = offset.getY();
        this.zOffset = offset.getZ();
    }

    public Chunk3D(final int xSize, final int ySize, final int zSize, final int[] data) {
        this.data = data;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;

        assert (data.length == (xSize * ySize * zSize)) : "Wrong box data.";
    }

    public Chunk3D(final int xSize, final int ySize, final int zSize, final short[] data) {
        this(xSize, ySize, zSize, Utils.convertShortArrayToIntArray(data));
    }

    public void setOffsets(final long xOffset, final long yOffset, final long zOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    /**
     * Calculate the index inside data array.
     *
     * @param x Zero based x coordinate.
     * @param y Zero based y coordinate.
     * @param z Zero based z coordinate.
     * @return Index inside data array.
     */
    private int computeIndex(final int x, final int y, final int z) {
        if (((x < 0) || (x >= xSize)) || (y < 0) || (y >= ySize) || (z < 0) || (z >= zSize)) {
            throw new IndexOutOfBoundsException("One of index x,y,z is out of bounds of the 3D box");
        }

        /// NOTE    In higher dimensions, the last dimension specified is the fastest changing on disk.
        //          So if we have a four dimensional dataset A, then the first element on disk would be A[0][0][0][0],
        //          the second A[0][0][0][1], the third A[0][0][0][2], and so on.
        //          SOURCE: https://support.hdfgroup.org/HDF5/doc/Advanced/Chunking/index.html


        /*
            (row*colCount) + col
            (x * (xSize+ySize)) + (y * ySize) + z
            0*(2*2)+0*2+0=0
            0*(2*2)+0*2+1=1
            0*(2*2)+1*2+0=2
            0*(2*2)+1*2+1=3
            1*(2*2)+0*2+0=4
            1*(2*2)+0*2+1=5
            1*(2*2)+1*2+0=6
            1*(2*2)+1*2+1=7
            3D 2x2x2 box
            0           1           2           3           4           5           6           7
            A[0][0][0]  A[0][0][1]  A[0][1][0]  A[0][1][1]  A[1][0][0]  A[1][0][1]  A[1][1][0]  A[1][1][1]
         */
        final int index = (x * (ySize * zSize)) + (y * zSize) + z;
        assert (index < data.length) : "Index calculation is wrong";
        return index;
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
    private int computeIndex(final int x, final int y, final int z, final V3i chunkDims) {
        if (((x < 0) || (x >= chunkDims.getX())) || (y < 0) || (y >= chunkDims.getY()) || (z < 0) || (z >= chunkDims.getZ())) {
            throw new IndexOutOfBoundsException("One of index x,y,z is out of bounds of the 3D box");
        }

        final int index = (x * (chunkDims.getY() * chunkDims.getZ())) + (y * chunkDims.getZ()) + z;
        return index;
    }


    public int getValueAt(final int x, final int y, final int z) {
        return data[computeIndex(x, y, z)];
    }

    public void setValueAt(final int x, final int y, final int z, final int value) {
        data[computeIndex(x, y, z)] = value;
    }

    public int getXSize() {
        return xSize;
    }

    public int getYSize() {
        return ySize;
    }

    public int getZSize() {
        return zSize;
    }

    public long getXOffset() {
        return xOffset;
    }

    public long getYOffset() {
        return yOffset;
    }

    public long getZOffset() {
        return zOffset;
    }

    @Override
    public String toString() {
        return String.format("3D box [%dx%dx%d] %d values", xSize, ySize, zSize, data.length);
    }

    public Chunk3D[] divideIntoChunks(final V3i chunkDims) {

        final int a = (int) Math.ceil(xSize / (double) chunkDims.getX());
        final int b = (int) Math.ceil(ySize / (double) chunkDims.getY());
        final int c = (int) Math.ceil(zSize / (double) chunkDims.getZ());
        final int chunkCount = a * b * c;
        Chunk3D[] chunks = new Chunk3D[chunkCount];
        int chunkIndex = 0;
        for (int chunkZOffset = 0; chunkZOffset < zSize; chunkZOffset += chunkDims.getZ()) {

            for (int chunkYOffset = 0; chunkYOffset < ySize; chunkYOffset += chunkDims.getY()) {

                for (int chunkXOffset = 0; chunkXOffset < xSize; chunkXOffset += chunkDims.getX()) {

                    chunks[chunkIndex++] = copyChunkFromBox(chunkDims, new V3i(chunkXOffset, chunkYOffset, chunkZOffset));

                }
            }
        }
        return chunks;
    }


    private boolean isInside(final int x, final int y, final int z) {
        return (((x >= 0) && (x < xSize)) && (y >= 0) && (y < ySize) && (z >= 0) && (z < zSize));
    }

    private Chunk3D copyChunkFromBox(final V3i chunkDims, final V3i chunkOffsets) {
        int[] chunkData = new int[(int) (chunkDims.getX() * chunkDims.getY() * chunkDims.getZ())];
        final int FILL_VALUE = 0;
        int srcX, srcY, srcZ;
        for (int x = 0; x < chunkDims.getX(); x++) {
            srcX = chunkOffsets.getX() + x;
            for (int y = 0; y < chunkDims.getY(); y++) {
                srcY = chunkOffsets.getY() + y;
                for (int z = 0; z < chunkDims.getZ(); z++) {
                    srcZ = chunkOffsets.getZ() + z;
                    final int dstIndex = computeIndex(x, y, z, chunkDims);

                    if (isInside(srcX, srcY, srcZ)) {
                        final int srcIndex = computeIndex(srcX, srcY, srcZ);
                        chunkData[dstIndex] = data[srcIndex];
                    } else {
                        // NOTE(Moravec): This make sense only when FILL_VALUE != 0
                        chunkData[dstIndex] = FILL_VALUE;
                    }
                }
            }
        }


        final Chunk3D chunk = new Chunk3D(chunkDims, chunkData);
        chunk.setOffsets(xOffset + chunkOffsets.getX(),
                yOffset + chunkOffsets.getY(),
                zOffset + chunkOffsets.getZ());
        return chunk;
    }
}
