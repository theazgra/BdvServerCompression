package compression.quantization.vector;

import compression.U16;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

public class LBGVectorQuantizer {
    private final double EPSILON = 0.005;
    private final double PERTURBATION_FACTOR_MIN = 0.10;
    private final double PERTURBATION_FACTOR_MAX = 0.60;
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
        ArrayList<LearningCodebookEntry> codebook = initializeCodebook();
        System.out.println("Got initial codebook. Improving codebook...");
        LBG(codebook, EPSILON * 0.1);
        System.out.println("Improved codebook.");
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
        return getPerturbationVector(PERTURBATION_FACTOR_MIN, PERTURBATION_FACTOR_MAX);
    }


    private ArrayList<Double> getPerturbationVector(final double minPerturbationFactor, final double maxPerturbationFactor) {
        final int vecSize = (vectorWidth * vectorHeight);
        ArrayList<Double> prt = new ArrayList<>(vecSize);
        for (int i = 0; i < vecSize; i++) {
            final double rnd = minPerturbationFactor +
                    ((maxPerturbationFactor - minPerturbationFactor) * random.nextDouble());
            prt.add(rnd);
        }
        return prt;
    }

    private void assertThatNewCodebookEntryIsOriginal(final ArrayList<LearningCodebookEntry> codebook, final LearningCodebookEntry newEntry) {

        for (final LearningCodebookEntry entry : codebook) {
            assert !(newEntry.equals(entry)) : "New entry is not original";
        }
    }

    private ArrayList<LearningCodebookEntry> initializeCodebook() {
        ArrayList<LearningCodebookEntry> codebook = new ArrayList<>(codebookSize);
        // Initialize first codebook entry as average of training vectors
        int k = 1;
        ArrayList<Integer> initialEntry = LearningCodebookEntry.vectorMean(trainingVectors);
        codebook.add(new LearningCodebookEntry(initialEntry));

        while (k != codebookSize) {

            assert (codebook.size() == k);
            ArrayList<LearningCodebookEntry> newCodebook = new ArrayList<>(k * 2);
            // Create perturbation vector.


            // TODO(Moravec):   Make sure that when we are splitting entry we don't end up creating two same entries.
            //                  The problem happens when we try to split Vector full of zeroes.
            // Split each entry in codebook with fixed perturbation vector.
            for (final LearningCodebookEntry entryToSplit : codebook) {
                ArrayList<Double> prtV = getPerturbationVector(0.2, 0.8);

                // We always want to carry zero vector to next iteration.
                if (entryToSplit.isZeroVector()) {
                    newCodebook.add(entryToSplit);

                    ArrayList<Integer> rndEntryValues = new ArrayList<>(prtV.size());
                    for (int j = 0; j < prtV.size(); j++) {
                        final int value = (int) Math.floor(U16.Max * prtV.get(j));
                        assert (value >= 0) : "rVal value is negative!";
                        rndEntryValues.add(value);
                    }
                    newCodebook.add(new LearningCodebookEntry(rndEntryValues));
                    continue;
                }

                ArrayList<Integer> left = new ArrayList<>(prtV.size());
                ArrayList<Integer> right = new ArrayList<>(prtV.size());
                for (int j = 0; j < prtV.size(); j++) {
                    final int lVal = (int) Math.round(entryToSplit.getVector()[j] * (1.0 - prtV.get(j)));
                    final int rVal = (int) Math.round(entryToSplit.getVector()[j] * (1.0 + prtV.get(j)));

                    assert (rVal >= 0) : "rVal value is negative!";
                    assert (lVal >= 0) : "lVal value is negative!";

                    left.add(lVal);
                    right.add(rVal);
                }
                // NOTE(Moravec):   Maybe we just create one new entry and bring the "original" one to the next iteration
                //                  as stated in Sayood's book (p. 302)
                final LearningCodebookEntry rightEntry = new LearningCodebookEntry(right);
                final LearningCodebookEntry leftEntry = new LearningCodebookEntry(left);
                assert (!rightEntry.equals(leftEntry)) : "Entry was split to two identical entries!";
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

    private void LBG(ArrayList<LearningCodebookEntry> codebook) {
        LBG(codebook, EPSILON);
    }

    private void LBG(ArrayList<LearningCodebookEntry> codebook, final double epsilon) {

        double previousDistortion = Double.POSITIVE_INFINITY;

        int iteration = 1;
        while (true) {

            // Step 1
            for (final ArrayList<Integer> trainingVec : trainingVectors) {
                double minDist = Double.POSITIVE_INFINITY;
                LearningCodebookEntry closestEntry = null;
                for (LearningCodebookEntry entry : codebook) {
                    double entryDistance = vectorDistance(entry.getVectorAsArrayList(), trainingVec);
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
            for (LearningCodebookEntry entry : codebook) {
                avgDistortion += entry.getAverageDistortion();
            }
            avgDistortion /= (double) codebook.size();

            // Step 3
            double dist = (previousDistortion - avgDistortion) / avgDistortion;
            System.out.println(String.format("It: %d Distortion: %.5f", iteration++, dist));
            if (dist < epsilon) {
                break;
            }
            previousDistortion = avgDistortion;

            // Step 4
            for (LearningCodebookEntry entry : codebook) {
                entry.calculateCentroid();
                entry.clearTrainingData();
            }
        }
    }


    private void fixEmptyEntries(final ArrayList<LearningCodebookEntry> codebook) {
        final int originalSize = codebook.size();
        final ArrayList<LearningCodebookEntry> fixedCodebook = new ArrayList<>(originalSize);

        LearningCodebookEntry emptyEntry = null;
        for (final LearningCodebookEntry potentionallyEmptyEntry : codebook) {
            if (potentionallyEmptyEntry.getTrainingVectors().size() == 0) {
                emptyEntry = potentionallyEmptyEntry;
            }
        }
        while (emptyEntry != null) {
            fixSingleEmptyEntry(codebook, emptyEntry);
            emptyEntry = null;
            for (final LearningCodebookEntry potentionallyEmptyEntry : codebook) {
                if (potentionallyEmptyEntry.getTrainingVectors().size() == 0) {
                    emptyEntry = potentionallyEmptyEntry;
                }
            }
        }
    }

    private void fixSingleEmptyEntry(ArrayList<LearningCodebookEntry> codebook, final LearningCodebookEntry emptyEntry) {
        System.out.println("****** FOUND EMPTY ENTRY ******");
        // Remove empty entry from codebook.
        codebook.remove(emptyEntry);

        // Find biggest partition.
        LearningCodebookEntry biggestPartition = emptyEntry;
        for (final LearningCodebookEntry entry : codebook) {
            // NOTE(Moravec):   We can not select random training vector from zero vector
            //                  because we would just create another zero vector most likely.
            if ((!entry.isZeroVector()) && (entry.getTrainingVectors().size() > biggestPartition.getTrainingVectors().size())) {
                biggestPartition = entry;
            }
        }
        // Assert that we have found some.
        assert (biggestPartition.getTrainingVectors().size() > 0) : "Biggest partitions was empty before split";

        // Choose random trainingVector from biggest partition and set it as new entry.
        int randomIndex = new Random().nextInt(biggestPartition.getTrainingVectors().size());
        LearningCodebookEntry newEntry = new LearningCodebookEntry(biggestPartition.getTrainingVectors().get(randomIndex));
        // Add new entry to the codebook.
        codebook.add(newEntry);
        // Remove that vector from training vectors of biggest partition
        biggestPartition.removeTrainingVectorAndDistance(randomIndex);

        // Redistribute biggest partition training vectors
        final ArrayList<ArrayList<Integer>> trainingVectors = (ArrayList<ArrayList<Integer>>) biggestPartition.getTrainingVectors().clone();
        biggestPartition.clearTrainingData();
        newEntry.clearTrainingData();

        for (final ArrayList<Integer> trVec : trainingVectors) {
            double originalPartitionDist = vectorDistance(biggestPartition.getVectorAsArrayList(), trVec);
            double newEntryDist = vectorDistance(newEntry.getVectorAsArrayList(), trVec);
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
