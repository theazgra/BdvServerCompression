package compression.quantization.vector;

import compression.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LearningCodebookEntry extends CodebookEntry {
    private ArrayList<Double> trainingVectorsDistances;
    private ArrayList<int[]> trainingVectors;

    public LearningCodebookEntry(int[] codebook) {
        super(codebook);
        trainingVectors = new ArrayList<>();
        trainingVectorsDistances = new ArrayList<>();
    }

    public LearningCodebookEntry(final ArrayList<Integer> codebook) {
        this(codebook.stream().mapToInt(i -> i).toArray());
    }

    public double getAverageDistortion() {
        assert (trainingVectors.size() == trainingVectorsDistances.size());
        assert (trainingVectors.size() > 0) : "Empty entry!";
        double totalDistortion = Utils.arrayListSum(trainingVectorsDistances);
        return (totalDistortion / (double) trainingVectors.size());
    }

    public ArrayList<Integer> getVectorAsArrayList() {
        return Arrays.stream(vector).boxed().collect(Collectors.toCollection(ArrayList::new));
    }


    public ArrayList<int[]> getTrainingVectors() {
        return trainingVectors;
    }

    public void setTrainingVectors(ArrayList<int[]> trainingVectors) {
        this.trainingVectors = trainingVectors;
    }

    public void addTrainingVector(final int[] trainingVec, final double vecDist) {
        trainingVectors.add(trainingVec);
        trainingVectorsDistances.add(vecDist);
    }

    public void clearTrainingData() {
        trainingVectors.clear();
        trainingVectorsDistances.clear();
    }

    public void calculateCentroid() {
        // If we dont have any training vectors we cannot recalculate the centroid.
        if (trainingVectors.size() > 0) {
            ArrayList<Integer> mean = vectorMean2(trainingVectors.stream(),
                                                  trainingVectors.size(),
                                                  trainingVectors.get(0).length);
            assert (mean.size() == vector.length) : "Mismatched collection sizes";
            for (int i = 0; i < vector.length; i++) {
                vector[i] = mean.get(i);
            }
        }
    }

    public static ArrayList<Integer> vectorMean2(final Stream<int[]> vectorStream,
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

    //    public static ArrayList<Integer> vectorMean(final int[][] vectors) {
    //        final int vectorSize = vectors[0].length;
    //        double[] vectorSum = new double[vectorSize];
    //
    //        for (final int[] vector : vectors) {
    //            for (int vecIndex = 0; vecIndex < vectorSize; vecIndex++) {
    //                vectorSum[vecIndex] += (double) vector[vecIndex];
    //            }
    //        }
    //
    //        ArrayList<Integer> average = new ArrayList<>(vectorSize);
    //        for (double sum : vectorSum) {
    //            average.add((int) Math.round(sum / (double) vectors.length));
    //        }
    //        return average;
    //    }

    public void removeTrainingVectorAndDistance(int index) {
        trainingVectors.remove(index);
        trainingVectorsDistances.remove(index);
    }
}
