package compression.de.shade;

import compression.U16;
import compression.de.DeException;
import compression.quantization.QTrainIteration;
import compression.utilities.Means;
import compression.utilities.Utils;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import compression.de.DEIndividual;
import compression.utilities.Stopwatch;

import java.util.ArrayList;

public class ILShadeSolver extends LShadeSolver {

    private double currentMutationGreediness;
    private double minMutationGreediness = 0.1;

    public ILShadeSolver(int dimension, int populationSize, int generationCount, int memorySize) {
        super(dimension, populationSize, generationCount, memorySize);

        maxMutationGreediness = 0.2;
        minMutationGreediness = 0.1;
        currentMutationGreediness = maxMutationGreediness;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public QTrainIteration[] train() throws DeException {
        final String delimiter = "-------------------------------------------";
        int maxNfe = (populationSize * generationCount);
        int nfe = 0;
        QTrainIteration[] solutionHistory = new QTrainIteration[generationCount];

        RandomGenerator rg = new MersenneTwister();
        initializeMemory(0.8, 0.5);
        ArrayList<Double> successfulCr = new ArrayList<Double>();
        ArrayList<Double> successfulF = new ArrayList<Double>();
        ArrayList<Double> absDelta = new ArrayList<Double>();

        generateInitialPopulation();
        double averageMSE = calculateFitnessForPopulationParallel(currentPopulation);

        UniformIntegerDistribution memoryIndexDist = new UniformIntegerDistribution(rg, 0, (memorySize - 1));
        UniformIntegerDistribution jRandDist = new UniformIntegerDistribution(rg, 0, (dimensionCount - 1));
        UniformRealDistribution crDist = new UniformRealDistribution(rg, 0.0, 1.0);

        Stopwatch stopwatch = new Stopwatch();

        for (int generation = 0; generation < generationCount; generation++) {
            stopwatch.restart();

            successfulCr.clear();
            successfulF.clear();
            absDelta.clear();
            StringBuilder generationLog = new StringBuilder(String.format("%s\niL-SHADE\nGeneration: %d\n", delimiter, (generation + 1)));

            currentPopulationSorted = createSortedCopyOfCurrentPopulation();
            DEIndividual[] offsprings = new DEIndividual[currentPopulationSize];

            UniformIntegerDistribution rndPopArchiveDist =
                    new UniformIntegerDistribution(rg, 0, ((currentPopulationSize - 1) + archive.size()));
            int pBestUpperLimit = (int) Math.floor(currentPopulationSize * currentMutationGreediness);
            UniformIntegerDistribution rndPBestDist = new UniformIntegerDistribution(rg, 0, (pBestUpperLimit - 1));
            UniformIntegerDistribution rndIndDist = new UniformIntegerDistribution(rg, 0, (currentPopulationSize - 1));

            for (int i = 0; i < currentPopulationSize; i++) {
                int randomMemIndex = memoryIndexDist.sample();
                if (randomMemIndex == (memorySize - 1)) {
                    memoryCr[randomMemIndex] = 0.9;
                    memoryF[randomMemIndex] = 0.9;
                }
                currentPopulation[i].setCrossoverProbability(iLShadeGenerateCrossoverProbability(randomMemIndex, generation));
                currentPopulation[i].setMutationFactor(iLShadeGenerateMutationFactor(randomMemIndex, generation));

                DEIndividual x_p_Best = getRandomFromPBest(rndPBestDist, currentPopulation[i]);
                DEIndividual x_r1 = getRandomFromCurrentPopulation(rndIndDist, currentPopulation[i], x_p_Best);
                DEIndividual x_r2 = getRandomFromPopulationAndArchive(rndPopArchiveDist, currentPopulation[i], x_p_Best, x_r1);

                final int[] mutationVector = createMutationVectorCurrentToPBest(currentPopulation[i], x_p_Best, x_r1, x_r2);
                offsprings[i] = currentPopulation[i].createOffspringBinominalCrossover(mutationVector, jRandDist.sample(), crDist);
            }

            calculateFitnessForPopulationParallel(offsprings);
            nfe += currentPopulationSize;

            DEIndividual[] nextPopulation = new DEIndividual[currentPopulationSize];
            // NOTE(Moravec): We are minimalizing!
            for (int i = 0; i < currentPopulationSize; i++) {
                final DEIndividual old = currentPopulation[i];
                if (offsprings[i].getFitness() <= old.getFitness()) {
                    nextPopulation[i] = offsprings[i];

                    if (offsprings[i].getFitness() < old.getFitness()) {
                        archive.add(old);
                        absDelta.add(Math.abs(offsprings[i].getFitness() - currentPopulation[i].getFitness()));
                        successfulCr.add(old.getCrossoverProbability());
                        successfulF.add(old.getMutationFactor());
                    }
                } else {
                    nextPopulation[i] = currentPopulation[i];
                }
            }

            updateMemory(successfulCr, successfulF, absDelta);
            currentPopulation = nextPopulation;
            applyLinearReductionOfPopulationSize(nfe, maxNfe);
            truncateArchive();
            updateMutationGreediness(nfe, maxNfe);

            averageMSE = getMseFromCalculatedFitness(currentPopulation);

            // NOTE(Moravec): After LRPS the population is sorted according.
            final double bestMSE = currentPopulation[0].getFitness();
            final double bestPSNR = Utils.calculatePsnr(bestMSE, U16.Max);
            final double averagePSNR = Utils.calculatePsnr(averageMSE, U16.Max);

            solutionHistory[generation] = new QTrainIteration(generation, averageMSE, bestMSE, averagePSNR, bestPSNR);

            stopwatch.stop();

            generationLog.append(String.format("Current population size: %d\n", currentPopulationSize));
            generationLog.append(String.format("Mutation greediness: %.5f\n", currentMutationGreediness));
            generationLog.append(String.format("Current best fitness: %.5f Current PSNR: %.5f dB", bestMSE, bestPSNR));
            generationLog.append(String.format("\nAvg. cost(after LPSR): %.6f\nAvg. PSNR (after LPSR): %.6f dB\nIteration finished in: %d ms", averageMSE, averagePSNR, stopwatch.totalElapsedMilliseconds()));
            System.out.println(generationLog.toString());
        }

        return solutionHistory;
    }

    private double iLShadeGenerateCrossoverProbability(final int memIndex, final int currentGeneration) {
        double cr = generateCrossoverProbability(memIndex);
        if ((double) currentGeneration < (0.25 * (double) generationCount)) {
            cr = Math.max(cr, 0.5);
        } else if ((double) currentGeneration < (0.5 * (double) generationCount)) {
            cr = Math.max(cr, 0.25);
        }
        return cr;
    }


    private double iLShadeGenerateMutationFactor(final int memIndex, final int currentGeneration) {
        double f = generateMutationFactor(memIndex);
        if ((double) currentGeneration < (0.25 * (double) generationCount)) {
            f = Math.max(f, 0.7);
        } else if ((double) currentGeneration < (0.5 * (double) generationCount)) {
            f = Math.max(f, 0.8);
        } else if ((double) currentGeneration < (0.75 * (double) generationCount)) {
            f = Math.max(f, 0.9);
        }
        return f;
    }

    private void updateMutationGreediness(final int nfes, final int maxNfes) {
        currentMutationGreediness = (((maxMutationGreediness - minMutationGreediness) / (double) maxNfes) * nfes) + minMutationGreediness;
    }

    @Override
    protected void updateMemory(final ArrayList<Double> successfulCr,
                                final ArrayList<Double> successfulF,
                                final ArrayList<Double> absDelta) {

        if ((!successfulCr.isEmpty()) && (!successfulF.isEmpty())) {
            assert ((absDelta.size() == successfulCr.size()) && (successfulCr.size() == successfulF.size()));

            double[] weights = calculateLehmerWeihts(absDelta);

            if ((Double.isNaN(memoryCr[memoryIndex])) || (Utils.arrayListMax(successfulCr) == 0)) {
                memoryCr[memoryIndex] = Double.NaN;
            } else {
                memoryCr[memoryIndex] = ((Means.weightedLehmerMean(successfulCr, weights) + memoryCr[memoryIndex]) / 2.0);
            }
            memoryF[memoryIndex] = ((Means.weightedLehmerMean(successfulF, weights) + memoryF[memoryIndex]) / 2.0);
            ++memoryIndex;
            if (memoryIndex >= memorySize) {
                memoryIndex = 0;
            }
        }
    }

    public double getMinMutationGreediness() {
        return minMutationGreediness;
    }

    public void setMinMutationGreediness(double minMutationGreediness) {
        this.minMutationGreediness = minMutationGreediness;
    }

    @Override
    public void setMaxMutationGreediness(double maxMutationGreediness) {
        super.setMaxMutationGreediness(maxMutationGreediness);
        currentMutationGreediness = maxMutationGreediness;
    }
}
