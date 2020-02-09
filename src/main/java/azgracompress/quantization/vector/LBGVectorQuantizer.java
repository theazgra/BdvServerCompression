package azgracompress.quantization.vector;

import azgracompress.U16;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.Utils;

import java.util.Arrays;
import java.util.Random;

public class LBGVectorQuantizer {
    public final static double PRT_VECTOR_DIVIDER = 4.0;
    private final double EPSILON = 0.005;
    private final int vectorSize;
    private final int codebookSize;
    private final int workerCount;

    private final TrainingVector[] trainingVectors;
    private final VectorDistanceMetric metric = VectorDistanceMetric.Euclidean;

    boolean verbose = false;
    private double _mse = 0.0;

    public LBGVectorQuantizer(final int[][] vectors, final int codebookSize, final int workerCount) {

        assert (vectors.length > 0) : "No training vectors provided";

        this.trainingVectors = new TrainingVector[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            trainingVectors[i] = new TrainingVector(vectors[i]);
        }

        this.vectorSize = vectors[0].length;
        this.codebookSize = codebookSize;
        this.workerCount = workerCount;
    }

    /**
     * Find the optimal codebook of vectors, used for vector quantization.
     *
     * @return Result of the search.
     */
    public LBGResult findOptimalCodebook() {
        return findOptimalCodebook(false);
    }

    /**
     * Find the optimal codebook of vectors, used for vector quantization.
     *
     * @param isVerbose True if program algorithm should be verbose.
     * @return Result of the search.
     */
    public LBGResult findOptimalCodebook(boolean isVerbose) {
        Stopwatch stopwatch = Stopwatch.startNew("findOptimalCodebook");
        this.verbose = isVerbose;

        LearningCodebookEntry[] codebook = initializeCodebook();
        if (verbose) {
            System.out.println("Got initial codebook. Improving codebook...");
        }
        LBG(codebook, EPSILON * 0.1);
        final double finalMse = averageMse(codebook);
        final double psnr = Utils.calculatePsnr(finalMse, U16.Max);
        if (verbose) {
            System.out.println(String.format("Improved codebook, final average MSE: %.4f PSNR: %.4f (dB)",
                                             finalMse,
                                             psnr));
        }
        stopwatch.stop();
        if (verbose) {
            System.out.println(stopwatch);
        }
        return new LBGResult(learningCodebookToCodebook(codebook), finalMse, psnr);
    }

    /**
     * Convert LearningCodebookEntry array to CodebookEntry array.
     *
     * @param learningCodebook Source array of LearningCodebookEntry.
     * @return Array of CodebookEntries.
     */
    private CodebookEntry[] learningCodebookToCodebook(final LearningCodebookEntry[] learningCodebook) {
        CodebookEntry[] codebook = new CodebookEntry[learningCodebook.length];
        for (int i = 0; i < codebook.length; i++) {
            codebook[i] = new CodebookEntry(learningCodebook[i].getVector());
        }
        return codebook;
    }

    /**
     * Add value to the global mse in synchronized way.
     *
     * @param threadMse Value to add.
     */
    private synchronized void updateMse(final double threadMse) {
        _mse += threadMse;
    }

