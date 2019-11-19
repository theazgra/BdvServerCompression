package compression.de.shade;

import compression.U16;
import compression.de.DeException;
import compression.utilities.Utils;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import compression.de.DEIndividual;
import compression.de.DESolverWithArchive;
import compression.de.DeHistory;
import compression.utilities.Means;
import compression.utilities.Stopwatch;

import java.util.ArrayList;

public class LShadeSolver extends DESolverWithArchive {
    protected int memorySize;
    protected int memoryIndex = 0;
    protected double[] memoryCr;
    protected double[] memoryF;

    protected double maxMutationGreediness = 0.1;

    protected int minimalPopulationSize = MINIMAL_POPULATION_SIZE;

    public LShadeSolver(int dimension, int populationSize, int generationCount, int memorySize) {
        super(dimension, populationSize, generationCount, populationSize);
        this.memorySize = memorySize;
    }

    protected void initializeMemory(final double initialCrValue, final double initialFValue) {
        memoryIndex = 0;
        memoryCr = new double[memorySize];
        memoryF = new double[memorySize];
        for (int memIndex = 0; memIndex < memorySize; memIndex++) {
            memoryCr[memIndex] = initialCrValue;
            memoryF[memIndex] = initialFValue;
        }
    }


    protected double generateCrossoverProbability(final int memIndex) {
        double memCr = memoryCr[memIndex];
        if (Double.isNaN(memCr)) {
            return 0.0;
        } else {
            return generateCrossoverProbability(new NormalDistribution(memCr, 0.1));
        }
    }

    protected double generateMutationFactor(final int memIndex) {
        return generateMutationFactor(new CauchyDistribution(memoryF[memIndex], 0.1));
    }

