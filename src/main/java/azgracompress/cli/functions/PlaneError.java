package azgracompress.cli.functions;

public class PlaneError {
    private final int planeIndex;
    private final double absoluteError;
    private final double meanAbsoluteError;

    public PlaneError(int planeIndex, double absoluteError, double meanAbsoluteError) {
        this.planeIndex = planeIndex;
        this.absoluteError = absoluteError;
        this.meanAbsoluteError = meanAbsoluteError;
    }

    public int getPlaneIndex() {
        return planeIndex;
    }

    public double getAbsoluteError() {
        return absoluteError;
    }

    public double getMeanAbsoluteError() {
        return meanAbsoluteError;
    }
}
