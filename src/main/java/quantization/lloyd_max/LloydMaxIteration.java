package quantization.lloyd_max;

import java.util.Arrays;

public class LloydMaxIteration {
    private int m_interation;
    private double m_mse;
    private int[] m_centroids;

    public LloydMaxIteration(final int iter, final double mse, final int[] centroids) {
        m_interation = iter;
        m_mse = mse;
        m_centroids = Arrays.copyOf(centroids, centroids.length);
    }

    public int getIteration() {
        return m_interation;
    }

    public double getMse() {
        return m_mse;
    }

    public int[] getCentroids() {
        return m_centroids;
    }
}
