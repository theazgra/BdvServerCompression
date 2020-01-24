package azgracompress.quantization.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LearningCodebookEntry extends CodebookEntry {
    //    private ArrayList<Double> trainingVectorsDistances;
    //    private ArrayList<int[]> trainingVectors;

    private double averageDistortion = -1.0f;
    private int vectorCount = -1;
    private double[] perturbationVector;

    public LearningCodebookEntry(int[] codebook) {
        super(codebook);
        //        trainingVectors = new ArrayList<>();
        //        trainingVectorsDistances = new ArrayList<>();
    }

    public LearningCodebookEntry(final ArrayList<Integer> codebook) {
        this(codebook.stream().mapToInt(i -> i).toArray());
    }

    public void setAverageDistortion(final double averageDistortion) {
        this.averageDistortion = averageDistortion;
    }

    public void setVectorCount(final int vectorCount) {
        this.vectorCount = vectorCount;
    }

    public double[] getPerturbationVector() {
        return perturbationVector;
    }

    public void setCentroid(final int[] centroid) {
        assert (centroid.length == this.vector.length);
        System.arraycopy(centroid, 0, this.vector, 0, centroid.length);
    }

    public double getAverageDistortion() {
        return averageDistortion;
    }

    public int getVectorCount() {
        return vectorCount;
    }

    public void setPerturbationVector(double[] perturbationVector) {
        this.perturbationVector = perturbationVector;
    }

    //    public double getAverageDistortion() {
    //        assert (trainingVectors.size() == trainingVectorsDistances.size());
    //        assert (trainingVectors.size() > 0) : "Empty entry!";
    //        double totalDistortion = Utils.arrayListSum(trainingVectorsDistances);
    //        return (totalDistortion / (double) trainingVectors.size());
    //    }

    //    public ArrayList<int[]> getTrainingVectors() {
    //        return trainingVectors;
    //    }

    public ArrayList<Integer> getVectorAsArrayList() {
        return Arrays.stream(vector).boxed().collect(Collectors.toCollection(ArrayList::new));
    }


    //
    //    public void setTrainingVectors(ArrayList<int[]> trainingVectors) {
    //        this.trainingVectors = trainingVectors;
    //    }

    //    public void addTrainingVector(final int[] trainingVec, final double vecDist) {
    //        trainingVectors.add(trainingVec);
    //        trainingVectorsDistances.add(vecDist);
    //    }
    //
    //    public void clearTrainingData() {
    //        trainingVectors.clear();
    //        trainingVectorsDistances.clear();
    //    }

    //    public void calculateCentroid() {
    //        // If we dont have any training vectors we cannot recalculate the centroid.
    //        if (trainingVectors.size() > 0) {
    //            ArrayList<Integer> mean = vectorMean(trainingVectors.stream(),
    //                                                 trainingVectors.size(),
    //                                                 trainingVectors.get(0).length);
    //            assert (mean.size() == vector.length) : "Mismatched collection sizes";
    //            for (int i = 0; i < vector.length; i++) {
    //                vector[i] = mean.get(i);
    //                assert (vector[i] >= 0) : "Centroid value is too low";
    //                assert (vector[i] <= U16.Max) : "Centroid value is too big";
    //            }
    //        }
    //    }

    public static ArrayList<Integer> vectorMean(final Stream<int[]> vectorStream,
                                                final int vectorCount,
                                                final int vectorSize) {
        double[] vectorSum = new double[vectorSize];

        vectorStream.forEach(vector -> {
            for (int i = 0; i < vectorSize; i++) {
                vectorSum[i] += (double) vector[i];
            }
        });

        ArrayList<Integer> average = new ArrayList<>(vectorSize);
        for (double sum : vectorSum) {
            average.add((int) Math.round(sum / (double) vectorCount));
        }
        return average;
    }

    //    public void removeTrainingVectorAndDistance(int index) {
    //        trainingVectors.remove(index);
    //        trainingVectorsDistances.remove(index);
    //    }
    //
    //    public boolean isEmpty() {
    //
    //        return (trainingVectors.isEmpty());
    //    }
}
