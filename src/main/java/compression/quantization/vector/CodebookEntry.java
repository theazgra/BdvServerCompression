package compression.quantization.vector;

import compression.utilities.Utils;

import java.util.ArrayList;


public class CodebookEntry {

    private ArrayList<Integer> vector;
    private ArrayList<Double> trainingVectorsDistances;
    private ArrayList<ArrayList<Integer>> trainingVectors;

    public CodebookEntry(ArrayList<Integer> codebook) {
        this.vector = codebook;
        trainingVectors = new ArrayList<>();
        trainingVectorsDistances = new ArrayList<>();
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

    public ArrayList<Integer> getVector() {
        return vector;
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
            vector = vectorMean(trainingVectors);
        }
    }

    public static ArrayList<Integer> vectorMean(final ArrayList<ArrayList<Integer>> vectors) {
//        if (vectors.size() == 0) {
//
//        }
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
