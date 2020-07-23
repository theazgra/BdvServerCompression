package azgracompress.data;

public class ImageU16Dataset {
    private final V2i planeDimensions;
    private final int planeCount;
    private final short[][] data;

    public ImageU16Dataset(V2i planeDimensions, int planeCount, short[][] planeData) {
        this.planeDimensions = planeDimensions;
        this.planeCount = planeCount;
        this.data = planeData;
    }

    public V2i getPlaneDimensions() {
        return planeDimensions;
    }

    public int getPlaneCount() {
        return planeCount;
    }

    public short[] getPlaneData(final int planeIndex) {
        assert (planeIndex >= 0);
        assert (planeIndex < data.length);
        return data[planeIndex];
    }
}
