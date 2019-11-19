package compression.quantization.scalar;

import java.util.Arrays;

public class LloydMaxIteration {
    private int m_interation;
    private double m_mse;
    private double m_psnr;
    private int[] m_centroids;

    public LloydMaxIteration(final int iter, final double mse, final double psnr, final int[] centroids) {
        m_interation = iter;
        m_mse = mse;
        m_psnr = psnr;
        m_centroids = Arrays.copyOf(centroids, centroids.length);
    }

    public int getIteration() {
        return m_interation;
    }

    public double getMse() {
        return m_mse;
    }

    public double getPsnr() {
        return m_psnr;
    }

    public int[] getCentroids() {
        return m_centroids;
    }
}
