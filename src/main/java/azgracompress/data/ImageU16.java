package azgracompress.data;

import azgracompress.utilities.Utils;

public class ImageU16 {

    private final int width;
    private final int height;
    private int[] data;

    public ImageU16(final int width, final int height, final int[] data) {
        assert ((width * height) == data.length) : "Wrong data size in ImageU16 constructor.";
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public ImageU16(final V2i dims, final int[] data) {
        this(dims.getX(), dims.getY(), data);
    }

    private int index(final int x, final int y) {
        assert ((x >= 0 && x < height) && (y >= 0 && y < width)) : "Index out of bounds";
        return (x * width) + y;
    }

    public Block as2dChunk() {
        return new Block(new V2i(width, height), data);
    }

    public ImageU16 difference(final ImageU16 other) {
        assert (width == other.width && height == other.height) : "Different image dimensions in difference()";
        final int[] diffData = Utils.getDifference(data, other.getData());
        return new ImageU16(width, height, diffData);
    }

    public int getValueAt(final int x, final int y) {
        return data[index(x, y)];
    }

    public void setValueAt(final int x, final int y, final short value) {
        data[index(x, y)] = value;
    }

    public int[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

//    /**
//     * Chunk the image data into quantization vectors of requested dimension.
//     *
//     * @param qVectorDims Quantization vector dimension.
//     * @return Array of quantization vectors.
//     */
//    public int[][] toQuantizationVectors(final V2i qVectorDims) {
//        if (qVectorDims.getY() == 1) {
//            // 1D row vectors.
//            return as2dChunk().divideInto1DVectors(qVectorDims.getX());
//        } else {
//            // 2D matrix vectors.
//            return as2dChunk().divideInto2DVectors(qVectorDims);
//            //return Chunk2D.chunksAsImageVectors(as2dChunk().divideIntoChunks(qVectorDims));
//        }
//    }
}
