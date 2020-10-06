package cz.it4i.qcmp.data;

public class Block {
    private final int FILL_VALUE = 0;
    private final int[] data;

    private final V2i dims;

    /**
     * Create the Chunk2D of given dimensions, offset and initialize it with data.
     *
     * @param dims Dimensions of the chunk.
     * @param data Chunk data.
     */
    public Block(final V2i dims, final int[] data) {
        this.dims = dims;
        this.data = data;
        assert (data.length == (dims.getX() * dims.getY())) : "Wrong box data.";
    }

    /**
     * Create Chunk2D of given dimensions and offset. Allocates the data.
     *
     * @param dims Dimensions of the chunk.
     */
    public Block(final V2i dims) {
        this(dims, new int[dims.getX() * dims.getY()]);
    }

    /**
     * Calculate the index inside data array.
     *
     * @param x Zero based x coordinate.
     * @param y Zero based y coordinate.
     * @return Index inside data array.
     */
    private int index(final int x, final int y) {
        return index(x, y, dims.getX());
    }

    /**
     * Calculate the index inside `2D` array
     *
     * @param x          Zero based x coordinate.
     * @param y          Zero based y coordinate.
     * @param chunkWidth Data width.
     * @return Index inside chunk dimension data array.
     */
    public static int index(final int x, final int y, final int chunkWidth) {
        //        assert (x >= 0 && x < chunkDims.getX()) : "Index X out of bounds.";
        //        assert (y >= 0 && y < chunkDims.getY()) : "Index Y out of bounds.";
        return (y * chunkWidth) + x;
    }


    public int getValueAt(final int x, final int y) {
        return data[index(x, y)];
    }

    public void setValueAt(final int x, final int y, final int value) {
        data[index(x, y)] = value;
    }

    public V2i getDims() {
        return dims;
    }

    @Override
    public String toString() {
        return String.format("2D shape %s %d values", dims.toString(), data.length);
    }

    /**
     * Reconstruct this Chunk from array of 1D row vectors.
     *
     * @param vectors Array of 1D row vectors.
     */
    public void reconstructFromVectors(final int[][] vectors) {
        if (vectors.length == 0) {
            return;
        }
        final int vectorSize = vectors[0].length;
        final int rowVectorCount = (int) Math.ceil(dims.getX() / (float) vectorSize);
        final int vectorCount = rowVectorCount * dims.getY();
        assert (vectors.length == vectorCount) : "Wrong vector count in reconstruct.";

        int vec = 0;
        int dstX;
        for (int dstY = 0; dstY < dims.getY(); dstY++) {
            for (int vecIndex = 0; vecIndex < rowVectorCount; vecIndex++) {
                for (int x = 0; x < vectorSize; x++) {
                    dstX = (vecIndex * vectorSize) + x;
                    if (isInside(dstX, dstY)) {
                        data[index(dstX, dstY)] = vectors[vec][x];
                    }
                }
                ++vec;
            }
        }
    }

    /**
     * Reconstruct this Chunk (copy data) from matrix vectors.
     *
     * @param vectors     Matrix vector data.
     * @param qVectorDims Matrix dimensions.
     */
    public void reconstructFrom2DVectors(final int[][] vectors, final V2i qVectorDims) {
        final int xSize = dims.getX();
        final int ySize = dims.getY();

        final int chunkXSize = qVectorDims.getX();
        final int chunkYSize = qVectorDims.getY();

        int vecIndex = 0;

        for (int chunkYOffset = 0; chunkYOffset < ySize; chunkYOffset += chunkYSize) {
            for (int chunkXOffset = 0; chunkXOffset < xSize; chunkXOffset += chunkXSize) {
                copyDataFromVector(vectors[vecIndex++], qVectorDims, chunkXOffset, chunkYOffset);
            }
        }
        assert (vecIndex == vectors.length);
    }

    /**
     * Calculate the number of required 2D matrices for plane of given dimensions.
     *
     * @param imageDims Plane dimensions.
     * @param chunkDims Matrix dimension.
     * @return Number of required chunks.
     */
    public static int calculateRequiredChunkCount(final V2i imageDims, final V2i chunkDims) {
        final int xChunkCount = (int) Math.ceil((double) imageDims.getX() / (double) chunkDims.getX());
        final int yChunkCount = (int) Math.ceil((double) imageDims.getY() / (double) chunkDims.getY());

        return (xChunkCount * yChunkCount);
    }

    /**
     * Check if point [x,y] is inside this chunk boundaries.
     *
     * @param x X coordinate (width).
     * @param y Y coordinate (height).
     * @return True if point is inside.
     */
    private boolean isInside(final int x, final int y) {
        return (((x >= 0) && (x < dims.getX())) && (y >= 0) && (y < dims.getY()));
    }

    /**
     * Copy data from chunk vector to this chunk.
     *
     * @param vector       Chunk vector.
     * @param qVectorDims  Chunk dimensions.
     * @param chunkXOffset Chunk X offset.
     * @param chunkYOffset Chunk Y offset.
     */
    private void copyDataFromVector(final int[] vector,
                                    final V2i qVectorDims,
                                    final int chunkXOffset,
                                    final int chunkYOffset) {

        final int qVecYSize = qVectorDims.getY();
        final int qVecXSize = qVectorDims.getX();
        int dstX, dstY;

        for (int chunkY = 0; chunkY < qVecYSize; chunkY++) {
            dstY = chunkYOffset + chunkY;
            for (int chunkX = 0; chunkX < qVecXSize; chunkX++) {
                dstX = chunkXOffset + chunkX;
                if (!(dstX >= dims.getX() || dstY >= dims.getY())) {
                    setValueAt(dstX, dstY, vector[index(chunkX, chunkY, qVectorDims.getY())]);
                }
            }
        }
    }

    /**
     * Check whether two Chunk2D are equal in data.
     *
     * @param obj Other object
     * @return True if same.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Block) {
            final Block otherChunk = (Block) obj;
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

    /**
     * Get this chunk data.
     *
     * @return Data array.
     */
    public int[] getData() {
        return data;
    }

    /**
     * Convert Chunk2D to ImageU16 one-to-one.
     */
    public ImageU16 asImageU16() {
        return new ImageU16(dims.getX(), dims.getY(), data);
    }
}
