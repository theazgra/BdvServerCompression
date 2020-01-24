package azgracompress.quantization.vector;

import azgracompress.U16;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

public class LBGVectorQuantizer {
    private final static double PRTV_DIVIDER = 4.0;
    private final double EPSILON = 0.005;
    private final int vectorSize;
    private final int codebookSize;
    private int currentCodebookSize = 0;
    //    private final int[][] vectors;
    private final TrainingVector[] trainingVectors;
    private final VectorDistanceMetric metric = VectorDistanceMetric.Euclidean;

    boolean verbose = false;
    private final int workerCount;

    public LBGVectorQuantizer(final int[][] vectors, final int codebookSize, final int workerCount) {

        assert (vectors.length > 0) : "No training vectors provided";
        //        this.vectors = vectors;
        this.trainingVectors = new TrainingVector[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            trainingVectors[i] = new TrainingVector(vectors[i]);
        }

        this.vectorSize = vectors[0].length;
        this.codebookSize = codebookSize;
        this.workerCount = workerCount;
    }

    public LBGResult findOptimalCodebook() {
        return findOptimalCodebook(true);
    }

    public LBGResult findOptimalCodebook(boolean isVerbose) {
        this.verbose = isVerbose;

        // TODO(Moravec): Remove in production.
        this.verbose = true;

        LearningCodebookEntry[] codebook = initializeCodebook();
        if (verbose) {
            System.out.println("Got initial codebook. Improving codebook...");
        }
        LBG(codebook, EPSILON * 0.01);
        final double finalMse = averageMse(codebook);
        final double psnr = Utils.calculatePsnr(finalMse, U16.Max);
        if (verbose) {
            System.out.println(String.format("Improved codebook, final average MSE: %.4f PSNR: %.4f (dB)",
                                             finalMse,
                                             psnr));
        }
        return new LBGResult(learningCodebookToCodebook(codebook), finalMse, psnr);
    }

    private CodebookEntry[] learningCodebookToCodebook(final LearningCodebookEntry[] learningCodebook) {
        CodebookEntry[] codebook = new CodebookEntry[learningCodebook.length];
        for (int i = 0; i < codebook.length; i++) {
            codebook[i] = new CodebookEntry(learningCodebook[i].getVector());
        }
        return codebook;
    }

    private int[][] quantizeTrainingVectors(final VectorQuantizer quantizer) {
        int[][] result = new int[trainingVectors.length][vectorSize];
        for (int i = 0; i < trainingVectors.length; i++) {
            result[i] = quantizer.quantize(trainingVectors[i].getVector());
        }
        return result;
    }

    private double averageMse(final LearningCodebookEntry[] codebook) {
        VectorQuantizer quantizer = new VectorQuantizer(learningCodebookToCodebook(codebook));
        final int[][] quantizedVectors = quantizeTrainingVectors(quantizer);

        assert trainingVectors.length == quantizedVectors.length;
        double mse = 0.0;

        for (int vIndex = 0; vIndex < trainingVectors.length; vIndex++) {
            for (int i = 0; i < vectorSize; i++) {
                mse += Math.pow(
                        ((double) trainingVectors[vIndex].getVector()[i] - (double) quantizedVectors[vIndex][i]),
                        2);
            }
        }

        final double avgMse = mse / (double) (trainingVectors.length * vectorSize);
        return avgMse;
    }

    private double[] getPerturbationVector(final Stream<TrainingVector> vectors) {

        // Max is initialized to zero that is ok.
        int[] max = new int[vectorSize];
        // We have to initialize min to Max values.
        int[] min = new int[vectorSize];
        Arrays.fill(min, U16.Max);

        vectors.forEach(v -> {
            for (int i = 0; i < vectorSize; i++) {
                if (v.getVector()[i] < min[i]) {
                    min[i] = v.getVector()[i];
                }
                if (v.getVector()[i] > max[i]) {
                    max[i] = v.getVector()[i];
                }
            }
        });

        double[] perturbationVector = new double[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            // NOTE(Moravec): Divide by 16 instead of 4, because we are dealing with maximum difference of 65535.
            perturbationVector[i] = ((double) max[i] - (double) min[i]) / PRTV_DIVIDER;
        }
        return perturbationVector;
    }

