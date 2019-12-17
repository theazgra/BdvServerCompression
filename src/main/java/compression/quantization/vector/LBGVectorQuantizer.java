package compression.quantization.vector;

import compression.U16;
import compression.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

public class LBGVectorQuantizer {
    private final double EPSILON = 0.005;
    private final double PERTURBATION_FACTOR_MIN = 0.10;
    private final double PERTURBATION_FACTOR_MAX = 0.60;
    private final int vectorSize;
    private final int codebookSize;
    private final int[][] trainingVectors;
    private final VectorDistanceMetric metric = VectorDistanceMetric.Euclidean;

    private Random random = new Random();

    public LBGVectorQuantizer(final int[][] trainingVectors, final int codebookSize, final int vectorSize) {

        assert (trainingVectors.length > 0) : "No training vectors provided";
        assert (trainingVectors[0].length == vectorSize) : "Training vector is different from provided vectorSize";

        this.trainingVectors = trainingVectors;
        this.vectorSize = vectorSize;
        this.codebookSize = codebookSize;
    }

    // TODO(Moravec): Maybe return QTrainIteration somehow?
    public LBGResult findOptimalCodebook() {
        ArrayList<LearningCodebookEntry> codebook = initializeCodebook();
        System.out.println("Got initial codebook. Improving codebook...");
        LBG(codebook, EPSILON * 0.01);
        final double finalMse = averageMse(codebook);
        final double psnr = Utils.calculatePsnr(finalMse, U16.Max);
        System.out.println(String.format("Improved codebook, final average MSE: %.4f PSNR: %.4f (dB)", finalMse, psnr));
        LBGResult result = new LBGResult(learningCodebookToCodebook(codebook), finalMse, psnr);
        return result;
    }

    private CodebookEntry[] learningCodebookToCodebook(final ArrayList<LearningCodebookEntry> learningCodebook) {
        CodebookEntry[] codebook = new CodebookEntry[learningCodebook.size()];
        for (int i = 0; i < codebook.length; i++) {
            codebook[i] = new CodebookEntry(learningCodebook.get(i).getVector());
        }
        return codebook;
    }

    private double averageMse(final ArrayList<LearningCodebookEntry> codebook) {
        VectorQuantizer quantizer = new VectorQuantizer(learningCodebookToCodebook(codebook));
        final int[][] quantizedVectors = quantizer.quantize(trainingVectors);

        double mse = 0.0;

        for (int vecIndex = 0; vecIndex < quantizedVectors.length; vecIndex++) {
            for (int vecValIndex = 0; vecValIndex < vectorSize; vecValIndex++) {
                mse += Math.pow((trainingVectors[vecIndex][vecValIndex] - quantizedVectors[vecIndex][vecValIndex]), 2);
            }
        }
        mse /= (double) (trainingVectors.length * vectorSize);
        return mse;
    }

    private double[] getPerturbationVector(final Stream<int[]> vectors) {

        // Max is initialized to zero that is ok.
        int[] max = new int[vectorSize];
        // We have to initialize min to Max values.
        int[] min = new int[vectorSize];
        Arrays.fill(min, U16.Max);
        boolean executed = false;

        vectors.forEach(v -> {
            for (int i = 0; i < vectorSize; i++) {
                if (v[i] < min[i]) {
                    min[i] = v[i];
                }
                if (v[i] > max[i]) {
                    max[i] = v[i];
                }
            }
        });

        double[] perturbationVector = new double[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            // NOTE(Moravec): Divide by 16 instead of 4, because we are dealing with maximum difference of 65535.
            perturbationVector[i] = ((double) max[i] - (double) min[i]) / 4.0;
        }
        return perturbationVector;
    }

    private void assertThatNewCodebookEntryIsOriginal(final ArrayList<LearningCodebookEntry> codebook,
                                                      final LearningCodebookEntry newEntry) {

        for (final LearningCodebookEntry entry : codebook) {
            assert !(newEntry.equals(entry)) : "New entry is not original";
        }
    }

