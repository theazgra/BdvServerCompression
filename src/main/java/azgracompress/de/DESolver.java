package azgracompress.de;

import azgracompress.U16;
import azgracompress.de.jade.RunnablePopulationFitness;
import azgracompress.utilities.Utils;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.Arrays;

public abstract class DESolver implements IDESolver {

    public static final int MINIMAL_POPULATION_SIZE = 5;
    protected int minConstraint = U16.Min;
    protected int maxConstraint = U16.Max;
    protected int populationSize;
    protected int generationCount;
    protected int dimensionCount;
    protected int[] trainingData;
    protected int threadCount;
    protected int currentPopulationSize;

    protected DEIndividual bestSolution = null;

    protected DEIndividual[] currentPopulation;
    protected DEIndividual[] currentPopulationSorted;

    public DESolver(final int dimension, final int populationSize, final int generationCount) {
        //threadCount = Runtime.getRuntime().availableProcessors() - 1;
        // NOTE(Moravec): Let's go with 4 threads for now.
        threadCount = 4;
        //assert (threadCount > 1);

        this.dimensionCount = dimension;
        this.populationSize = populationSize;
        this.generationCount = generationCount;
        this.currentPopulationSize = populationSize;
    }

    /**
     * Generate individual attributes, so that all are unique.
     *
     * @param distribution Uniform integer distribution with constraints.
     * @return Array of unique attributes.
     */
    private int[] generateIndividualAttribues(UniformIntegerDistribution distribution) {

        int[] attributes = new int[dimensionCount];
        // NOTE(Moravec):   We are cheting here, when we set the first attribute to be zero, because we know that this is the best value there is.
        attributes[0] = minConstraint;
        for (int dim = 1; dim < dimensionCount; dim++) {
            int rndValue = distribution.sample();
            while (Utils.arrayContainsToIndex(attributes, dim, rndValue)) {
                rndValue = distribution.sample();
            }
            attributes[dim] = rndValue;
        }
        Arrays.sort(attributes);
        return attributes;
    }

    /**
     * Generate initial population based on population size and constraints. Also allocates archive.
     *
     * @throws DeException Throws exception on wrong population size or wrong dimension.
     */
    protected void generateInitialPopulation() throws DeException {
        assertPopulationSize();
        assertDimension();

        currentPopulation = new DEIndividual[populationSize];
        UniformIntegerDistribution uniformIntDistribution = new UniformIntegerDistribution(new MersenneTwister(), minConstraint, maxConstraint);

        for (int individualIndex = 0; individualIndex < populationSize; individualIndex++) {
            int[] attributes = generateIndividualAttribues(uniformIntDistribution);
            currentPopulation[individualIndex] = new DEIndividual(attributes);
        }
    }

    protected double getMseFromCalculatedFitness(final DEIndividual[] population) {
        double mse = 0.0;
        for (final DEIndividual individual : population) {
            assert (individual.isFitnessCached());
            mse += individual.getFitness();
        }
        return (mse / (double) population.length);
    }

    /**
     * Parallelized calculation of fitness values for individuals in current population.
     */
    protected double calculateFitnessForPopulationParallel(DEIndividual[] population) {

        double avg = 0.0;
        RunnablePopulationFitness[] workerInfos = new RunnablePopulationFitness[threadCount];
        Thread[] workers = new Thread[threadCount];
        int threadWorkSize = population.length / threadCount;

        for (int workerId = 0; workerId < threadCount; workerId++) {
            int workerFrom = workerId * threadWorkSize;
            int workerTo = (workerId == (threadCount - 1)) ? population.length : (workerId * threadWorkSize) + threadWorkSize;
            workerInfos[workerId] = new RunnablePopulationFitness(trainingData, population, workerFrom, workerTo);
            workers[workerId] = new Thread(workerInfos[workerId]);
            workers[workerId].start();
        }

        try {
            for (int workerId = 0; workerId < threadCount; workerId++) {
                workers[workerId].join();
                avg += workerInfos[workerId].getTotalMse();
            }
        } catch (InterruptedException ignored) {
        }


        avg /= (double) population.length;
        return avg;
    }