    private int[] createInitialEntry() {
        double[] vectorSum = new double[vectorSize];

        for (final TrainingVector trainingVector : trainingVectors) {
            for (int i = 0; i < vectorSize; i++) {
                vectorSum[i] += (double) trainingVector.getVector()[i];
            }
        }
        int[] result = new int[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            result[i] = (int) Math.round(vectorSum[i] / (double) trainingVectors.length);
        }
        return result;
    }

    private LearningCodebookEntry[] initializeCodebook() {

        currentCodebookSize = 1;
        LearningCodebookEntry[] codebook = new LearningCodebookEntry[currentCodebookSize];
        codebook[0] = new LearningCodebookEntry(createInitialEntry());

        while (currentCodebookSize != codebookSize) {
            int cbIndex = 0;
            LearningCodebookEntry[] newCodebook = new LearningCodebookEntry[currentCodebookSize * 2];

            // Split each entry in codebook with fixed perturbation vector.
            for (final LearningCodebookEntry entryToSplit : codebook) {
                double[] prtV;
                if (codebook.length == 1) {
                    assert (trainingVectors.length > 0) :
                            "There are no vectors from which to create perturbation " + "vector";
                    prtV = getPerturbationVector(Arrays.stream(trainingVectors));
                } else {
                    assert (entryToSplit.getVectorCount() > 0) :
                            "There are no vectors from which to create perturbation vector";

                    prtV = entryToSplit.getPerturbationVector();
                }

                // We always want to carry zero vector to next iteration.
                if (entryToSplit.isZeroVector()) {
                    if (verbose) {
                        System.out.println("--------------------------IS zero vector");
                    }
                    newCodebook[cbIndex++] = entryToSplit;

                    ArrayList<Integer> rndEntryValues = new ArrayList<>(prtV.length);
                    for (final double v : prtV) {
                        final int value = (int) Math.floor(v);
                        assert (value >= 0) : "value is too low!";
                        assert (value <= U16.Max) : "value is too big!";
                        rndEntryValues.add(value);
                    }
                    newCodebook[cbIndex++] = new LearningCodebookEntry(rndEntryValues);
                    continue;
                }

                int[] left = new int[prtV.length];
                int[] right = new int[prtV.length];
                for (int i = 0; i < prtV.length; i++) {
                    final int lVal = (int) ((double) entryToSplit.getVector()[i] - prtV[i]);
                    final int rVal = (int) ((double) entryToSplit.getVector()[i] + prtV[i]);

                    left[i] = lVal;
                    right[i] = rVal;
                }
                final LearningCodebookEntry rightEntry = new LearningCodebookEntry(right);
                final LearningCodebookEntry leftEntry = new LearningCodebookEntry(left);
                assert (!rightEntry.equals(leftEntry)) : "Entry was split to two identical entries!";
                newCodebook[cbIndex++] = rightEntry;
                newCodebook[cbIndex++] = leftEntry;

            }
            codebook = newCodebook;
            assert (codebook.length == (currentCodebookSize * 2));
            if (verbose) {
                System.out.println(String.format("Split from %d -> %d", currentCodebookSize, currentCodebookSize * 2));
            }
            currentCodebookSize *= 2;

            // Execute LBG Algorithm on current codebook to improve it.
            if (verbose) {
                System.out.println("Improving current codebook...");
            }
            LBG(codebook);


            if (verbose) {
                final double avgMse = averageMse(codebook);
                System.out.println(String.format("Average MSE: %.4f", avgMse));
            }
        }
        return codebook;
    }


    private void LBG(LearningCodebookEntry[] codebook) {
        LBG(codebook, EPSILON);
    }

