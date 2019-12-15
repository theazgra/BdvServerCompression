package compression.quantization.scalar;

import compression.utilities.TypeConverter;

public class ScalarQuantizer {
    private final int min;
    private final int max;
    private int[] centroids;
    private int[] boundaryPoints;

    public ScalarQuantizer(final int min, final int max, final int[] centroids) {
        this.centroids = centroids;
        boundaryPoints = new int[centroids.length + 1];
        this.min = min;
        this.max = max;

        calculateBoundaryPoints();
    }

    public short[] quantize(short[] data) {
        short[] result = new short[data.length];
        for (int i = 0; i < data.length; i++) {
            final int intRepresentationOfValue = TypeConverter.shortToInt(data[i]);
            final int quantizedValue = quantize(intRepresentationOfValue);
            final short shortRepresentation = TypeConverter.intToShort(quantizedValue);
            result[i] = shortRepresentation;
        }
        return result;
    }

    private void calculateBoundaryPoints() {
        boundaryPoints[0] = min;
        boundaryPoints[centroids.length] = max;
        for (int j = 1; j < centroids.length; j++) {
            boundaryPoints[j] = (this.centroids[j] + this.centroids[j - 1]) / 2;
        }
    }

    public int quantize(final int value) {
        for (int intervalId = 1; intervalId <= centroids.length; intervalId++) {
            if ((value >= boundaryPoints[intervalId - 1]) && (value <= boundaryPoints[intervalId])) {
                return centroids[intervalId - 1];
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }

    public double getMse(final int[] data) {
        double mse = 0.0;
        for (int i = 0; i < data.length; i++) {
            int quantizedValue = quantize(data[i]);
            mse += Math.pow(((double) data[i] - (double) quantizedValue), 2);
        }
        mse /= (double) data.length;
        return mse;
    }

    public int[] getCentroids() {
        return centroids;
    }
}
