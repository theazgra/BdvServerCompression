package azgracompress.quantization.scalar;

import azgracompress.U16;

public class ScalarQuantizer {
    private final int min;
    private final int max;
    private final SQCodebook codebook;
    private int[] boundaryPoints;

    public ScalarQuantizer(final int min, final int max, final SQCodebook codebook) {
        this.codebook = codebook;
        boundaryPoints = new int[codebook.getCodebookSize() + 1];
        this.min = min;
        this.max = max;

        calculateBoundaryPoints();
    }

    public ScalarQuantizer(final SQCodebook codebook) {
        this(U16.Min, U16.Max, codebook);
    }

    public int[] quantize(int[] data) {
        int[] result = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = quantize(data[i]);
        }
        return result;
    }

    public int[] quantizeIntoIndices(int[] data, final int maxWorkerCount) {
        int[] indices = new int[data.length];
        // Speedup?
        if (maxWorkerCount == 1) {
            for (int i = 0; i < data.length; i++) {
                final int index = quantizeIndex(data[i]);
                indices[i] = index;
            }
        } else {
            // NOTE(Moravec): This function is fast enough single thread. So we use max 2 threads.
            final int workerCount = Math.min(maxWorkerCount, 2);
            Thread[] workers = new Thread[workerCount];
            final int workSize = data.length / workerCount;

            for (int wId = 0; wId < workerCount; wId++) {
                final int fromIndex = wId * workSize;
                final int toIndex = (wId == workerCount - 1) ? data.length : (workSize + (wId * workSize));

                workers[wId] = new Thread(() -> {
                    for (int vectorIndex = fromIndex; vectorIndex < toIndex; vectorIndex++) {
                        indices[vectorIndex] = quantizeIndex(data[vectorIndex]);
                    }
                });

                workers[wId].start();
            }
            try {
                for (int wId = 0; wId < workerCount; wId++) {
                    workers[wId].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return indices;
    }

    private void calculateBoundaryPoints() {
        boundaryPoints[0] = min;
        boundaryPoints[codebook.getCodebookSize()] = max;
        final int[] centroids = codebook.getCentroids();
        for (int j = 1; j < centroids.length; j++) {
            boundaryPoints[j] = (centroids[j] + centroids[j - 1]) / 2;
        }
    }

    public int quantizeIndex(final int value) {
        for (int intervalId = 1; intervalId <= codebook.getCodebookSize(); intervalId++) {
            if ((value >= boundaryPoints[intervalId - 1]) && (value <= boundaryPoints[intervalId])) {
                return (intervalId - 1);
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }

    public int quantize(final int value) {
        return codebook.getCentroids()[quantizeIndex(value)];
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

    public SQCodebook getCodebook() {
        return codebook;
    }
}
