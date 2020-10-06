package cz.it4i.qcmp.cli.functions;

public class PlaneError {
    private final int planeIndex;
    private final double absoluteError;
    private final double meanAbsoluteError;

    public PlaneError(final int planeIndex, final double absoluteError, final double meanAbsoluteError) {
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
