package compression.data;

import compression.utilities.Utils;

public class Chunk2D {
    private final int[] data;

    private final V2i dims;
    private final V2l offset;

    public Chunk2D(final V2i dims, final V2l offset, final int[] data) {
        this.dims = dims;
        this.data = data;
        this.offset = offset;
        assert (data.length == (dims.getX() * dims.getY())) : "Wrong box data.";
    }

    public Chunk2D(final V2i chunkDdims, final V2l offset, final short[] data) {
        this(chunkDdims, offset, Utils.convertShortArrayToIntArray(data));
    }

    public Chunk2D(final V2i chunkDdims, final V2l offset) {
        this(chunkDdims, offset, new int[chunkDdims.getX() * chunkDdims.getY()]);
    }

    /**
     * Calculate the index inside data array.
     *
     * @param x Zero based x coordinate.
     * @param y Zero based y coordinate.
     * @return Index inside data array.
     */
    private int index(final int x, final int y) {
        assert (x >= 0 && x < dims.getX()) : "Index X out of bounds.";
        assert (y >= 0 && y < dims.getY()) : "Index Y out of bounds.";

        return (x * dims.getY()) + y;
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
        if (((x < 0) || (x >= chunkDims.getX())) || (y < 0) || (y >= chunkDims.getY())) {
            throw new IndexOutOfBoundsException("One of index x,y is out of bounds of the 2D shape");
        }

        return (x * chunkDims.getY()) + y;
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

    public V2l getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return String.format("2D shape %s %d values", dims.toString(), data.length);
    }

    public Chunk2D[] divideIntoChunks(final V2i chunkDims) {

        final int xSize = dims.getX();
        final int ySize = dims.getY();

        final int chunkCount = getRequiredChunkCount(chunkDims);

        Chunk2D[] chunks = new Chunk2D[chunkCount];
        int chunkIndex = 0;

        for (int chunkYOffset = 0; chunkYOffset < ySize; chunkYOffset += chunkDims.getY()) {
            for (int chunkXOffset = 0; chunkXOffset < xSize; chunkXOffset += chunkDims.getX()) {
                chunks[chunkIndex++] = copyToChunk(chunkDims, new V2i(chunkXOffset, chunkYOffset));
            }
        }
        return chunks;
    }

    private int getRequiredChunkCount(final V2i chunkDims) {
        final int xChunkCount = (int) Math.ceil(dims.getX() / (double) chunkDims.getX());
        final int yChunkCount = (int) Math.ceil(dims.getY() / (double) chunkDims.getY());

        return (xChunkCount * yChunkCount);
    }

    public void reconstructFromChunks(final Chunk2D[] chunks) {
        assert (chunks.length > 0) : "No chunks in reconstruct";
        final V2i chunkDims = chunks[0].getDims();

        assert (getRequiredChunkCount(chunkDims) == chunks.length) : "Wrong chunk count in reconstruct";

        for (final Chunk2D chunk : chunks) {
            copyFromChunk(chunk);
        }
    }


    private boolean isInside(final int x, final int y) {
        return (((x >= 0) && (x < dims.getX())) && (y >= 0) && (y < dims.getY()));
    }

    private void copyFromChunk(final Chunk2D chunk) {

        final V2i chunkDims = chunk.getDims();
        final V2l localOffset = chunk.getOffset();
        int dstX, dstY;

        for (int chunkY = 0; chunkY < chunkDims.getY(); chunkY++) {
            dstY = (int) localOffset.getY() + chunkY;
            for (int chunkX = 0; chunkX < chunkDims.getX(); chunkX++) {
                dstX = (int) localOffset.getX() + chunkX;

                // NOTE(Moravec):   Negating this expression!
                //                  If dst coordinates are NOT outside bounds, copy the value.
                if (!(dstX >= dims.getX() || dstY >= dims.getY())) {
                    setValueAt(dstX, dstY, chunk.getValueAt(chunkX, chunkY));
                }
            }
        }
    }

    private Chunk2D copyToChunk(final V2i chunkDims, final V2i chunkOffset) {
        int[] chunkData = new int[(chunkDims.getX() * chunkDims.getY())];
        final int FILL_VALUE = 0;
        int srcX, srcY;

        for (int y = 0; y < chunkDims.getY(); y++) {
            srcY = chunkOffset.getY() + y;
            for (int x = 0; x < chunkDims.getX(); x++) {
                srcX = chunkOffset.getX() + x;
                final int dstIndex = index(x, y, chunkDims);
                chunkData[dstIndex] = isInside(srcX, srcY) ? data[index(srcX, srcY)] : FILL_VALUE;
            }
        }

        // NOTE(Moravec):   We will save only local offset inside current box, which will be used
        //                  to reconstruct the original box.
        return new Chunk2D(chunkDims, chunkOffset.toV2l(), chunkData);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Chunk2D) {
            final Chunk2D otherChunk = (Chunk2D) obj;
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

    public int[] getData() {
        return data;
    }
}
