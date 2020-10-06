package cz.it4i.qcmp.quantization.vector;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.compression.listeners.IStatusListener;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.utilities.Stopwatch;
import cz.it4i.qcmp.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class LBGVectorQuantizer {
    public final static double PRT_VECTOR_DIVIDER = 4.0;
    private final double EPSILON = 0.005;
    final V3i vectorDimensions;
    private final int vectorSize;
    private final int codebookSize;
    private final int workerCount;

    private int uniqueVectorCount = 0;
    private ArrayList<TrainingVector> uniqueTrainingVectors;
    private final TrainingVector[] trainingVectors;
    private final VectorDistanceMetric metric = VectorDistanceMetric.Euclidean;

    private final long[] frequencies;

    private IStatusListener statusListener = null;
    private double _mse = 0.0;

    public LBGVectorQuantizer(final int[][] vectors,
                              final int codebookSize,
                              final int workerCount,
                              final V3i vectorDimensions) {
        assert (vectors.length > 0) : "No training vectors provided";

        this.vectorDimensions = vectorDimensions;
        this.vectorSize = vectors[0].length;


        this.trainingVectors = new TrainingVector[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            trainingVectors[i] = new TrainingVector(Arrays.copyOf(vectors[i], vectorSize));
        }

        this.codebookSize = codebookSize;
        this.workerCount = workerCount;

        frequencies = new long[this.codebookSize];
        findUniqueVectors();
    }

    public void setStatusListener(final IStatusListener statusListener) {
        this.statusListener = statusListener;
    }

    private void reportStatus(final String message) {
        if (statusListener != null)
            statusListener.sendMessage(message);
    }

    private void reportStatus(final String format, final Object... arg) {
        reportStatus(String.format(format, arg));
    }

    private void findUniqueVectors() {
        uniqueVectorCount = 0;
        uniqueTrainingVectors = new ArrayList<>(codebookSize);
        boolean unique;
        for (final TrainingVector trainingVector : trainingVectors) {
            unique = true;
            for (final TrainingVector uniqueVector : uniqueTrainingVectors) {
                if (uniqueVector.vectorEqual(trainingVector)) {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                uniqueTrainingVectors.add(trainingVector);
                ++uniqueVectorCount;
                // If there is more than codebookSize training vectors, then we must use the LBG algorithm.
                if (uniqueVectorCount > codebookSize) {
                    uniqueTrainingVectors = null;
                    break;
                }
            }
        }
    }

    private LBGResult createCodebookFromUniqueVectors() {

        assert (uniqueTrainingVectors != null) : "uniqueTrainingVectors aren't initialized.";
        reportStatus("There is only %d unique vectors. Creating codebook from unique vectors...",
                     uniqueTrainingVectors.size());
        final int[][] codebook = new int[codebookSize][vectorSize];
        final int[] zeroEntry = new int[vectorSize];
        Arrays.fill(zeroEntry, 0);
        for (int i = 0; i < codebookSize; i++) {
            if (i < uniqueVectorCount) {
                codebook[i] = uniqueTrainingVectors.get(i).getVector();
            } else {
                codebook[i] = zeroEntry;
            }
        }
        final double mse = averageMse(codebook);
        final double psnr = Utils.calculatePsnr(mse, U16.Max);
        reportStatus("Unique vector codebook, MSE: %f  PSNR: %f(dB)", mse, psnr);
        return new LBGResult(vectorDimensions, codebook, frequencies, mse, psnr);
    }

    /**
     * Find the optimal codebook of vectors, used for vector quantization.
     *
     * @return Result of the search.
     */
    public LBGResult findOptimalCodebook() {
        final Stopwatch stopwatch = Stopwatch.startNew("LBG::findOptimalCodebook()");

        if (uniqueVectorCount < codebookSize) {
            return createCodebookFromUniqueVectors();
        }

        final LearningCodebookEntry[] codebook = initializeCodebook();
        reportStatus("LBG::findOptimalCodebook() - Got initial codebook. Improving it...");

        LBG(codebook, EPSILON * 0.1);
        final double finalMse = averageMse(codebook);
        final double psnr = Utils.calculatePsnr(finalMse, U16.Max);
        reportStatus("LBG::findOptimalCodebook() - Improved the codebook. Final MSE: %f  PSNR: %f (dB)",
                     finalMse,
                     psnr);
        stopwatch.stop();
        reportStatus(stopwatch.toString());
        return new LBGResult(vectorDimensions, learningCodebookToCodebook(codebook), frequencies, finalMse, psnr);
    }

    /**
     * Convert LearningCodebookEntry array to CodebookEntry array.
     *
     * @param learningCodebook Source array of LearningCodebookEntry.
     * @return Array of CodebookEntries.
     */
    private int[][] learningCodebookToCodebook(final LearningCodebookEntry[] learningCodebook) {
        final int[][] codebook = new int[learningCodebook.length][vectorSize];
        for (int i = 0; i < codebook.length; i++) {
            codebook[i] = learningCodebook[i].getVector();
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

    private void resetFrequencies() {
        Arrays.fill(frequencies, 0);
    }

    private synchronized void addWorkerFrequencies(final long[] workerFrequencies) {
        for (int i = 0; i < workerFrequencies.length; i++) {
            frequencies[i] += workerFrequencies[i];
        }
    }

    private double averageMse(final LearningCodebookEntry[] codebook) {
        return averageMse(learningCodebookToCodebook(codebook));
    }

    /**
     * Calculate the average mean square error of the codebook.
     *
     * @param codebook Codebook of vectors.
     * @return Mean square error.
     */
    private double averageMse(final int[][] codebook) {
        double mse = 0.0;
        resetFrequencies();
        if (workerCount > 1) {
            // Reset the global mse
            _mse = 0.0;
            final Thread[] workers = new Thread[workerCount];
            final int workSize = trainingVectors.length / workerCount;
            for (int wId = 0; wId < workerCount; wId++) {
                final int fromIndex = wId * workSize;
                final int toIndex = (wId == workerCount - 1) ? trainingVectors.length : (workSize + (wId * workSize));

                workers[wId] = new Thread(() -> {
                    final long[] workerFrequencies = new long[codebook.length];
                    final VectorQuantizer quantizer = new VectorQuantizer(new VQCodebook(vectorDimensions,
                                                                                         codebook,
                                                                                         frequencies));

                    double threadMse = 0.0;
                    int[] vector;
                    int qIndex;
                    int[] qVector;
                    for (int i = fromIndex; i < toIndex; i++) {
                        vector = trainingVectors[i].getVector();
                        qIndex = quantizer.quantizeToIndex(vector);
                        ++workerFrequencies[qIndex];


                        qVector = quantizer.getCodebookVectors()[qIndex];
                        for (int vI = 0; vI < vectorSize; vI++) {
                            threadMse += Math.pow(((double) vector[vI] - (double) qVector[vI]), 2);
                        }
                    }
                    threadMse /= (double) (toIndex - fromIndex);

                    // Update global mse, updateMse function is synchronized.
                    updateMse(threadMse);
                    addWorkerFrequencies(workerFrequencies);
                });
                workers[wId].start();

            }

            try {
                for (int wId = 0; wId < workerCount; wId++) {
                    workers[wId].join();
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            mse = _mse / (double) workerCount;
        } else {
            final VectorQuantizer quantizer = new VectorQuantizer(new VQCodebook(vectorDimensions,
                                                                                 codebook,
                                                                                 frequencies));
            int qIndex;
            int[] qVector;
            for (final TrainingVector trV : trainingVectors) {
                qIndex = quantizer.quantizeToIndex(trV.getVector());
                qVector = quantizer.getCodebookVectors()[qIndex];
                ++frequencies[qIndex];
                for (int i = 0; i < vectorSize; i++) {
                    mse += Math.pow(((double) trV.getVector()[i] - (double) qVector[i]), 2);
                }
            }
            mse /= (double) trainingVectors.length;
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
        final int[] max = new int[vectorSize];
        // We have to initialize min to Max values.
        final int[] min = new int[vectorSize];
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

        final double[] perturbationVector = new double[vectorSize];
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
        final double[] vectorSum = new double[vectorSize];

        for (final TrainingVector trainingVector : trainingVectors) {
            for (int i = 0; i < vectorSize; i++) {
                vectorSum[i] += (double) trainingVector.getVector()[i];
            }
        }
        final int[] result = new int[vectorSize];
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
            final LearningCodebookEntry[] newCodebook = new LearningCodebookEntry[currentCodebookSize * 2];

            // Split each entry in codebook with fixed perturbation vector.
            for (final LearningCodebookEntry entryToSplit : codebook) {
                final double[] prtV;
                if (codebook.length == 1) {
                    assert (trainingVectors.length > 0) :
                            "There are no vectors from which to create perturbation " + "vector";
                    prtV = getPerturbationVector(trainingVectors);
                } else {
                    //                    assert (entryToSplit.getVectorCount() > 0) :
                    //                            "There are no vectors from which to create perturbation vector";

                    if (entryToSplit.getVectorCount() > 0) {
                        prtV = entryToSplit.getPerturbationVector();
                    } else {
                        prtV = generateRandomVectorDouble();
                    }


                }

                // We always want to carry zero vector to next iteration.
                if (isZeroVector(entryToSplit.getVector())) {
                    // Use zero vector in next iteration.
                    newCodebook[cbIndex++] = entryToSplit;

                    // Create another codebook entry from perturbation vector.
                    final int[] entryFromPrtVecValues = new int[prtV.length];
                    for (int i = 0; i < prtV.length; i++) {
                        final int value = (int) Math.floor(prtV[i]);
                        //                        assert (value >= 0) : "value is too low!";
                        //                        assert (value <= U16.Max) : "value is too big!";
                        entryFromPrtVecValues[i] = value;
                    }
                    newCodebook[cbIndex++] = new LearningCodebookEntry(entryFromPrtVecValues);
                    continue;
                }

                // Zero perturbation vector can't create two different entries.
                // Also vector with values from range [0.0;1.0) is considered as 'zero' vector,
                // because those values would be rounded down by the cast to integer.
                if (isPotentialZeroVector(prtV)) {
                    // The original entry is going to be moved to the next codebook with the new
                    // random entry, which will get improved in the LBG algorithm.
                    final int[] randomEntryValues = generateRandomVector();
                    newCodebook[cbIndex++] = entryToSplit;
                    newCodebook[cbIndex++] = new LearningCodebookEntry(randomEntryValues);
                } else {
                    final int[] left = new int[prtV.length];
                    final int[] right = new int[prtV.length];
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
            reportStatus("LBG::initializeCodebook() - Dividing codebook from %d --> %d",
                         currentCodebookSize,
                         2 * currentCodebookSize);
            currentCodebookSize *= 2;

            // Execute LBG Algorithm on current codebook to improve it.
            LBG(codebook);


            final double avgMse = averageMse(codebook);
            reportStatus("MSE of improved divided codebook: %f", avgMse);
        }
        return codebook;
    }

    /**
     * Generate random vector of size = this.vectorSize
     *
     * @return Generated random vector.
     */
    private int[] generateRandomVector() {
        final int[] randomVector = new int[vectorSize];
        final Random rnd = new Random();
        for (int i = 0; i < vectorSize; i++) {
            randomVector[i] = rnd.nextInt(U16.Max + 1);
        }
        return randomVector;
    }

    private double[] generateRandomVectorDouble() {
        final double[] randomVector = new double[vectorSize];
        final Random rnd = new Random();
        for (int i = 0; i < vectorSize; i++) {
            randomVector[i] = rnd.nextInt(U16.Max + 1);
        }
        return randomVector;
    }


    /**
     * Execute the LBG algorithm with default epsilon value.
     *
     * @param codebook Codebook to improve.
     */
    private void LBG(final LearningCodebookEntry[] codebook) {
        LBG(codebook, EPSILON);
    }

    /**
     * Execute the LBG algorithm with specific epsilon value.
     *
     * @param codebook Codebook to improve.
     * @param epsilon  Epsilon value.
     */
    private void LBG(final LearningCodebookEntry[] codebook, final double epsilon) {
        //this.verbose = true;
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
            for (final LearningCodebookEntry entry : codebook) {
                avgDistortion += entry.getAverageDistortion();
            }
            avgDistortion /= codebook.length;

            // Calculate distortion
            final double distortion = (previousDistortion - avgDistortion) / avgDistortion;
            reportStatus("LBG::LBG() - Iteration: %d  Distortion: %.5f", iteration++, distortion);


            if (Double.isNaN(distortion)) {
                reportStatus("Distortion is NaN. Stopping LBG::LBG().");
                break;
            }

            if (distortion > lastDist) {
                reportStatus("Previous distortion was better. Stopping LBG::LBG().");
                break;
            }

            // Check distortion against epsilon
            if (distortion < epsilon) {
                break;
            } else {
                previousDistortion = avgDistortion;
                lastDist = distortion;
            }
        }
    }

    /**
     * Assign each training vector to the closest codebook entry.
     *
     * @param codebook Vector codebook.
     */
    private void assignVectorsToClosestEntry(final LearningCodebookEntry[] codebook) {
        //        Stopwatch stopwatch = Stopwatch.startNew("assignVectorsToClosestEntry");
        if (workerCount > 1) {
            parallelAssignVectors(codebook);
            // In parallel version entry properties are already calculated.
        } else {
            defaultAssignVectors(codebook);
            // Calculate all the entry properties.
            calculateEntryProperties(codebook);
        }
        //        stopwatch.stop();
        //        if (this.verbose) {
        //            System.out.println(stopwatch);
        //        }

    }

    /**
     * Default version (Single Thread) of assigning vectors to closest codebook entry.
     *
     * @param codebook Vector codebook.
     */
    private void defaultAssignVectors(final LearningCodebookEntry[] codebook) {
        double minDist;
        int closestEntryIndex;

        for (final TrainingVector trainingVector : trainingVectors) {
            minDist = Double.POSITIVE_INFINITY;
            closestEntryIndex = -1;

            for (int entryIndex = 0; entryIndex < codebook.length; entryIndex++) {
                final double entryDistance = VectorQuantizer.distanceBetweenVectors(codebook[entryIndex].getVector(),
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
    private void parallelAssignVectors(final LearningCodebookEntry[] codebook) {
        final Thread[] workers = new Thread[workerCount];
        final int workSize = trainingVectors.length / workerCount;
        final EntryInfo[][] threadEntryInfos = new EntryInfo[workerCount][codebook.length];


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
        } catch (final InterruptedException e) {
            e.printStackTrace();
            assert (false) : "Failed parallel join";
        }

        // Combine all thread entry infos to final array distributed to the entries.
        final EntryInfo[] entryInfos = new EntryInfo[codebook.length];
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

    private void combine(final EntryInfo finalEntryInfo, final EntryInfo threadEntryInfo) {
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
    private void calculateEntryProperties(final LearningCodebookEntry[] codebook) {

        int value;
        final EntryInfo[] entryInfos = new EntryInfo[codebook.length];
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
    }


    /**
     * Fix all empty codebook entries.
     *
     * @param codebook Vector codebook.
     */
    private void fixEmptyEntries(final LearningCodebookEntry[] codebook) {
        int emptyEntryIndex = -1;
        for (int i = 0; i < codebook.length; i++) {
            if (codebook[i].getVectorCount() < 2) {
                emptyEntryIndex = i;
            }
        }

        boolean ableToFix = true;
        while (emptyEntryIndex != -1) {
            ableToFix = fixSingleEmptyEntry(codebook, emptyEntryIndex);
            if (!ableToFix) {
                break;
            }
            emptyEntryIndex = -1;
            for (int i = 0; i < codebook.length; i++) {
                if (codebook[i].getVectorCount() < 2) {
                    emptyEntryIndex = i;
                }
            }
        }

        if (ableToFix) {
            for (final LearningCodebookEntry lce : codebook) {
                assert (lce.getVectorCount() > 0) : "LearningCodebookEntry is empty!";
            }
        }
    }

    /**
     * Fix empty codebook entry by splitting the largest codebook entry.
     *
     * @param codebook        Vector codebook.
     * @param emptyEntryIndex Index of the empty entry.
     */
    private boolean fixSingleEmptyEntry(final LearningCodebookEntry[] codebook, final int emptyEntryIndex) {
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
        if (largestEntryIndex == emptyEntryIndex) {
            // Unable to find empty entry.
            return false;
        }
        // Assert that we have found some non empty codebook entry.
        //assert (largestEntryIndex != emptyEntryIndex) : "Unable to find biggest partition.";
        assert (codebook[largestEntryIndex].getVectorCount() > 0) : "Biggest partitions was empty before split";

        // Get training vectors assigned to the largest codebook entry.
        final TrainingVector[] largestPartitionVectors = getEntryTrainingVectors(largestEntryIndex,
                                                                                 codebook[largestEntryIndex].getVectorCount());

        // Choose random trainingVector from biggest partition and set it as new entry.
        final int randomIndex = new Random().nextInt(largestPartitionVectors.length);

        // Plane the new entry on the index of the empty entry.
        codebook[emptyEntryIndex] = new LearningCodebookEntry(largestPartitionVectors[randomIndex].getVector());


        // Speedup - speed the look for closest entry.

        final EntryInfo oldEntryInfo = new EntryInfo(vectorSize);
        final EntryInfo newEntryInfo = new EntryInfo(vectorSize);

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
        return true;
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

        int count = 0;
        for (final TrainingVector trainingVector : trainingVectors) {
            if (trainingVector.getEntryIndex() == entryIndex) {
                ++count;
            }
        }
        final TrainingVector[] vectors = new TrainingVector[count];

        for (final TrainingVector trainingVector : trainingVectors) {
            if (trainingVector.getEntryIndex() == entryIndex) {
                vectors[index++] = trainingVector;
            }
        }
        return vectors;
    }

    /**
     * Check whether all vector elements less than 1.0 .
     *
     * @param vector Vector array.
     * @return True if all elements less than 1.0.
     */
    private boolean isPotentialZeroVector(final double[] vector) {
        for (final double value : vector) {
            if (value >= 1.0) {
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
     * Get codebook size.
     *
     * @return Codebook size.
     */
    public int getCodebookSize() {
        return codebookSize;
    }
}