    /**
     * Select random individual from p*100% top individuals.
     *
     * @param pBestDistribution Distribution for p*100% random.
     * @param others            Other individuals.
     * @return Random individual from p*100%.
     */
    protected DEIndividual getRandomFromPBest(UniformIntegerDistribution pBestDistribution,
                                              final DEIndividual... others) {
        assert (currentPopulationSorted != null);

        int rndIndex = pBestDistribution.sample();
        while (Utils.arrayContains(others, currentPopulationSorted[rndIndex])) {
            rndIndex = pBestDistribution.sample();
        }
        return currentPopulationSorted[rndIndex];
    }

    /**
     * Get random individual from current population, distinct from the other.
     *
     * @param rndIndDist Distribution of current population random.
     * @param others     Other individuals.
     * @return Distinct random individual from the another one.
     */
    protected DEIndividual getRandomFromCurrentPopulation(UniformIntegerDistribution rndIndDist,
                                                          final DEIndividual... others) {
        DEIndividual rndIndiv = currentPopulation[rndIndDist.sample()];
        while (Utils.arrayContains(others, rndIndiv)) {
            rndIndiv = currentPopulation[rndIndDist.sample()];
        }
        return rndIndiv;
    }

    protected int[] createMutationVectorCurrentToPBest(final DEIndividual current,
                                                       final DEIndividual x_p_Best,
                                                       final DEIndividual x_r1,
                                                       final DEIndividual x_r2) {
        int[] mutationVector = new int[dimensionCount];

        double mutationFactor = current.getMutationFactor();
        for (int j = 0; j < dimensionCount; j++) {

            mutationVector[j] = (int) Math.floor(current.getAttribute(j) +
                    (mutationFactor * ((double) x_p_Best.getAttribute(j) - current.getAttribute(j))) +
                    (mutationFactor * ((double) x_r1.getAttribute(j) - x_r2.getAttribute(j))));

            if (mutationVector[j] < minConstraint) {
                mutationVector[j] = (minConstraint + current.getAttribute(j)) / 2;
            } else if (mutationVector[j] > maxConstraint) {
                mutationVector[j] = (maxConstraint + current.getAttribute(j)) / 2;
            }
        }
        return mutationVector;
    }

    protected DEIndividual[] createSortedCopyOfCurrentPopulation() {
        currentPopulationSorted = Arrays.copyOf(currentPopulation, currentPopulation.length);
        Arrays.sort(currentPopulationSorted);
        return currentPopulationSorted;
    }

    protected double generateMutationFactor(CauchyDistribution dist) {
        double factor = dist.sample();
        while (factor <= 0.0) { // || Double.isNaN(factor)) {
            factor = dist.sample();
        }
        if (factor > 1.0) {
            factor = 1.0;
        }
        assert (factor > 0.0 && factor <= 1.0);
        return factor;
    }

    protected double generateCrossoverProbability(NormalDistribution dist) {
        double prob = dist.sample();

        // NOTE(Moravec): Sometimes dist.sample() returns NaN...
        while (Double.isNaN(prob)) {
            prob = dist.sample();
        }


        if (prob < 0.0) {
            prob = 0.0;
        } else if (prob > 1.0) {
            prob = 1.0;
        }
        assert (prob >= 0.0 && prob <= 1.0);
        return prob;
    }

    protected void assertPopulationSize() throws DeException {
        if (populationSize < MINIMAL_POPULATION_SIZE) {
            throw new DeException("Population size is too low. Required population size >= 5.");
        }
    }

    protected void assertDimension() throws DeException {
        if (dimensionCount < 1) {
            throw new DeException("Dimension is too low. Required dimension >= 1.");
        }
    }

    @Override
    public void setMinimalValueConstraint(int min) {
        minConstraint = min;
    }

    @Override
    public void setMaximalValueConstraint(int max) {
        maxConstraint = max;
    }

    @Override
    public void setPopulationSize(int populationSize) throws DeException {
        assertPopulationSize();
        this.populationSize = populationSize;
        currentPopulationSize = populationSize;
    }

    @Override
    public void setGenerationCount(int generationCount) {
        this.generationCount = generationCount;
    }

    @Override
    public void setDimensionCount(int dimensionCount) {
        this.dimensionCount = dimensionCount;
    }

    @Override
    public void setTrainingData(int[] data) {
        trainingData = data;
    }

    @Override
    public IDEIndividual getBestSolution() {
        return bestSolution;
    }
}
