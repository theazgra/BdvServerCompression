package compression.data;

public class ImageU16 {

    private final int width;
    private final int height;
    private final short[] data;

    public ImageU16(int width, int height, short[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }
}
