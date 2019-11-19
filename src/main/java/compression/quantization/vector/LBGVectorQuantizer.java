package compression.quantization.vector;

import java.util.ArrayList;

public class LBGVectorQuantizer {
    private final double EPSILON = 0.0005;
    private final int vectorWidth;
    private final int vectorHeight;
    private final int codebookSize;
    private final int[] trainingData;

    private ArrayList<ArrayList<Integer>> trainingVectors;
    private ArrayList<ArrayList<Integer>> codebook;

    public LBGVectorQuantizer(final int[] trainingData, final int codeblockSize, final int vecWidth, final int vecHeight) {
        // NOTE(Moravec): This supports only `line` vector quantization, later we want to support blocks and boxes.
        assert (vecHeight == 1);

        // NOTE(Moravec): Also trainingData.length must be multiple of vecWidth.
        assert ((trainingData.length % vecWidth) == 0);

        this.vectorHeight = vecHeight;
        this.vectorWidth = vecWidth;
        this.codebookSize = codeblockSize;
        this.trainingData = trainingData;
    }

    private void initializeTrainingVectors() {
        final int initialCodebookSize = trainingData.length / vectorWidth;
        trainingVectors = new ArrayList<>(initialCodebookSize);
        for (int cbEntry = 0; cbEntry < initialCodebookSize; cbEntry++) {
            ArrayList<Integer> vecEntry = new ArrayList<>(vectorWidth);
            for (int i = 0; i < vectorWidth; i++) {
                vecEntry.add(trainingData[(cbEntry * vectorWidth) + i]);
            }
            trainingVectors.add(vecEntry);
        }
    }

    private ArrayList<Integer> getPerturbationVector(final ArrayList<Integer> entry) {
        ArrayList<Integer> prt = new ArrayList<>(entry.size());
        for (Integer element : entry) {
            prt.add((element / 2));
        }
        return prt;
    }


    private void initializeCodebook() {
        codebook.clear();
        // Initialize first codebook entry as average of training vectors
        ArrayList<Integer> initialEntry = getAverageOfVectors(trainingVectors);
        codebook.add(initialEntry);
        codebookIteration(codebookSize, initialEntry);
    }

    private void codebookIteration(final int level, final ArrayList<Integer> parentEntry) {
        if (level == 1) {
            return;
        }
        ArrayList<Integer> prtV = getPerturbationVector(parentEntry);
        ArrayList<Integer> left = new ArrayList<>(prtV.size());
        ArrayList<Integer> right = new ArrayList<>(prtV.size());
        for (int i = 0; i < prtV.size(); i++) {
            right.add(parentEntry.get(i) + prtV.get(i));
            left.add(parentEntry.get(i) - prtV.get(i));
        }
        codebook.add(left);
        codebook.add(right);

        // TODO (Moravec): Execute LBG Algorithm on current codebook to improve it and then recursively split them.

        codebookIteration(level / 2, left);
        codebookIteration(level / 2, right);
    }

    public void train() {
        initializeTrainingVectors();
        codebook = new ArrayList<>();
        System.out.println("got initial codebook");
    }

    private void optimize() {

    }


    private ArrayList<Integer> getAverageOfVectors(final ArrayList<ArrayList<Integer>> vectors) {
        final int vectorSize = vectorWidth * vectorHeight;
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


    private int euclidDistance(final ArrayList<Integer> x, final ArrayList<Integer> y, final int incrementFactor) {
        double distance = 0;
        for (int i = 0; i < x.size(); i++) {
            distance += Math.pow((x.get(i) - y.get(i) + incrementFactor), 2);
        }
        return (int) Math.sqrt(distance);
    }


    public int getVectorWidth() {
        return vectorWidth;
    }

    public int getVectorHeight() {
        return vectorHeight;
    }

    public int getCodebookSize() {
        return codebookSize;
    }
}
