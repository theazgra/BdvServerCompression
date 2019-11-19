package compression.quantization.scalar;

public class Quantizer implements Runnable {
    private int[] m_centroids;
    private int[] m_boundaryPoints;
    private double m_mse = 0.0;

    private int[] m_trainData = null;

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

    public void setThreadTrainData(final int[] data) {
        m_trainData = data;
    }

    @Override
    public void run() {
        if (m_trainData == null) {
            throw new RuntimeException("Train data weren't set.");
        }
        m_mse = getMse(m_trainData);
    }

    public double getMse() {
        return m_mse;
    }
}
