package azgracompress.data;

public class ImageU16Dataset {
    private final V2i planeDimensions;
    private final int planeCount;
    private final short[] data;

    public ImageU16Dataset(V2i planeDimensions, int planeCount, short[] data) {
        this.planeDimensions = planeDimensions;
        this.planeCount = planeCount;
        this.data = data;
    }

    public V2i getPlaneDimensions() {
        return planeDimensions;
    }

    public int getPlaneCount() {
        return planeCount;
    }

    public short[] getData() {
        return data;
    }
}