    private ArrayList<LearningCodebookEntry> initializeCodebook() {
        ArrayList<LearningCodebookEntry> codebook = new ArrayList<>(codebookSize);
        // Initialize first codebook entry as average of training vectors
        int k = 1;
        ArrayList<Integer> initialEntry = LearningCodebookEntry.vectorMean2(Arrays.stream(trainingVectors),
                                                                            trainingVectors.length,
                                                                            vectorSize);

        codebook.add(new LearningCodebookEntry(initialEntry));

        while (k != codebookSize) {

            assert (codebook.size() == k);
            ArrayList<LearningCodebookEntry> newCodebook = new ArrayList<>(k * 2);
            // Create perturbation vector.


            // TODO(Moravec):   Make sure that when we are splitting entry we don't end up creating two same entries.
            //                  The problem happens when we try to split Vector full of zeroes.
            // Split each entry in codebook with fixed perturbation vector.
            for (final LearningCodebookEntry entryToSplit : codebook) {
                double[] prtV;
                if (codebook.size() == 1) {
                    assert (trainingVectors.length > 0) :
                            "There are no vectors from which to create perturbation " + "vector";
                    prtV = getPerturbationVector(Arrays.stream(trainingVectors));
                }
                //                else if (entryToSplit.getTrainingVectors().size() == 1) {
                //                    prtV = new double[vectorSize];
                //                    for (int i = 0; i < vectorSize; i++) {
                //                        prtV[i] = (double) (entryToSplit.getVector()[i] * 0.5);
                //                    }
                //                }
                else {
                    assert (entryToSplit.getTrainingVectors().size() > 0) : "There are no vectors from which to " +
                            "create perturbation vector";

                    prtV = getPerturbationVector(entryToSplit.getTrainingVectors().stream());
                }

                // We always want to carry zero vector to next iteration.
                if (entryToSplit.isZeroVector()) {
                    System.out.println("--------------------------IS zero vector");
                    newCodebook.add(entryToSplit);

                    ArrayList<Integer> rndEntryValues = new ArrayList<>(prtV.length);
                    for (int j = 0; j < prtV.length; j++) {
                        final int value = (int) Math.floor(prtV[j]);
                        assert (value >= 0) : "value is too low!";
                        assert (value <= U16.Max) : "value is too big!";
                        rndEntryValues.add(value);
                    }
                    newCodebook.add(new LearningCodebookEntry(rndEntryValues));
                    continue;
                }

                // NOTE(Moravec):   Maybe we just create one new entry and bring the "original" one to the next
                //                  iteration as stated in Sayood's book (p. 302)

                int[] left = new int[prtV.length];
                int[] right = new int[prtV.length];
                for (int i = 0; i < prtV.length; i++) {
                    final int lVal = (int) ((double) entryToSplit.getVector()[i] - prtV[i]);
                    final int rVal = (int) ((double) entryToSplit.getVector()[i] + prtV[i]);

                    // NOTE(Moravec): We allow values outside boundaries, because LBG should fix them later.
                    //                    assert (rVal >= 0) : "rVal value is negative!";
                    //                    assert (lVal >= 0) : "lVal value is negative!";
                    //                    assert (rVal <= U16.Max) : "rVal value is too big!";
                    //                    assert (lVal <= U16.Max) : "lVal value is too big!";

                    left[i] = lVal;
                    right[i] = rVal;
                }
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
            System.out.println("Improving current codebook...");
            LBG(codebook);
            final double avgMse = averageMse(codebook);
            System.out.println(String.format("Average MSE: %.4f", avgMse));
        }
        return codebook;
    }


    private void LBG(ArrayList<LearningCodebookEntry> codebook) {
        LBG(codebook, EPSILON);
    }

    private void LBG(ArrayList<LearningCodebookEntry> codebook, final double epsilon) {
        codebook.forEach(entry -> {
            entry.clearTrainingData();
            assert (entry.getTrainingVectors().size() == 0) : "Using entries which are not cleared.";
        });

        double previousDistortion = Double.POSITIVE_INFINITY;

        int iteration = 1;
        while (true) {

            // Step 1
            for (final int[] trainingVec : trainingVectors) {
                double minDist = Double.POSITIVE_INFINITY;
                LearningCodebookEntry closestEntry = null;

                for (LearningCodebookEntry entry : codebook) {
                    double entryDistance = VectorQuantizer.distanceBetweenVectors(entry.getVector(),
                                                                                  trainingVec,
                                                                                  metric);

                    if (entryDistance < minDist) {
                        minDist = entryDistance;
                        closestEntry = entry;
                    }
                }
                assert (closestEntry != null) : "Did not found closest entry.";
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
                // NOTE(Moravec):   We will leave training data in entries so we can use them for
                //                  PRT vector calculation.
                break;
            } else {
                previousDistortion = avgDistortion;
                // Step 4
                for (LearningCodebookEntry entry : codebook) {
                    entry.calculateCentroid();
                    entry.clearTrainingData();
                }
            }
        }
    }


    private void fixEmptyEntries(final ArrayList<LearningCodebookEntry> codebook) {
        final int originalSize = codebook.size();

        LearningCodebookEntry emptyEntry = null;
        for (final LearningCodebookEntry potentiallyEmptyEntry : codebook) {
            if (potentiallyEmptyEntry.getTrainingVectors().size() < 2) { // < 2
                emptyEntry = potentiallyEmptyEntry;
            }
        }
        while (emptyEntry != null) {
            fixSingleEmptyEntry(codebook, emptyEntry);
            emptyEntry = null;
            for (final LearningCodebookEntry potentionallyEmptyEntry : codebook) {
                if (potentionallyEmptyEntry.getTrainingVectors().size() < 2) { // <2
                    emptyEntry = potentionallyEmptyEntry;
                }
            }
        }
    }

    private void fixSingleEmptyEntry(ArrayList<LearningCodebookEntry> codebook,
                                     final LearningCodebookEntry emptyEntry) {
        System.out.println("******** FOUND EMPTY ENTRY ********");
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
        assert (biggestPartition != emptyEntry);
        assert (biggestPartition.getTrainingVectors().size() > 0) : "Biggest partitions was empty before split";

        // Choose random trainingVector from biggest partition and set it as new entry.
        int randomIndex = new Random().nextInt(biggestPartition.getTrainingVectors().size());
        LearningCodebookEntry newEntry =
                new LearningCodebookEntry(biggestPartition.getTrainingVectors().get(randomIndex));
        // Add new entry to the codebook.
        codebook.add(newEntry);
        // Remove that vector from training vectors of biggest partition
        biggestPartition.removeTrainingVectorAndDistance(randomIndex);

        // Redistribute biggest partition training vectors
        final ArrayList<int[]> partionVectors = (ArrayList<int[]>) biggestPartition.getTrainingVectors().clone();
        biggestPartition.clearTrainingData();
        newEntry.clearTrainingData();

        for (final int[] trVec : partionVectors) {
            double originalPartitionDist = VectorQuantizer.distanceBetweenVectors(biggestPartition.getVector(),
                                                                                  trVec,
                                                                                  metric);

            double newEntryDist = VectorQuantizer.distanceBetweenVectors(newEntry.getVector(), trVec, metric);
            if (originalPartitionDist < newEntryDist) {
                biggestPartition.addTrainingVector(trVec, originalPartitionDist);
            } else {
                newEntry.addTrainingVector(trVec, newEntryDist);
            }
        }

        assert (biggestPartition.getTrainingVectors().size() > 0) : "Biggest partition is empty";
        assert (newEntry.getTrainingVectors().size() > 0) : "New entry is empty";
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public int getCodebookSize() {
        return codebookSize;
    }
}
