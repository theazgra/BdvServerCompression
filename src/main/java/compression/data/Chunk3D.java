package compression.data;

import compression.utilities.Utils;

import java.util.ArrayList;

public class Chunk3D {
    private final int[] data;

    private final V3i dims;
    private final V3l offset;

    public Chunk3D(final V3i dims, final V3l offset, final int[] data) {
        this.dims = dims;
        this.data = data;
        this.offset = offset;
        assert (data.length == (dims.getX() * dims.getY() * dims.getZ())) : "Wrong box data.";
    }

    public Chunk3D(final V3i dims, final int[] data) {
        this(dims, new V3l(0), data);

    }

    public Chunk3D(final V3i dims, final short[] data) {
        this(dims, new V3l(0), Utils.convertShortArrayToIntArray(data));
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
        assert (x >= 0 && x < dims.getX()) : "Index X out of bounds.";
        assert (y >= 0 && y < dims.getY()) : "Index Y out of bounds.";
        assert (z >= 0 && z < dims.getZ()) : "Index Z out of bounds.";
        /// NOTE    In higher dimensions, the last dimension specified is the fastest changing on disk.
        //          So if we have a four dimensional dataset A, then the first element on disk would be A[0][0][0][0],
        //          the second A[0][0][0][1], the third A[0][0][0][2], and so on.
        //          SOURCE: https://support.hdfgroup.org/HDF5/doc/Advanced/Chunking/index.html


        /*
            0           1           2           3           4           5           6           7
            A[0][0][0]  A[0][0][1]  A[0][1][0]  A[0][1][1]  A[1][0][0]  A[1][0][1]  A[1][1][0]  A[1][1][1]
         */
        final int index = (x * (dims.getY() * dims.getZ())) + (y * dims.getZ()) + z;
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

    public V3i getDims() {
        return dims;
    }

    public V3l getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return String.format("3D box %s %d values", dims.toString(), data.length);
    }

    public Chunk3D[] divideIntoChunks(final V3i chunkDims) {

        final int xSize = dims.getX();
        final int ySize = dims.getY();
        final int zSize = dims.getZ();

        final int xChunkCount = (int) Math.ceil(xSize / (double) chunkDims.getX());
        final int yChunkCount = (int) Math.ceil(ySize / (double) chunkDims.getY());
        final int zChunkCount = (int) Math.ceil(zSize / (double) chunkDims.getZ());
        final int chunkCount = xChunkCount * yChunkCount * zChunkCount;
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

    public void reconstructFromChunks(final Chunk3D[] chunks) {
        assert (chunks.length > 0) : "No chunks in reconstruct";
        final V3i chunkDims = chunks[0].getDims();
        final int xSize = dims.getX();
        final int ySize = dims.getY();
        final int zSize = dims.getZ();
        assert assertCorrectChunkCount(chunkDims, chunks.length) : "Wrong chunk count in reconstruct";


    }

    private boolean assertCorrectChunkCount(final V3i chunkDims, final int receivedChunkCount) {
        final int xChunkCount = (int) Math.ceil(dims.getX() / (double) chunkDims.getX());
        final int yChunkCount = (int) Math.ceil(dims.getY() / (double) chunkDims.getY());
        final int zChunkCount = (int) Math.ceil(dims.getZ() / (double) chunkDims.getZ());
        final int expectedChunkCount = xChunkCount * yChunkCount * zChunkCount;
        if (receivedChunkCount != expectedChunkCount) {
            return false;
        } else {
            return true;
        }
    }


    private boolean isInside(final int x, final int y, final int z) {
        return (((x >= 0) && (x < dims.getX())) && (y >= 0) && (y < dims.getY()) && (z >= 0) && (z < dims.getZ()));
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


        final V3l chunkOffset = offset.add(chunkOffsets);
        final Chunk3D chunk = new Chunk3D(chunkDims, chunkOffset, chunkData);
        return chunk;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Chunk3D) {
            final Chunk3D otherChunk = (Chunk3D) obj;
            if (data.length != otherChunk.data.length) {
                return false;
            } else if (!(offset.equals(otherChunk.offset))) {
                return false;
            } else {
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != otherChunk.data[i]) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return super.equals(obj);
        }
    }
}
