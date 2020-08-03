package azgracompress.data;

public class Chunk2D {
    private final int FILL_VALUE = 0;
    private int[] data;

    private final V2i dims;

    /**
     * Create the Chunk2D of given dimensions, offset and initialize it with data.
     *
     * @param dims Dimensions of the chunk.
     * @param data Chunk data.
     */
    public Chunk2D(final V2i dims, final int[] data) {
        this.dims = dims;
        this.data = data;
        assert (data.length == (dims.getX() * dims.getY())) : "Wrong box data.";
    }

    /**
     * Create Chunk2D of given dimensions and offset. Allocates the data.
     *
     * @param dims Dimensions of the chunk.
     */
    public Chunk2D(final V2i dims) {
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
        return index(x, y, dims);
    }

    /**
     * Calculate the index inside array of dimensions specified by chunkDims.
     *
     * @param x         Zero based x coordinate.
     * @param y         Zero based y coordinate.
     * @param chunkDims Chunk dimensions.
     * @return Index inside chunk dimension data array.
     */
    private int index(final int x, final int y, final V2i chunkDims) {
        assert (x >= 0 && x < chunkDims.getX()) : "Index X out of bounds.";
        assert (y >= 0 && y < chunkDims.getY()) : "Index Y out of bounds.";
        return (y * chunkDims.getX()) + x;
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
     * Divide this Chunk to 1D row vector of given length.
     *
     * @param vectorSize Vector length.
     * @return Array of row vectors.
     */
    public int[][] divideInto1DVectors(final int vectorSize) {
        final int rowVectorCount = (int) Math.ceil(dims.getX() / (float) vectorSize);
        final int vectorCount = rowVectorCount * dims.getY();
        int[][] imageVectors = new int[vectorCount][vectorSize];

        int vec = 0;
        int srcX;
        for (int row = 0; row < dims.getY(); row++) {
            for (int vecIndex = 0; vecIndex < rowVectorCount; vecIndex++) {
                for (int x = 0; x < vectorSize; x++) {
                    srcX = (vecIndex * vectorSize) + x;
                    imageVectors[vec][x] = isInside(srcX, row) ? data[index(srcX, row)] : FILL_VALUE;
                }
                ++vec;
            }
        }

        return imageVectors;
    }

    /**
     * Reconstruct this Chunk from array of 1D row vectors.
     *
     * @param vectors Array of 1D row vectors.
     */
    public void reconstructFromVectors(int[][] vectors) {
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
     * Divide this Chunk to 2D matrices of given dimensions.
     *
     * @param qVectorDims Matrix dimension.
     * @return Array of matrix data.
     */
    public int[][] divideInto2DVectors(final V2i qVectorDims) {
        final int chunkSize = qVectorDims.getX() * qVectorDims.getY();
        final int chunkCount = calculateRequiredChunkCount(qVectorDims);

        int[][] vectors = new int[chunkCount][chunkSize];
        int vecIndex = 0;

        for (int chunkYOffset = 0; chunkYOffset < dims.getY(); chunkYOffset += qVectorDims.getY()) {
            for (int chunkXOffset = 0; chunkXOffset < dims.getX(); chunkXOffset += qVectorDims.getX()) {
                copyDataToVector(vectors[vecIndex++], qVectorDims, chunkXOffset, chunkYOffset);
            }
        }
        return vectors;
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
     * Calculate the number of required 2D matrices for plane.
     *
     * @param chunkDims Matrix dimension.
     * @return Number of required chunks.
     */
    private int calculateRequiredChunkCount(final V2i chunkDims) {
        return calculateRequiredChunkCount(dims, chunkDims);
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
     * Copy this chunk data to chunk vector.
     *
     * @param vector       Chunk vector.
     * @param qVectorDims  Dimensions of the vector.
     * @param chunkXOffset Chunk X offset
     * @param chunkYOffset Chunk Y offset.
     */
    private void copyDataToVector(int[] vector, final V2i qVectorDims, final int chunkXOffset, final int chunkYOffset) {
        int srcX, srcY;
        for (int y = 0; y < qVectorDims.getY(); y++) {
            srcY = chunkYOffset + y;
            for (int x = 0; x < qVectorDims.getX(); x++) {
                srcX = chunkXOffset + x;
                final int dstIndex = index(x, y, qVectorDims);
                vector[dstIndex] = isInside(srcX, srcY) ? data[index(srcX, srcY)] : FILL_VALUE;
            }
        }
    }

    /**
     * Copy data from chunk vector to this chunk.
     *
     * @param vector       Chunk vector.
     * @param qVectorDims  Chunk dimensions.
     * @param chunkXOffset Chunk X offset.
     * @param chunkYOffset Chunk Y offset.
     */
    private void copyDataFromVector(int[] vector,
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
                    setValueAt(dstX, dstY, vector[index(chunkX, chunkY, qVectorDims)]);
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
    public boolean equals(Object obj) {
        if (obj instanceof Chunk2D) {
            final Chunk2D otherChunk = (Chunk2D) obj;
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