    @Override
    public DeHistory[] train() throws DeException {
        final String delimiter = "-------------------------------------------";
        int maxNfe = (populationSize * generationCount);
        int nfe = 0;
        DeHistory[] solutionHistory = new DeHistory[generationCount];

        RandomGenerator rg = new MersenneTwister();
        initializeMemory(0.5, 0.5);
        ArrayList<Double> successfulCr = new ArrayList<Double>();
        ArrayList<Double> successfulF = new ArrayList<Double>();
        ArrayList<Double> absDelta = new ArrayList<Double>();

        generateInitialPopulation();
        double avgFitness = calculateFitnessForPopulationParallel(currentPopulation);

        UniformIntegerDistribution memoryIndexDist = new UniformIntegerDistribution(rg, 0, (memorySize - 1));
        UniformIntegerDistribution jRandDist = new UniformIntegerDistribution(rg, 0, (dimensionCount - 1));
        UniformRealDistribution crDist = new UniformRealDistribution(rg, 0.0, 1.0);

        Stopwatch stopwatch = new Stopwatch();

        for (int generation = 0; generation < generationCount; generation++) {
            stopwatch.restart();

            successfulCr.clear();
            successfulF.clear();
            absDelta.clear();
            StringBuilder generationLog = new StringBuilder(String.format("%s\nGeneration: %d\n", delimiter, (generation + 1)));

            currentPopulationSorted = createSortedCopyOfCurrentPopulation();
            DEIndividual[] offsprings = new DEIndividual[currentPopulationSize];

            UniformIntegerDistribution rndPopArchiveDist =
                    new UniformIntegerDistribution(rg, 0, ((currentPopulationSize - 1) + archive.size()));
            int pBestUpperLimit = (int) Math.floor(currentPopulationSize * maxMutationGreediness);
            UniformIntegerDistribution rndPBestDist = new UniformIntegerDistribution(rg, 0, (pBestUpperLimit - 1));
            UniformIntegerDistribution rndIndDist = new UniformIntegerDistribution(rg, 0, (currentPopulationSize - 1));

            for (int i = 0; i < currentPopulationSize; i++) {
                int randomMemIndex = memoryIndexDist.sample();
                currentPopulation[i].setCrossoverProbability(generateCrossoverProbability(randomMemIndex));
                currentPopulation[i].setMutationFactor(generateMutationFactor(randomMemIndex));

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
            avgFitness = getMseFromCalculatedFitness(currentPopulation);

            // NOTE(Moravec): After LRPS the population is sorted according.
            final double currentBestFitness = currentPopulation[0].getFitness();
            final double psnr = Utils.calculatePsnr(currentBestFitness, U16.Max);
            final double avgPsnr = Utils.calculatePsnr(avgFitness, U16.Max);
            solutionHistory[generation] = new DeHistory(generation, avgFitness, currentBestFitness, psnr, avgPsnr);

            stopwatch.stop();

            generationLog.append(String.format("Archive size after truncate: %d\n", archive.size()));
            generationLog.append(String.format("Current population size: %d\n", currentPopulationSize));
            generationLog.append(String.format("Current best fitness: %.5f Current PSNR: %.5f dB", currentBestFitness, psnr));
            generationLog.append(String.format("\nAvg. cost(after LPSR): %.6f\nAvg. PSNR (after LPSR): %.6f dB\nIteration finished in: %d ms", avgFitness, avgPsnr, stopwatch.totalElapsedMilliseconds()));
            System.out.println(generationLog.toString());
        }

        return solutionHistory;
    }

    protected void applyLinearReductionOfPopulationSize(final int nfe, final int maxNfe) {
        final int oldPopulationSize = currentPopulationSize;
        currentPopulationSize = getNewPopulationSize(nfe, maxNfe);
        maxArchiveSize = currentPopulationSize;
        if (currentPopulationSize < oldPopulationSize) {
            DEIndividual[] reducedPopulation = new DEIndividual[currentPopulationSize];
            System.arraycopy(currentPopulationSorted, 0, reducedPopulation, 0, currentPopulationSize);
            currentPopulation = reducedPopulation;
        }
    }

    private int getNewPopulationSize(final int nfe, final int maxNfe) {
        int newPopulationSize = (int) Math.round(((((double) minimalPopulationSize - (double) populationSize) / (double) maxNfe) * (double) nfe) + (double) populationSize);
        return newPopulationSize;
    }

    protected double[] calculateLehmerWeihts(final ArrayList<Double> absDelta) {
        int kCount = absDelta.size();
        double[] weights = new double[kCount];
        for (int k = 0; k < kCount; k++) {

            final double numerator = absDelta.get(k);
            final double denominator = Utils.arrayListSum(absDelta);
            weights[k] = (numerator / denominator);
        }
        return weights;
    }

    protected void updateMemory(final ArrayList<Double> successfulCr,
                                final ArrayList<Double> successfulF,
                                final ArrayList<Double> absDelta) {

        if ((!successfulCr.isEmpty()) && (!successfulF.isEmpty())) {
            assert ((absDelta.size() == successfulCr.size()) && (successfulCr.size() == successfulF.size()));
            double[] weights = calculateLehmerWeihts(absDelta);

            if ((Double.isNaN(memoryCr[memoryIndex])) || (Utils.arrayListMax(successfulCr) == 0)) {
                memoryCr[memoryIndex] = Double.NaN;
            } else {
                memoryCr[memoryIndex] = Means.weightedLehmerMean(successfulCr, weights);
            }
            memoryF[memoryIndex] = Means.weightedLehmerMean(successfulF, weights);
            ++memoryIndex;
            if (memoryIndex >= memorySize) {
                memoryIndex = 0;
            }
        }

//        StringBuilder sb = new StringBuilder();
//        sb.append("MEMORY F: ");
//        for (int i = 0; i < memoryF.length; i++) {
//            sb.append(String.format("%.5f    ", memoryF[i]));
//        }
//        sb.append("\n");
//        sb.append("MEMORY Cr: ");
//        for (int i = 0; i < memoryCr.length; i++) {
//            sb.append(String.format("%.5f    ", memoryCr[i]));
//        }
//        System.out.println(sb.toString());
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(final int memorySize) {
        this.memorySize = memorySize;
    }

    public void setMaxMutationGreediness(final double maxMutationGreediness) {
        this.maxMutationGreediness = maxMutationGreediness;
    }

    public double getMaxMutationGreediness() {
        return maxMutationGreediness;
    }

    public int getMinimalPopulationSize() {
        return minimalPopulationSize;
    }

    public void setMinimalPopulationSize(int minimalPopulationSize) {
        this.minimalPopulationSize = minimalPopulationSize;
    }
}