    private void LBG(LearningCodebookEntry[] codebook, final double epsilon) {
        Stopwatch totalLbgFun = Stopwatch.startNew("Whole LBG function");

        double previousDistortion = Double.POSITIVE_INFINITY;

        Stopwatch innerLoopStopwatch = new Stopwatch("LBG inner loop");
        Stopwatch findingClosestEntryStopwatch = new Stopwatch("FindingClosestEntry");
        Stopwatch distCalcStopwatch = new Stopwatch("DistortionCalc");
        Stopwatch fixEmptyStopwatch = new Stopwatch("FixEmpty");
        int iteration = 1;
        while (true) {
            innerLoopStopwatch.restart();

            // Step 1


            findingClosestEntryStopwatch.restart();

            assignVectorsToClosestEntry(codebook);

            findingClosestEntryStopwatch.stop();

            //            System.out.println(findingClosestEntryStopwatch);

            //            fixEmptyStopwatch.restart();
            fixEmptyEntries(codebook);
            //            fixEmptyStopwatch.stop();
            //            System.out.println(fixEmptyStopwatch);

            // Step 2
            distCalcStopwatch.restart();
            double avgDistortion = 0;
            for (LearningCodebookEntry entry : codebook) {
                avgDistortion += entry.getAverageDistortion();
            }
            avgDistortion /= codebook.length;
            distCalcStopwatch.stop();

            //            System.out.println(distCalcStopwatch);

            // Step 3
            double dist = (previousDistortion - avgDistortion) / avgDistortion;
            if (verbose) {
                System.out.println(String.format("---- It: %d Distortion: %.5f", iteration++, dist));
            }

            if (dist < epsilon) {
                // NOTE(Moravec):   We will leave training data in entries so we can use them for
                //                  PRT vector calculation.
                break;
            } else {
                previousDistortion = avgDistortion;


                // Step 4
                // NOTE: Centroid is already calculated.
                //                for (LearningCodebookEntry entry : codebook) {
                //                    entry.calculateCentroid();
                //                    entry.clearTrainingData();
                //                }
            }
            innerLoopStopwatch.stop();

            //            System.out.println(innerLoopStopwatch);
        }

        totalLbgFun.stop();
        System.out.println(totalLbgFun);
    }

