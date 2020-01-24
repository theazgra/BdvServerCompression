package azgracompress.quantization.vector;

public class TrainingVector {
    final int[] vector;
    private int entryIndex = -1;
    private double entryDistance = Double.POSITIVE_INFINITY;

    public TrainingVector(int[] vector) {
        this.vector = vector;
    }

    public int[] getVector() {
        return vector;
    }

    public int getEntryIndex() {
        return entryIndex;
    }

    public void setEntryIndex(int entryIndex) {
        this.entryIndex = entryIndex;
    }

    public double getEntryDistance() {
        return entryDistance;
    }

    public void setEntryDistance(double entryDistance) {
        this.entryDistance = entryDistance;
    }
}
