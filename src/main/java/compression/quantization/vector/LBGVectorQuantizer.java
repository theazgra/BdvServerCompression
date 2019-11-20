package compression.quantization.vector;

import java.util.ArrayList;
import java.util.Random;

public class LBGVectorQuantizer {
    private final double EPSILON = 0.005;
    private final double PERTURBATION_FACTOR_MIN = 0.001; //     0.1 %
    private final double PERTURBATION_FACTOR_MAX = 0.200; //    20.0 %
    private final int vectorWidth;
    private final int vectorHeight;
    private final int codebookSize;
    private final int[] trainingData;
    private Random random = new Random();

    private ArrayList<ArrayList<Integer>> trainingVectors;

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

    public void findOptimalCodebook() {
        initializeTrainingVectors();
        ArrayList<CodebookEntry> codebook = initializeCodebook();
        System.out.println("FINISHED");
        //LBG(codebook);
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

    private ArrayList<Double> getPerturbationVector() {
        final int vecSize = (vectorWidth * vectorHeight);
        ArrayList<Double> prt = new ArrayList<>(vecSize);
        for (int i = 0; i < vecSize; i++) {
            final double rnd = PERTURBATION_FACTOR_MIN +
                    ((PERTURBATION_FACTOR_MAX - PERTURBATION_FACTOR_MIN) * random.nextDouble());
            prt.add(rnd);
        }
        return prt;
    }


    private ArrayList<CodebookEntry> initializeCodebook() {
        ArrayList<CodebookEntry> codebook = new ArrayList<>(codebookSize);
        // Initialize first codebook entry as average of training vectors
        int k = 1;
        ArrayList<Integer> initialEntry = CodebookEntry.vectorMean(trainingVectors);
        codebook.add(new CodebookEntry(initialEntry));

        while (k != codebookSize) {

            assert (codebook.size() == k);
            ArrayList<CodebookEntry> newCodebook = new ArrayList<>(k * 2);
            // Create perturbation vector.
            ArrayList<Double> prtV = getPerturbationVector();

            // TODO(Moravec):   Make sure that when we are splitting entry we don't end up creating two same entries.
            //                  The problem happens when we try to split Vector full of zeroes.
            // Split each entry in codebook with fixed perturbation vector.
            for (final CodebookEntry entryToSplit : codebook) {

                ArrayList<Integer> left = new ArrayList<>(prtV.size());
                ArrayList<Integer> right = new ArrayList<>(prtV.size());
                for (int j = 0; j < prtV.size(); j++) {

                    final int rVal = (int) Math.round(entryToSplit.getVector().get(j) * (1.0 + prtV.get(j)));
                    final int lVal = (int) Math.round(entryToSplit.getVector().get(j) * (1.0 - prtV.get(j)));

                    assert (rVal >= 0 && lVal >= 0) : "Vector value are negative!";
                    right.add(rVal);
                    left.add(lVal);
                }
                final CodebookEntry rightEntry = new CodebookEntry(right);
                final CodebookEntry leftEntry = new CodebookEntry(left);
                assert (!rightEntry.equals(leftEntry)) : "Split entry to two same entries!";
                newCodebook.add(rightEntry);
                newCodebook.add(leftEntry);
            }
            codebook = newCodebook;
            assert (codebook.size() == (k * 2));
            System.out.println(String.format("Split from %d -> %d", k, k * 2));
            k *= 2;
            // Execute LBG Algorithm on current codebook to improve it.
            LBG(codebook);
        }
        return codebook;
    }


    private double vectorDistance(final ArrayList<Integer> entry, final ArrayList<Integer> vector) {
        return euclidDistance(entry, vector);
    }

    private void LBG(ArrayList<CodebookEntry> codebook) {

        double previousDistortion = Double.POSITIVE_INFINITY;

        int iteration = 1;
        while (true) {

            // Step 1
            for (final ArrayList<Integer> trainingVec : trainingVectors) {
                double minDist = Double.POSITIVE_INFINITY;
                CodebookEntry closestEntry = null;
                for (CodebookEntry entry : codebook) {
                    double entryDistance = vectorDistance(entry.getVector(), trainingVec);
                    if (entryDistance < minDist) {
                        minDist = entryDistance;
                        closestEntry = entry;
                    }
                }
                if (closestEntry != null) {
                    closestEntry.addTrainingVector(trainingVec, minDist);
                }
            }

            fixEmptyEntries(codebook);

            // Step 2
            double avgDistortion = 0;
            for (CodebookEntry entry : codebook) {
                avgDistortion += entry.getAverageDistortion();
            }
            avgDistortion /= (double) codebook.size();

            // Step 3
            double dist = (previousDistortion - avgDistortion) / avgDistortion;
            System.out.println(String.format("It: %d Distortion: %.5f", iteration++, dist));
            if (dist < EPSILON) {
                break;
            }
            previousDistortion = avgDistortion;

            // Step 4
            for (CodebookEntry entry : codebook) {
                entry.calculateCentroid();
                entry.clearTrainingData();
            }
        }
    }


    private void fixEmptyEntries(final ArrayList<CodebookEntry> codebook) {
        final int originalSize = codebook.size();
        final ArrayList<CodebookEntry> fixedCodebook = new ArrayList<>(originalSize);

        CodebookEntry emptyEntry = null;
        for (final CodebookEntry potentionallyEmptyEntry : codebook) {
            if (potentionallyEmptyEntry.getTrainingVectors().size() == 0) {
                emptyEntry = potentionallyEmptyEntry;
            }
        }
        while (emptyEntry != null) {
            fixSingleEmptyEntry(codebook, emptyEntry);
            emptyEntry = null;
            for (final CodebookEntry potentionallyEmptyEntry : codebook) {
                if (potentionallyEmptyEntry.getTrainingVectors().size() == 0) {
                    emptyEntry = potentionallyEmptyEntry;
                }
            }
        }
    }

    private void fixSingleEmptyEntry(ArrayList<CodebookEntry> codebook, final CodebookEntry emptyEntry) {
        System.out.println("****** FOUND EMPTY ENTRY ******");
        // Remove empty entry from codebook.
        codebook.remove(emptyEntry);

        // Find biggest partition.
        CodebookEntry biggestPartition = emptyEntry;
        for (final CodebookEntry entry : codebook) {
            if (entry.getTrainingVectors().size() > biggestPartition.getTrainingVectors().size()) {
                biggestPartition = entry;
            }
        }
        // Assert that we have found some.
        assert (biggestPartition.getTrainingVectors().size() > 0) : "Biggest partitions was empty before split";

        // Choose random trainingVector from biggest partition and set it as new entry.
        int randomIndex = new Random().nextInt(biggestPartition.getTrainingVectors().size());
        CodebookEntry newEntry = new CodebookEntry(biggestPartition.getTrainingVectors().get(randomIndex));
        // Add new entry to the codebook.
        codebook.add(newEntry);
        // Remove that vector from training vectors of biggest partition
        biggestPartition.removeTrainingVectorAndDistance(randomIndex);

        // Redistribute biggest partition training vectors
        final ArrayList<ArrayList<Integer>> trainingVectors = (ArrayList<ArrayList<Integer>>) biggestPartition.getTrainingVectors().clone();
        biggestPartition.clearTrainingData();
        newEntry.clearTrainingData();

        for (final ArrayList<Integer> trVec : trainingVectors) {
            double originalPartitionDist = vectorDistance(biggestPartition.getVector(), trVec);
            double newEntryDist = vectorDistance(newEntry.getVector(), trVec);
            if (originalPartitionDist < newEntryDist) {
                biggestPartition.addTrainingVector(trVec, originalPartitionDist);
            } else {
                newEntry.addTrainingVector(trVec, newEntryDist);
            }
        }

        assert (biggestPartition.getTrainingVectors().size() > 0) : "Biggest partition is empty";
        assert (newEntry.getTrainingVectors().size() > 0) : "New entry is empty";
    }

    private int euclidDistance(final ArrayList<Integer> x, final ArrayList<Integer> y) {
        double distance = 0;
        for (int i = 0; i < x.size(); i++) {
            distance += Math.pow((x.get(i) - y.get(i)), 2);
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