    private void assignVectorsToClosestEntry(LearningCodebookEntry[] codebook) {
        for (int i = 0; i < codebook.length; i++) {
            codebook[i].setVectorCount(-1);
        }
        if (workerCount > 1) {
            Thread[] workers = new Thread[workerCount];
            final int workSize = trainingVectors.length / workerCount;

            for (int wId = 0; wId < workerCount; wId++) {
                final int fromIndex = wId * workSize;
                final int toIndex = (wId == workerCount - 1) ? trainingVectors.length : (workSize + (wId * workSize));

                workers[wId] = new Thread(() -> {
                    double minimalDistance, entryDistance;
                    for (int vecIndex = fromIndex; vecIndex < toIndex; vecIndex++) {
                        minimalDistance = Double.POSITIVE_INFINITY;
                        int closestEntryIndex = -1;

                        for (int entryIndex = 0; entryIndex < codebook.length; entryIndex++) {
                            entryDistance = VectorQuantizer.distanceBetweenVectors(codebook[entryIndex].getVector(),
                                                                                   trainingVectors[vecIndex].getVector(),
                                                                                   metric);

                            if (entryDistance < minimalDistance) {
                                minimalDistance = entryDistance;
                                closestEntryIndex = entryIndex;
                            }
                        }

                        if (closestEntryIndex != -1) {
                            trainingVectors[vecIndex].setEntryIndex(closestEntryIndex);
                            trainingVectors[vecIndex].setEntryDistance(minimalDistance);
                        } else {
                            assert (false) : "Did not found closest entry.";
                            System.err.println("Did not found closest entry.");
                        }
                    }
                });
                workers[wId].start();
            }

            try {
                for (int wId = 0; wId < workerCount; wId++) {
                    workers[wId].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                assert (false) : "Failed parallel join";
            }

        } else {
            //////////////////////////////////////////////////////////////////////////
            // Speedup - speed the finding of the closest codebook entry.
            for (TrainingVector trainingVector : trainingVectors) {
                double minDist = Double.POSITIVE_INFINITY;
                int closestEntryIndex = -1;

                for (int entryIndex = 0; entryIndex < codebook.length; entryIndex++) {
                    double entryDistance = VectorQuantizer.distanceBetweenVectors(codebook[entryIndex].getVector(),
                                                                                  trainingVector.getVector(),
                                                                                  metric);

                    if (entryDistance < minDist) {
                        minDist = entryDistance;
                        closestEntryIndex = entryIndex;
                    }
                }

                if (closestEntryIndex != -1) {
                    trainingVector.setEntryIndex(closestEntryIndex);
                    trainingVector.setEntryDistance(minDist);
                } else {
                    assert (false) : "Did not found closest entry.";
                    System.err.println("Did not found closest entry.");
                }
            }

        }

        int[] vectorCounts = new int[codebook.length];
        double[] distanceSums = new double[codebook.length];
        double[][] dimensionSum = new double[codebook.length][vectorSize];

        // Max is initialized to zero that is ok.
        int[][] maxs = new int[codebook.length][vectorSize];
        // We have to initialize min to Max values.
        int[][] mins = new int[codebook.length][vectorSize];
        for (int cbInd = 0; cbInd < codebook.length; cbInd++) {
            Arrays.fill(mins[cbInd], U16.Max);
        }

        int value;
        for (final TrainingVector trainingVector : trainingVectors) {
            final int entryIndex = trainingVector.getEntryIndex();
            assert (entryIndex >= 0);

            ++vectorCounts[entryIndex];
            distanceSums[entryIndex] += trainingVector.getEntryDistance();

            for (int dim = 0; dim < vectorSize; dim++) {
                value = trainingVector.getVector()[dim];

                dimensionSum[entryIndex][dim] += value;

                if (value < mins[entryIndex][dim]) {
                    mins[entryIndex][dim] = value;
                }
                if (value > maxs[entryIndex][dim]) {
                    maxs[entryIndex][dim] = value;
                }
            }
        }

        int[] centroid = new int[vectorSize];
        double[] perturbationVector = new double[vectorSize];

        for (int entryIndex = 0; entryIndex < codebook.length; entryIndex++) {
            LearningCodebookEntry entry = codebook[entryIndex];
            entry.setVectorCount(vectorCounts[entryIndex]);
            entry.setAverageDistortion(distanceSums[entryIndex] / (double) vectorCounts[entryIndex]);

            for (int dim = 0; dim < vectorSize; dim++) {
                centroid[dim] = (int) Math.round(dimensionSum[entryIndex][dim] / (double) vectorCounts[entryIndex]);

                perturbationVector[dim] =
                        ((double) maxs[entryIndex][dim] - (double) mins[entryIndex][dim]) / PRTV_DIVIDER;
            }

            entry.setCentroid(centroid);
            entry.setPerturbationVector(perturbationVector);
        }
    }


    private void fixEmptyEntries(LearningCodebookEntry[] codebook) {
        int emptyEntryIndex = -1;
        for (int i = 0; i < codebook.length; i++) {
            if (codebook[i].getVectorCount() < 2) {
                emptyEntryIndex = i;
            }
        }

        while (emptyEntryIndex != -1) {
            fixSingleEmptyEntry(codebook, emptyEntryIndex);
            emptyEntryIndex = -1;
            for (int i = 0; i < codebook.length; i++) {
                if (codebook[i].getVectorCount() < 2) {
                    emptyEntryIndex = i;
                }
            }
        }

        for (final LearningCodebookEntry lce : codebook) {
            assert (lce.getVectorCount() > 0) : "LearningCodebookEntry is empty!";
        }
    }

    private void fixSingleEmptyEntry(LearningCodebookEntry[] codebook, final int emptyEntryIndex) {
        //        if (verbose) {
        //            System.out.println("******** FOUND EMPTY ENTRY ********");
        //        }

        // Remove empty entry from codebook.
        //        codebook.remove(emptyEntry);

        // Find biggest partition.
        int largestEntryIndex = emptyEntryIndex;
        int largestEntrySize = codebook[emptyEntryIndex].getVectorCount();
        for (int i = 0; i < codebook.length; i++) {
            // NOTE(Moravec):   We can not select random training vector from zero vector
            //                  because we would just create another zero vector.
            if ((codebook[i].getVectorCount() > largestEntrySize) && !codebook[i].isZeroVector()) {
                largestEntryIndex = i;
            }
        }

        // Assert that we have found some.
        assert (largestEntryIndex != emptyEntryIndex) : "Unable to find biggest partition.";
        assert (codebook[largestEntryIndex].getVectorCount() > 0) : "Biggest partitions was empty before split";

        TrainingVector[] largestPartitionVectors = getTrainingVectorFromEntry(largestEntryIndex,
                                                                              codebook[largestEntryIndex].getVectorCount());

        // Choose random trainingVector from biggest partition and set it as new entry.
        int randomIndex = new Random().nextInt(largestPartitionVectors.length);

        LearningCodebookEntry newEntry = new LearningCodebookEntry(largestPartitionVectors[randomIndex].getVector());
        // Add new entry to the codebook on plane of the empty entry.
        codebook[emptyEntryIndex] = newEntry;

        // Remove that vector from training vectors of biggest partition
        //        largestPartitionVectors[randomIndex].setEntryIndex(-1);
        //        largestPartitionVectors[randomIndex].setEntryDistance(Double.POSITIVE_INFINITY);


        // Speedup - speed the look for closest entry.

        int oldEntryVectorCount = 0;
        int newEntryVectorCount = 0;

        int[] oldMin = new int[vectorSize];
        int[] oldMax = new int[vectorSize];
        int[] newMin = new int[vectorSize];
        int[] newMax = new int[vectorSize];
        Arrays.fill(oldMin, U16.Max);
        Arrays.fill(newMin, U16.Max);

        double oldDistSum = 0.0;
        double newDistSum = 0.0;

        double[] oldDimensionSum = new double[vectorSize];
        double[] newDimensionSum = new double[vectorSize];

        int value;
        double oldDistance, newDistance;
        for (TrainingVector trainingVector : largestPartitionVectors) {
            oldDistance = VectorQuantizer.distanceBetweenVectors(trainingVector.getVector(),
                                                                 codebook[largestEntryIndex].getVector(),
                                                                 metric);
            newDistance = VectorQuantizer.distanceBetweenVectors(trainingVector.getVector(),
                                                                 codebook[emptyEntryIndex].getVector(),
                                                                 metric);

//            final int index = oldDistance < newDistance ? largestEntryIndex : emptyEntryIndex;

            if (oldDistance < newDistance) {
                trainingVector.setEntryIndex(largestEntryIndex);
                trainingVector.setEntryDistance(oldDistance);

                ++oldEntryVectorCount;
                oldDistSum += oldDistance;

                for (int dim = 0; dim < vectorSize; dim++) {
                    value = trainingVector.getVector()[dim];

                    oldDimensionSum[dim] += value;

                    if (value < oldMin[dim]) {
                        oldMin[dim] = value;
                    }
                    if (value > oldMax[dim]) {
                        oldMax[dim] = value;
                    }
                }
            } else {
                trainingVector.setEntryIndex(emptyEntryIndex);
                trainingVector.setEntryDistance(newDistance);

                ++newEntryVectorCount;
                newDistSum += newDistance;

                for (int dim = 0; dim < vectorSize; dim++) {
                    value = trainingVector.getVector()[dim];

                    newDimensionSum[dim] += value;

                    if (value < newMin[dim]) {
                        newMin[dim] = value;
                    }
                    if (value > newMax[dim]) {
                        newMax[dim] = value;
                    }
                }
            }
        }

        int[] oldCentroid = new int[vectorSize];
        int[] newCentroid = new int[vectorSize];
        double[] oldPerturbationVector = new double[vectorSize];
        double[] newPerturbationVector = new double[vectorSize];

        codebook[largestEntryIndex].setVectorCount(oldEntryVectorCount);
        codebook[emptyEntryIndex].setVectorCount(newEntryVectorCount);

        codebook[largestEntryIndex].setAverageDistortion(oldDistSum / (double) oldEntryVectorCount);
        codebook[emptyEntryIndex].setAverageDistortion(newDistSum / (double) newEntryVectorCount);

        for (int dim = 0; dim < vectorSize; dim++) {

            oldCentroid[dim] = (int) Math.round(oldDimensionSum[dim] / (double) oldEntryVectorCount);
            oldPerturbationVector[dim] = ((double) oldMax[dim] - (double) oldMin[dim]) / PRTV_DIVIDER;

            newCentroid[dim] = (int) Math.round(newDimensionSum[dim] / (double) newEntryVectorCount);
            newPerturbationVector[dim] = ((double) newMax[dim] - (double) newMin[dim]) / PRTV_DIVIDER;
        }

        codebook[largestEntryIndex].setCentroid(oldCentroid);
        codebook[emptyEntryIndex].setCentroid(newCentroid);

        codebook[largestEntryIndex].setPerturbationVector(oldPerturbationVector);
        codebook[emptyEntryIndex].setPerturbationVector(newPerturbationVector);
    }

    private TrainingVector[] getTrainingVectorFromEntry(final int entryIndex, final int vectorCount) {
//        TrainingVector[] vectors = new TrainingVector[vectorCount];
        ArrayList<TrainingVector> ints = new ArrayList<>(vectorCount);
        int index = 0;
        for (final TrainingVector trainingVector : trainingVectors) {
            if (trainingVector.getEntryIndex() == entryIndex) {
//                vectors[index++] = trainingVector;
                ints.add(trainingVector);
            }
        }
//        assert (index == vectorCount);
        return ints.toArray(new TrainingVector[0]);
//        return vectors;
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public int getCodebookSize() {
        return codebookSize;
    }
}
