package azgracompress.data;

import java.util.Arrays;

public class Chunk3D {
    private final int FILL_VALUE = 0;
    private final int[] data;

    private final V3i dims;

    public Chunk3D(final V3i dims, final int[] data) {
        this.dims = dims;
        this.data = data;
        assert (data.length == (dims.getX() * dims.getY() * dims.getZ())) : "Wrong box data.";
    }

    public Chunk3D(final V3i chunkDims) {
        this(chunkDims, new int[(int) chunkDims.multiplyTogether()]);
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
        return index(x, y, z, dims);
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
        assert (x >= 0 && x < dims.getX()) : "Index X out of bounds.";
        assert (y >= 0 && y < dims.getY()) : "Index Y out of bounds.";
        assert (z >= 0 && z < dims.getZ()) : "Index Z out of bounds.";

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

        return (chunkDims.getZ() * (chunkDims.getX() * chunkDims.getY())) + (y * chunkDims.getX()) + x;
    }


    public int getValueAt(final int x, final int y, final int z) {
        return data[index(x, y, z)];
    }

    public void setValueAt(final int x, final int y, final int z, final int value) {
        data[index(x, y, z)] = value;
    }

    public V3i getDims() {
        return dims;
    }

    @Override
    public String toString() {
        return String.format("3D box %s %d values", dims.toString(), data.length);
    }

    public int[][] divideInto3DVectors(final V3i qVectorDims) {
        final int chunkSize = qVectorDims.getX() * qVectorDims.getY() * qVectorDims.getZ();
        final int chunkCount = calculateRequiredChunkCount(qVectorDims);

        int[][] vectors = new int[chunkCount][chunkSize];
        int vecIndex = 0;

        for (int chunkZOffset = 0; chunkZOffset < dims.getZ(); chunkZOffset += qVectorDims.getZ()) {
            for (int chunkYOffset = 0; chunkYOffset < dims.getY(); chunkYOffset += qVectorDims.getY()) {
                for (int chunkXOffset = 0; chunkXOffset < dims.getX(); chunkXOffset += qVectorDims.getX()) {
                    copyDataToVector(vectors[vecIndex++], qVectorDims, chunkXOffset, chunkYOffset, chunkZOffset);
                }
            }
        }
        return vectors;
    }

    private int calculateRequiredChunkCount(final V3i chunkDims) {
        return calculateRequiredChunkCount(dims, chunkDims);
    }

    public static int calculateRequiredChunkCount(final V3i imageDims, final V3i chunkDims) {
        final int xChunkCount = (int) Math.ceil((double) imageDims.getX() / (double) chunkDims.getX());
        final int yChunkCount = (int) Math.ceil((double) imageDims.getY() / (double) chunkDims.getY());
        final int zChunkCount = (int) Math.ceil((double) imageDims.getZ() / (double) chunkDims.getZ());
        return (xChunkCount * yChunkCount * zChunkCount);
    }

    private boolean isInside(final int x, final int y, final int z) {
        return (((x >= 0) && (x < dims.getX())) && (y >= 0) && (y < dims.getY()) && (z >= 0) && (z < dims.getZ()));
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
        int srcX, srcY, srcZ;
        for (int z = 0; z < qVectorDims.getZ(); z++) {
            srcZ = chunkZOffset + z;
            for (int y = 0; y < qVectorDims.getY(); y++) {
                srcY = chunkYOffset + y;
                for (int x = 0; x < qVectorDims.getX(); x++) {
                    srcX = chunkXOffset + x;
                    final int dstIndex = index(x, y, z, qVectorDims);
                    vector[dstIndex] = isInside(srcX, srcY, srcZ) ? data[index(srcX, srcY, srcZ)] : FILL_VALUE;
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Chunk3D) {
            final Chunk3D otherChunk = (Chunk3D) obj;
            if (data.length != otherChunk.data.length) {
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

    public int[] getData() {
        return data;
    }

    public void zeroData() {
        Arrays.fill(data, 0);
    }
}
