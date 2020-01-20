package azgracompress.data;

import azgracompress.U16;

public class ImageU16 {

    private final int width;
    private final int height;
    private int[] data;

    public ImageU16(int width, int height, int[] data) {
        assert ((width * height) == data.length) : "Wrong data size in ImageU16 constructor.";
        this.width = width;
        this.height = height;
        this.data = data;
    }

    private int index(final int x, final int y) {
        assert ((x >= 0 && x < height) && (y >= 0 && y < width)) : "Index out of bounds";
        return (x * width) + y;
    }

    public Chunk2D as2dChunk() {
        return new Chunk2D(new V2i(width, height), new V2l(0, 0), data);
    }

    public ImageU16 difference(final ImageU16 other) {
        assert (width == other.width && height == other.height) : "Different image dimensions in difference()";
        int[] diffData = new int[data.length];
        int diffVal;
        for (int i = 0; i < data.length; i++) {
            diffVal = Math.abs(data[i] - other.data[i]);
            assert (diffVal >= 0 && diffVal <= U16.Max) : "Diff value can not be converted to short.";
            diffData[i] = diffVal;
        }
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
}
