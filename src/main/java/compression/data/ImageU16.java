package compression.data;

public class ImageU16 {

    private final int width;
    private final int height;
    private short[] data;

    public ImageU16(int width, int height, short[] data) {
        assert ((width * height) == data.length) : "Wrong data size in ImageU16 constructor.";
        this.width = width;
        this.height = height;
        this.data = data;
    }

    private int index(final int x, final int y) {
        assert ((x >= 0 && x < height) && (y >= 0 && y < width)) : "Index out of bounds";
        return (x * width) + y;
    }

    public short getValueAt(final int x, final int y) {
        return data[index(x, y)];
    }

    public void setValueAt(final int x, final int y, final short value) {
        data[index(x, y)] = value;
    }

    public short[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Chunk2D as2dChunk() {
        return new Chunk2D(new V2i(width, height), new V2l(0, 0), data);
    }
}
