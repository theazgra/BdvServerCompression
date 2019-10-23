package quantization;

public class Quantizer {
    private int[] m_centroids;
    private int[] m_boundaryPoints;

    public Quantizer(final int min, final int max, final int[] centroids) {
        m_centroids = centroids;
        m_boundaryPoints = new int[centroids.length + 1];

        m_boundaryPoints[0] = min;
        m_boundaryPoints[centroids.length] = max;

        for (int j = 1; j < centroids.length; j++) {
            m_boundaryPoints[j] = (m_centroids[j] + m_centroids[j - 1]) / 2;
        }
    }

    public int quantize(final int value) {
        for (int intervalId = 1; intervalId <= m_centroids.length; intervalId++) {
            if ((value >= m_boundaryPoints[intervalId - 1]) && (value < m_boundaryPoints[intervalId])) {
                return m_centroids[intervalId - 1];
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
}
