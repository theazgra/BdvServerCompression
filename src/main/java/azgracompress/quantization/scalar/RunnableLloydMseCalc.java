package azgracompress.quantization.scalar;

public class RunnableLloydMseCalc implements Runnable {
    final int[] trainingData;
    final int fromIndex;
    final int toIndex;
    final int[] centroids;
    final int[] boundaryPoints;
    final int codebookSize;
    double mse = 0.0;

    public RunnableLloydMseCalc(int[] trainingData, int fromIndex, int toIndex, int[] centroids, int[] boundaryPoints,
                                final int codebookSize) {
        this.trainingData = trainingData;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.centroids = centroids;
        this.boundaryPoints = boundaryPoints;
        this.codebookSize = codebookSize;
    }


    @Override
    public void run() {
        mse = 0.0;

        for (int i = fromIndex; i < toIndex; i++) {
            mse += Math.pow((double) trainingData[i] - (double) quantize(trainingData[i]), 2);
        }
    }

    public double getMse() {
        return mse;
    }

    private int quantize(final int value) {
        for (int intervalId = 1; intervalId <= codebookSize; intervalId++) {
            if ((value >= boundaryPoints[intervalId - 1]) && (value <= boundaryPoints[intervalId])) {
                return centroids[intervalId - 1];
            }
        }
        throw new RuntimeException("Value couldn't be quantized!");
    }
}
