package azgracompress.quantization.scalar;

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

    public int[] quantize(int[] data) {
        int[] result = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = quantize(data[i]);
        }
        return result;
    }

    public int[] quantizeIntoIndices(int[] data) {
        int[] indices = new int[data.length];
        // Speedup?
        for (int i = 0; i < data.length; i++) {
            final int index = quantizeIndex(data[i]);
            indices[i] = index;
        }
        return indices;
    }

    private void calculateBoundaryPoints() {
        boundaryPoints[0] = min;
        boundaryPoints[centroids.length] = max;
        for (int j = 1; j < centroids.length; j++) {
            boundaryPoints[j] = (this.centroids[j] + this.centroids[j - 1]) / 2;
        }
    }

    public int quantizeIndex(final int value) {
        for (int intervalId = 1; intervalId <= centroids.length; intervalId++) {
            if ((value >= boundaryPoints[intervalId - 1]) && (value <= boundaryPoints[intervalId])) {
                return (intervalId - 1);
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }

    public int quantize(final int value) {
        return centroids[quantizeIndex(value)];
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
