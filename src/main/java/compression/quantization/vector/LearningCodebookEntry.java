package compression.quantization.vector;

import compression.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LearningCodebookEntry extends CodebookEntry {
    private ArrayList<Double> trainingVectorsDistances;
    private ArrayList<ArrayList<Integer>> trainingVectors;

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
//        // TODO(Moravec): Is this correct way of doing it?
//        if (trainingVectors.size() == 0) {
//            return 0.0;
//        }
        double totalDistortion = Utils.arrayListSum(trainingVectorsDistances);
        return (totalDistortion / (double) trainingVectors.size());
    }

    public ArrayList<Integer> getVectorAsArrayList() {
        return Arrays.stream(vector).boxed().collect(Collectors.toCollection(ArrayList::new));
    }


    public ArrayList<ArrayList<Integer>> getTrainingVectors() {
        return trainingVectors;
    }

    public void setTrainingVectors(ArrayList<ArrayList<Integer>> trainingVectors) {
        this.trainingVectors = trainingVectors;
    }

    public void addTrainingVector(final ArrayList<Integer> trainingVec, final double vecDist) {
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
            ArrayList<Integer> mean = vectorMean(trainingVectors);
            assert (mean.size() == vector.length) : "Mismatched collection sizes";
            for (int i = 0; i < vector.length; i++) {
                vector[i] = mean.get(i);
            }
        }
    }

    public static ArrayList<Integer> vectorMean(final ArrayList<ArrayList<Integer>> vectors) {
        final int vectorSize = vectors.get(0).size();
        double[] vectorSum = new double[vectorSize];

        for (ArrayList<Integer> quantizationVector : vectors) {
            for (int i = 0; i < vectorSize; i++) {
                vectorSum[i] += (double) quantizationVector.get(i);
            }
        }

        ArrayList<Integer> average = new ArrayList<>(vectorSize);
        for (double sum : vectorSum) {
            average.add((int) Math.round(sum / (double) vectors.size()));
        }
        return average;
    }

    public void removeTrainingVectorAndDistance(int index) {
        trainingVectors.remove(index);
        trainingVectorsDistances.remove(index);
    }
}