    /**
     * Calculate the average mean square error of the codebook.
     *
     * @param codebook Codebook of vectors.
     * @return Mean square error.
     */
    private double averageMse(final LearningCodebookEntry[] codebook) {
        Stopwatch s = Stopwatch.startNew("averageMse");
        double mse = 0.0;
        if (workerCount > 1) {
            // Reset the global mse
            _mse = 0.0;
            Thread[] workers = new Thread[workerCount];
            final int workSize = trainingVectors.length / workerCount;
            for (int wId = 0; wId < workerCount; wId++) {
                final int fromIndex = wId * workSize;
                final int toIndex = (wId == workerCount - 1) ? trainingVectors.length : (workSize + (wId * workSize));

                workers[wId] = new Thread(() -> {
                    VectorQuantizer quantizer = new VectorQuantizer(learningCodebookToCodebook(codebook));
                    double threadMse = 0.0;
                    int cnt = 0;
                    int[] vector;
                    int[] quantizedVector;
                    for (int i = fromIndex; i < toIndex; i++) {
                        ++cnt;
                        vector = trainingVectors[i].getVector();
                        quantizedVector = quantizer.quantize(vector);

                        for (int vI = 0; vI < vectorSize; vI++) {
                            threadMse += Math.pow(((double) vector[vI] - (double) quantizedVector[vI]), 2);
                        }
                    }
                    assert (cnt == toIndex - fromIndex);
                    threadMse /= (double) (toIndex - fromIndex);

                    // Update global mse, updateMse function is synchronized.
                    updateMse(threadMse);
                });
                workers[wId].start();

            }

            try {
                for (int wId = 0; wId < workerCount; wId++) {
                    workers[wId].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mse = _mse / (double) workerCount;
        } else {
            VectorQuantizer quantizer = new VectorQuantizer(learningCodebookToCodebook(codebook));
            for (final TrainingVector trV : trainingVectors) {
                int[] quantizedV = quantizer.quantize(trV.getVector());

                for (int i = 0; i < vectorSize; i++) {
                    mse += Math.pow(((double) trV.getVector()[i] - (double) quantizedV[i]), 2);
                }
            }
            mse /= (double) trainingVectors.length;
        }
        s.stop();
        if (this.verbose) {
            System.out.println(s);
        }
        return mse;
    }

    /**
     * Calculate the initial perturbation from training vectors.
     *
     * @param vectors Training vectors.
     * @return Perturbation vector.
     */
    private double[] getPerturbationVector(final TrainingVector[] vectors) {

        // Max is initialized to zero that is ok.
        int[] max = new int[vectorSize];
        // We have to initialize min to Max values.
        int[] min = new int[vectorSize];
        Arrays.fill(min, U16.Max);

        for (final TrainingVector v : vectors) {
            for (int i = 0; i < vectorSize; i++) {
                if (v.getVector()[i] < min[i]) {
                    min[i] = v.getVector()[i];
                }
                if (v.getVector()[i] > max[i]) {
                    max[i] = v.getVector()[i];
                }
            }
        }

        double[] perturbationVector = new double[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            perturbationVector[i] = ((double) max[i] - (double) min[i]) / PRT_VECTOR_DIVIDER;
        }
        return perturbationVector;
    }

    /**
     * Create the initial entry as mean of all training vectors.
     *
     * @return Initial codebook entry.
     */
    private LearningCodebookEntry createInitialEntry() {
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
        return new LearningCodebookEntry(result);
    }

    /**
     * Initialize the LBG codebook be recursively splitting each entry into two new.
     *
     * @return The initial codebook to be improved by LBG.
     */
    private LearningCodebookEntry[] initializeCodebook() {

        int currentCodebookSize = 1;
        LearningCodebookEntry[] codebook = new LearningCodebookEntry[]{createInitialEntry()};

        while (currentCodebookSize != codebookSize) {
            int cbIndex = 0;
            LearningCodebookEntry[] newCodebook = new LearningCodebookEntry[currentCodebookSize * 2];

            // Split each entry in codebook with fixed perturbation vector.
            for (final LearningCodebookEntry entryToSplit : codebook) {
                double[] prtV;
                if (codebook.length == 1) {
                    assert (trainingVectors.length > 0) :
                            "There are no vectors from which to create perturbation " + "vector";
                    prtV = getPerturbationVector(trainingVectors);
                } else {
                    assert (entryToSplit.getVectorCount() > 0) :
                            "There are no vectors from which to create perturbation vector";

                    prtV = entryToSplit.getPerturbationVector();
                }

                // We always want to carry zero vector to next iteration.
                if (isZeroVector(entryToSplit.getVector())) {
                    // Use zero vector in next iteration.
                    newCodebook[cbIndex++] = entryToSplit;

                    // Create another codebook entry from perturbation vector.
                    int[] entryFromPrtVecValues = new int[prtV.length];
                    for (int i = 0; i < prtV.length; i++) {
                        final int value = (int) Math.floor(prtV[i]);
                        //                        assert (value >= 0) : "value is too low!";
                        //                        assert (value <= U16.Max) : "value is too big!";
                        entryFromPrtVecValues[i] = value;
                    }
                    newCodebook[cbIndex++] = new LearningCodebookEntry(entryFromPrtVecValues);
                    continue;
                }

                if (isZeroVector(prtV)) {
                    // Zero perturbation vector can't create two different entries.
                    // The original entry is going to be moved to the next codebook with the new
                    // random entry, which will get improved in the LBG algorithm.
                    final int[] randomEntryValues = generateRandomVector();
                    newCodebook[cbIndex++] = entryToSplit;
                    newCodebook[cbIndex++] = new LearningCodebookEntry(randomEntryValues);
                } else {
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
                    newCodebook[cbIndex++] = rightEntry;
                    newCodebook[cbIndex++] = leftEntry;
                }
                assert (!(newCodebook[cbIndex - 2].equals(newCodebook[cbIndex - 1]))) :
                        "Entry was split to two identical entries!";
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

    /**
     * Generate random vector of size = this.vectorSize
     *
     * @return Generated random vector.
     */
    private int[] generateRandomVector() {
        int[] randomVector = new int[vectorSize];
        Random rnd = new Random();
        for (int i = 0; i < vectorSize; i++) {
            randomVector[i] = rnd.nextInt(U16.Max + 1);
        }
        return randomVector;
    }

    /**
     * Check whether all vector elements are equal to 0.0
     *
     * @param vector Vector array.
     * @return True if all elements are zeros.
     */
    private boolean isZeroVector(final double[] vector) {
        for (final double value : vector) {
            if (value != 0.0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether all vector elements are equal to 0
     *
     * @param vector Vector array.
     * @return True if all elements are zeros.
     */
    private boolean isZeroVector(final int[] vector) {
        for (final double value : vector) {
            if (value != 0.0) {
                return false;
            }
        }
        return true;
    }


    /**
     * Execute the LBG algorithm with default epsilon value.
     *
     * @param codebook Codebook to improve.
     */
    private void LBG(LearningCodebookEntry[] codebook) {
        LBG(codebook, EPSILON);
    }

    /**
     * Execute the LBG algorithm with specific epsilon value.
     *
     * @param codebook Codebook to improve.
     * @param epsilon  Epsilon value.
     */
    private void LBG(LearningCodebookEntry[] codebook, final double epsilon) {
        double previousDistortion = Double.POSITIVE_INFINITY;
        int iteration = 1;
        double lastDist = Double.POSITIVE_INFINITY;
        while (true) {
            // Assign training vectors to the closest codebook entry and calculate the entry properties.
            assignVectorsToClosestEntry(codebook);

            // Fix empty codebook entries.
            fixEmptyEntries(codebook);

            // Calculate average distortion of the codebook.
            double avgDistortion = 0;
            for (LearningCodebookEntry entry : codebook) {
                avgDistortion += entry.getAverageDistortion();
            }
            avgDistortion /= codebook.length;

            // Calculate distortion
            double dist = (previousDistortion - avgDistortion) / avgDistortion;
            if (verbose) {
                System.out.println(String.format("---- It: %d Distortion: %.5f", iteration++, dist));
                System.out.println(String.format("Last Dist: %.5f Current dist: %.5f", lastDist, dist));
            }
            if (dist > lastDist) {
                if (verbose) {
                    System.out.println("Previous distortion was better. Ending LBG...");
                }
                break;
            }

            // Check distortion against epsilon
            if (dist < epsilon) {
                break;
            } else {
                previousDistortion = avgDistortion;
                lastDist = dist;
            }
        }
    }

    /**
     * Assign each training vector to the closest codebook entry.
     *
     * @param codebook Vector codebook.
     */
    private void assignVectorsToClosestEntry(LearningCodebookEntry[] codebook) {
        Stopwatch stopwatch = Stopwatch.startNew("assignVectorsToClosestEntry");
        if (workerCount > 1) {
            parallelAssignVectors(codebook);
            // In parallel version entry properties are already calculated.
        } else {
            defaultAssignVectors(codebook);
            // Calculate all the entry properties.
            calculateEntryProperties(codebook);
        }
        stopwatch.stop();
        if (this.verbose) {
            System.out.println(stopwatch);
        }

    }

    /**
     * Default version (Single Thread) of assigning vectors to closest codebook entry.
     *
     * @param codebook Vector codebook.
     */
    private void defaultAssignVectors(LearningCodebookEntry[] codebook) {
        double minDist;
        int closestEntryIndex;

        for (TrainingVector trainingVector : trainingVectors) {
            minDist = Double.POSITIVE_INFINITY;
            closestEntryIndex = -1;

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
                assert (closestEntryIndex < codebook.length);
                trainingVector.setEntryInfo(closestEntryIndex, minDist);
            } else {
                assert (false) : "Did not found closest entry.";
                System.err.println("Did not found closest entry.");
            }
        }
    }

    /**
     * Multi-Threaded version of assigning vectors to closest codebook entry.
     *
     * @param codebook Vector codebook.
     */
    private void parallelAssignVectors(LearningCodebookEntry[] codebook) {
        Thread[] workers = new Thread[workerCount];
        final int workSize = trainingVectors.length / workerCount;
        EntryInfo[][] threadEntryInfos = new EntryInfo[workerCount][codebook.length];


        for (int wId = 0; wId < workerCount; wId++) {
            final int fromIndex = wId * workSize;
            final int toIndex = (wId == workerCount - 1) ? trainingVectors.length : (workSize + (wId * workSize));

            threadEntryInfos[wId] = new EntryInfo[codebook.length];
            final EntryInfo[] threadEntryInfoArray = threadEntryInfos[wId];

            workers[wId] = new Thread(() -> {
                for (int eI = 0; eI < codebook.length; eI++) {
                    threadEntryInfoArray[eI] = new EntryInfo(vectorSize);
                }
                int value;
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
                        trainingVectors[vecIndex].setEntryInfo(closestEntryIndex, minimalDistance);

                        threadEntryInfoArray[closestEntryIndex].vectorCount += 1;
                        threadEntryInfoArray[closestEntryIndex].distanceSum += minimalDistance;
                        for (int dim = 0; dim < vectorSize; dim++) {
                            value = trainingVectors[vecIndex].getVector()[dim];

                            threadEntryInfoArray[closestEntryIndex].dimensionSum[dim] += value;


                            if (value < threadEntryInfoArray[closestEntryIndex].min[dim]) {
                                threadEntryInfoArray[closestEntryIndex].min[dim] = value;
                            }
                            if (value > threadEntryInfoArray[closestEntryIndex].max[dim]) {
                                threadEntryInfoArray[closestEntryIndex].max[dim] = value;
                            }
                        }

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

        // Combine all thread entry infos to final array distributed to the entries.
        EntryInfo[] entryInfos = new EntryInfo[codebook.length];
        // Assign first thread infos and we can skip it later.
        System.arraycopy(threadEntryInfos[0], 0, entryInfos, 0, codebook.length);

        for (int tId = 1; tId < workerCount; tId++) {
            for (int entryIndex = 0; entryIndex < codebook.length; entryIndex++) {
                combine(entryInfos[entryIndex], threadEntryInfos[tId][entryIndex]);
            }
        }

        for (int i = 0; i < codebook.length; i++) {
            codebook[i].setInfo(entryInfos[i]);
        }
    }

    private void combine(EntryInfo finalEntryInfo, final EntryInfo threadEntryInfo) {
        finalEntryInfo.vectorCount += threadEntryInfo.vectorCount;
        finalEntryInfo.distanceSum += threadEntryInfo.distanceSum;

        for (int dim = 0; dim < vectorSize; dim++) {
            finalEntryInfo.dimensionSum[dim] += threadEntryInfo.dimensionSum[dim];

            if (threadEntryInfo.min[dim] < finalEntryInfo.min[dim]) {
                finalEntryInfo.min[dim] = threadEntryInfo.min[dim];
            }

            if (threadEntryInfo.max[dim] > finalEntryInfo.max[dim]) {
                finalEntryInfo.max[dim] = threadEntryInfo.max[dim];
            }
        }
    }

    /**
     * Calculate properties (vector count, new centroid, distortion, perturbation vector) for codebook entries.
     *
     * @param codebook Vector codebook.
     */
    private void calculateEntryProperties(LearningCodebookEntry[] codebook) {

        Stopwatch stopwatch = Stopwatch.startNew("calculateEntryProperties");
        int value;
        EntryInfo[] entryInfos = new EntryInfo[codebook.length];
        for (int i = 0; i < entryInfos.length; i++) {
            entryInfos[i] = new EntryInfo(vectorSize);
        }

        for (final TrainingVector trainingVector : trainingVectors) {
            final int eIndex = trainingVector.getEntryIndex();

            entryInfos[eIndex].vectorCount += 1;
            entryInfos[eIndex].distanceSum += trainingVector.getEntryDistance();

            for (int dim = 0; dim < vectorSize; dim++) {
                value = trainingVector.getVector()[dim];

                entryInfos[eIndex].dimensionSum[dim] += value;


                if (value < entryInfos[eIndex].min[dim]) {
                    entryInfos[eIndex].min[dim] = value;
                }
                if (value > entryInfos[eIndex].max[dim]) {
                    entryInfos[eIndex].max[dim] = value;
                }
            }
        }
        for (int i = 0; i < codebook.length; i++) {
            codebook[i].setInfo(entryInfos[i]);
        }
        stopwatch.stop();
        if (this.verbose) {
            System.out.println(stopwatch);
        }
    }


    /**
     * Fix all empty codebook entries.
     *
     * @param codebook Vector codebook.
     */
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

    /**
     * Fix empty codebook entry by splitting the largest codebook entry.
     *
     * @param codebook        Vector codebook.
     * @param emptyEntryIndex Index of the empty entry.
     */
    private void fixSingleEmptyEntry(LearningCodebookEntry[] codebook, final int emptyEntryIndex) {
        // Find biggest partition.
        int largestEntryIndex = emptyEntryIndex;
        int largestEntrySize = codebook[emptyEntryIndex].getVectorCount();
        // NOTE(Moravec): We can't select random training vector, because zero vector would create another zero vector.
        for (int i = 0; i < codebook.length; i++) {
            if ((codebook[i].getVectorCount() > largestEntrySize) && !isZeroVector(codebook[i].getVector())) {
                largestEntryIndex = i;
                largestEntrySize = codebook[i].getVectorCount();
            }
        }

        // Assert that we have found some non empty codebook entry.
        assert (largestEntryIndex != emptyEntryIndex) : "Unable to find biggest partition.";
        assert (codebook[largestEntryIndex].getVectorCount() > 0) : "Biggest partitions was empty before split";

        // Get training vectors assigned to the largest codebook entry.
        final TrainingVector[] largestPartitionVectors = getEntryTrainingVectors(largestEntryIndex,
                                                                                 codebook[largestEntryIndex].getVectorCount());

        // Choose random trainingVector from biggest partition and set it as new entry.
        int randomIndex = new Random().nextInt(largestPartitionVectors.length);
        // Plane the new entry on the index of the empty entry.
        codebook[emptyEntryIndex] = new LearningCodebookEntry(largestPartitionVectors[randomIndex].getVector());


        // Speedup - speed the look for closest entry.

        EntryInfo oldEntryInfo = new EntryInfo(vectorSize);
        EntryInfo newEntryInfo = new EntryInfo(vectorSize);

        int value, entryIndex;
        double oldDistance, newDistance, distance;
        for (int vIndex = 0; vIndex < largestPartitionVectors.length; vIndex++) {
            oldDistance = VectorQuantizer.distanceBetweenVectors(largestPartitionVectors[vIndex].getVector(),
                                                                 codebook[largestEntryIndex].getVector(),
                                                                 metric);
            newDistance = VectorQuantizer.distanceBetweenVectors(largestPartitionVectors[vIndex].getVector(),
                                                                 codebook[emptyEntryIndex].getVector(),
                                                                 metric);

            distance = Math.min(oldDistance, newDistance);

            EntryInfo closerEntryInfo = null;
            if (newDistance == oldDistance) {
                // If old and new distance are equal we assign the vector to the smaller of the two.
                if (newEntryInfo.vectorCount < oldEntryInfo.vectorCount) {
                    closerEntryInfo = newEntryInfo;
                    entryIndex = emptyEntryIndex;
                } else {
                    closerEntryInfo = oldEntryInfo;
                    entryIndex = largestEntryIndex;
                }
            } else if (newDistance < oldDistance) {
                closerEntryInfo = newEntryInfo;
                entryIndex = emptyEntryIndex;
            } else {
                closerEntryInfo = oldEntryInfo;
                entryIndex = largestEntryIndex;
            }

            largestPartitionVectors[vIndex].setEntryInfo(entryIndex, distance);

            ++closerEntryInfo.vectorCount;
            closerEntryInfo.distanceSum += distance;

            for (int dim = 0; dim < vectorSize; dim++) {
                value = largestPartitionVectors[vIndex].getVector()[dim];

                closerEntryInfo.dimensionSum[dim] += value;


                if (value < closerEntryInfo.min[dim]) {
                    closerEntryInfo.min[dim] = value;
                }
                if (value > closerEntryInfo.max[dim]) {
                    closerEntryInfo.max[dim] = value;
                }
            }
        }

        codebook[largestEntryIndex].setInfo(oldEntryInfo);
        codebook[emptyEntryIndex].setInfo(newEntryInfo);
    }

    /**
     * Get training vectors associated with entry index.
     *
     * @param entryIndex  Codebook entry index.
     * @param vectorCount Codebook entry vector count.
     * @return Array of training vectors.
     */
    private TrainingVector[] getEntryTrainingVectors(final int entryIndex, final int vectorCount) {

        int index = 0;

        //        TrainingVector[] vectors = new TrainingVector[vectorCount];
        int count = 0;
        for (final TrainingVector trainingVector : trainingVectors) {
            if (trainingVector.getEntryIndex() == entryIndex) {
                ++count;
            }
        }
        TrainingVector[] vectors = new TrainingVector[count];
        //        if (count > vectorCount) {
        //            System.err.println(String.format(
        //                    "WARNING: got more training vectors than vectorCount! DIFF: %d",
        //                    count - vectorCount));
        //        }
        for (final TrainingVector trainingVector : trainingVectors) {
            if (trainingVector.getEntryIndex() == entryIndex) {
                vectors[index++] = trainingVector;
            }
        }
        return vectors;
    }

    /**
     * Get codebook size.
     *
     * @return Codebook size.
     */
    public int getCodebookSize() {
        return codebookSize;
    }
}
