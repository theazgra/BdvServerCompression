package quantization.de.jade;

import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import quantization.Quantizer;
import quantization.U16;
import quantization.Utils;
import quantization.de.IDESolver;
import quantization.de.IIndividual;
import quantization.de.DeException;
import quantization.utilities.Stopwatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class JadeSolver implements IDESolver {
    public static final int MinimalPopulationSize = 5;
    private int m_minConstraint = U16.Min;
    private int m_maxConstraint = U16.Max;

    private int m_populationSize;
    private int m_generationCount;
    private int m_dimension;

    private double m_muCr = 0.5;
    private double m_muF = 0.5;

    private double m_parameterAdaptationRate = 0.05;
    private double m_mutationGreediness = 0.1;

    private JadeIndividual[] m_currentPopulation;

    private int m_maxArchiveSize;
    private ArrayList<JadeIndividual> m_archive;

    private RandomGenerator rg;
    private int[] m_trainingData;

    private int m_workerCount;

    public JadeSolver() {
        rg = new MersenneTwister();
        m_workerCount = Runtime.getRuntime().availableProcessors() - 1;
        assert (m_workerCount > 0);
    }

    public JadeSolver(final int dimension, final int populationSize, final int generationCount) {
        this();
        m_dimension = dimension;
        m_populationSize = populationSize;
        m_generationCount = generationCount;
        m_maxArchiveSize = m_populationSize;
    }

    public JadeSolver(final int dimension, final int populationSize, final int generationCount,
                      final double parameterAdaptationRate, final double mutationGreediness) {

        this(dimension, populationSize, generationCount);
        m_parameterAdaptationRate = parameterAdaptationRate;
        m_mutationGreediness = mutationGreediness;
    }

    private double arithmeticMean(final ArrayList<Double> values) {
        double sum = 0.0;
        for (int i = 0; i < values.size(); i++) {
            sum += values.get(i);
        }
        double result = sum / (double) values.size();
        return result;
    }

    private double lehmerMean(final ArrayList<Double> values) {
        double numerator = 0.0;
        double denominator = 0.0;
        double value;
        for (int i = 0; i < values.size(); i++) {
            value = values.get(i);
            numerator += Math.pow(value, 2);
            denominator += value;
        }
        double result = numerator / denominator;
        return result;
    }

    private void assertPopulationSize() throws DeException {
        if (m_populationSize < MinimalPopulationSize) {
            throw new DeException("Population size is too low. Required population size >= 5.");
        }
    }

    private void assertDimension() throws DeException {
        if (m_dimension < 1) {
            throw new DeException("Dimension is too low. Required dimension >= 1.");
        }
    }

    /**
     * Generate individual attributes, so that all are unique.
     *
     * @param distribution Uniform integer distribution with constraints.
     * @return Array of unique attributes.
     */
    private int[] generateIndividualAttribues(UniformIntegerDistribution distribution) {
        int[] attributes = new int[m_dimension];
        for (int dim = 0; dim < m_dimension; dim++) {
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
    private void generateInitialPopulation() throws DeException {
        assertPopulationSize();
        assertDimension();
        assert m_populationSize > 0;
        m_currentPopulation = new JadeIndividual[m_populationSize];
        m_archive = new ArrayList<JadeIndividual>(m_maxArchiveSize);
        UniformIntegerDistribution uniformIntDistribution = new UniformIntegerDistribution(rg, m_minConstraint, m_maxConstraint);

        // NOTE(Moravec): What if we cheat and hardcode the first and last parameter?

        for (int individualIndex = 0; individualIndex < m_populationSize; individualIndex++) {
            int[] individualAttributes = generateIndividualAttribues(uniformIntDistribution);
            m_currentPopulation[individualIndex] = JadeIndividual.NewIndividual(individualAttributes);
        }
    }

    /**
     * Select random individual from p*100% top individuals.
     *
     * @param pBestDistribution Distribution for p*100% random.
     * @param others            Other individuals.
     * @return Random individual from p*100%.
     */
    private JadeIndividual getRandomFromPBest(UniformIntegerDistribution pBestDistribution,
                                              final JadeIndividual... others) {
        JadeIndividual[] sorted = Arrays.copyOf(m_currentPopulation, m_currentPopulation.length);
        Arrays.sort(sorted);

        int rndIndex = pBestDistribution.sample();
        while (Utils.arrayContains(others, sorted[rndIndex])) {
            rndIndex = pBestDistribution.sample();
        }
        return sorted[rndIndex];
    }

    /**
     * Get random individual from current population, distinct from the other.
     *
     * @param rndIndDist Distribution of current population random.
     * @param others     Other individuals.
     * @return Distinct random individual from the another one.
     */
    private JadeIndividual getRandomFromCurrentPopulation(UniformIntegerDistribution rndIndDist, final JadeIndividual... others) {
        JadeIndividual rndIndiv = m_currentPopulation[rndIndDist.sample()];
        while (Utils.arrayContains(others, rndIndiv)) {
            rndIndiv = m_currentPopulation[rndIndDist.sample()];
        }
        return rndIndiv;
    }

    /**
     * Get random individual (different from two others) from union of current population and archive.
     *
     * @param rndUnionDist Random distribution for the union of current population and archive.
     * @param others       Other individuals.
     * @return Random individual from union of current population and archive.
     */
    private JadeIndividual getRandomFromUnion(UniformIntegerDistribution rndUnionDist,
                                              final JadeIndividual... others) {

        int rndIndex = rndUnionDist.sample();
        JadeIndividual rndIndiv = (rndIndex >= m_populationSize) ? m_archive.get(rndIndex - m_populationSize) : m_currentPopulation[rndIndex];
        while (Utils.arrayContains(others, rndIndiv)) {
            rndIndex = rndUnionDist.sample();
            rndIndiv = (rndIndex >= m_populationSize) ? m_archive.get(rndIndex - m_populationSize) : m_currentPopulation[rndIndex];
        }

        return rndIndiv;
    }


    private double getIndividualFitness(final JadeIndividual individual) {
        Quantizer individualQuantizer = new Quantizer(m_minConstraint, m_maxConstraint, individual.getAttributes());
        double mse = individualQuantizer.getMse(m_trainingData);
        return mse;
    }

    /**
     * Parallelized calculation of fitness values for individuals in current population.
     */
    private double calculateFitnessForPopulationParallel() {

        double avg = 0.0;
        Stopwatch s = new Stopwatch();
        s.start();

        RunnablePopulationFitness[] workerInfos = new RunnablePopulationFitness[m_workerCount];
        Thread[] workers = new Thread[m_workerCount];
        int threadWorkSize = m_populationSize / m_workerCount;

        for (int workerId = 0; workerId < m_workerCount; workerId++) {
            int workerFrom = workerId * threadWorkSize;
            int workerTo = (workerId == (m_workerCount - 1)) ? m_populationSize : (workerId * threadWorkSize) + threadWorkSize;
            workerInfos[workerId] = new RunnablePopulationFitness(m_trainingData, m_currentPopulation, workerFrom, workerTo);
            workers[workerId] = new Thread(workerInfos[workerId]);
            workers[workerId].start();
        }

        try {
            for (int workerId = 0; workerId < m_workerCount; workerId++) {
                workers[workerId].join();
                avg += workerInfos[workerId].getTotalMse();
            }
        } catch (InterruptedException ignored) {
        }


        avg /= (double) m_populationSize;
        s.stop();
        System.out.println("Calculated population fitness in [ms]: " + s.totalElapsedMilliseconds());

        return avg;
    }

    private int[] createMutationVector(final JadeIndividual current,
                                       final JadeIndividual x_p_Best,
                                       final JadeIndividual x_r1,
                                       final JadeIndividual x_r2) {
        int[] mutationVector = new int[m_dimension];

        double mutationFactor = current.getMutationFactor();
        for (int j = 0; j < m_dimension; j++) {

            mutationVector[j] = (int) Math.floor(current.getAttribute(j) +
                    (mutationFactor * ((double) x_p_Best.getAttribute(j) - current.getAttribute(j))) +
                    (mutationFactor * ((double) x_r1.getAttribute(j) - x_r2.getAttribute(j))));

            if (mutationVector[j] < m_minConstraint) {
                mutationVector[j] = (m_minConstraint + current.getAttribute(j)) / 2;
            } else if (mutationVector[j] > m_maxConstraint) {
                mutationVector[j] = (m_maxConstraint + current.getAttribute(j)) / 2;
                assert (mutationVector[j] <= m_maxConstraint);
            }
        }


        return mutationVector;
    }

    private void truncateArchive() {
        int deleteCount = m_archive.size() - m_maxArchiveSize;
        if (deleteCount > 0) {
            Random random = new Random();

            for (int i = 0; i < deleteCount; i++) {
                m_archive.remove(random.nextInt(m_archive.size()));
            }
        }
        assert (m_archive.size() <= m_maxArchiveSize);
    }

    private double generateMutationFactor(CauchyDistribution dist) {
        double factor = dist.sample();
        while (factor <= 0.0) {
            factor = dist.sample();
        }
        if (factor > 1.0) {
            factor = 1.0;
        }
        assert (factor > 0.0 && factor <= 1.0);
        return factor;
    }

    private double generateCrossoverProbability(NormalDistribution dist) {
        double prob = dist.sample();
        if (prob < 0.0) {
            prob = 0.0;
        } else if (prob > 1.0) {
            prob = 1.0;
        }
        return prob;
    }

    @Override
    public void train() throws DeException {
        if (m_trainingData == null || m_trainingData.length <= 0) {
            throw new DeException("Training data weren't set.");
        }
        m_muCr = 0.5;
        m_muF = 0.5;

        int pBestUpperLimit = (int) Math.floor(m_populationSize * m_mutationGreediness);
        UniformIntegerDistribution rndPBestDist =
                new UniformIntegerDistribution(rg, 0, (pBestUpperLimit - 1));

        UniformIntegerDistribution rndIndDist =
                new UniformIntegerDistribution(rg, 0, (m_populationSize - 1));

        UniformIntegerDistribution rndJRandDist = new UniformIntegerDistribution(rg, 0, (m_dimension - 1));

        UniformRealDistribution rndCrDist = new UniformRealDistribution(rg, 0.0, 1.0);


        generateInitialPopulation();
        double avgFitness = calculateFitnessForPopulationParallel();
        System.out.println(String.format("Generation %d average fitness(COST): %.5f", 0, avgFitness));

        ArrayList<Double> successfulCr = new ArrayList<Double>(m_populationSize);
        ArrayList<Double> successfulF = new ArrayList<Double>(m_populationSize);

        NormalDistribution crNormalDistribution;
        CauchyDistribution fCauchyDistribution;
        for (int generation = 0; generation < m_generationCount; generation++) {

            successfulCr.clear();
            successfulF.clear();
            JadeIndividual[] nextPopulation = new JadeIndividual[m_populationSize];


            UniformIntegerDistribution rndPopArchiveDist =
                    new UniformIntegerDistribution(rg, 0, ((m_populationSize - 1) + m_archive.size()));

            crNormalDistribution = new NormalDistribution(rg, m_muCr, 0.1);
            fCauchyDistribution = new CauchyDistribution(rg, m_muF, 0.1);

            // NOTE(Moravec): Inner code could be parallelized.
            for (int i = 0; i < m_populationSize; i++) {

                JadeIndividual current = m_currentPopulation[i];
                current.setCrossoverProbability(generateCrossoverProbability(crNormalDistribution));
                assert (current.getCrossoverProbability() >= 0.0 && current.getCrossoverProbability() <= 1.0);
                current.setMutationFactor(generateMutationFactor(fCauchyDistribution));

                JadeIndividual x_p_Best = getRandomFromPBest(rndPBestDist, current);
                JadeIndividual x_r1 = getRandomFromCurrentPopulation(rndIndDist, current, x_p_Best);
                JadeIndividual x_r2 = getRandomFromUnion(rndPopArchiveDist, current, x_p_Best, x_r1);

                int[] mutationVector = createMutationVector(current, x_p_Best, x_r1, x_r2);
                int jRand = rndJRandDist.sample();

                JadeIndividual offspring = current.createOffspring(mutationVector, jRand, rndCrDist);
                double offspringFitness = getIndividualFitness(offspring);

                // NOTE(Moravec): We are minimalizing!
                if (offspringFitness <= current.getFitness()) {
                    nextPopulation[i] = offspring;
                    successfulCr.add(current.getCrossoverProbability());
                    successfulF.add(current.getMutationFactor());
                    m_archive.add(current);
                } else {
                    nextPopulation[i] = current;
                }
            }
            double oldMuCr = m_muCr, oldMuF = m_muF;
            m_muCr = ((1.0 - m_parameterAdaptationRate) * m_muCr) + (m_parameterAdaptationRate * arithmeticMean(successfulCr));
            m_muF = ((1.0 - m_parameterAdaptationRate) * m_muF) + (m_parameterAdaptationRate * lehmerMean(successfulF));
            System.out.println(String.format("Old μCR: %.4f    New μCR: %.4f\nOld μF: %.4f    New μF: %.4f",
                    oldMuCr, m_muCr, oldMuF, m_muF));
            truncateArchive();
            m_currentPopulation = nextPopulation;
            avgFitness = calculateFitnessForPopulationParallel();
            System.out.println(String.format("Generation %d average fitness(COST): %.5f", (generation + 1), avgFitness));
        }
    }

    @Override
    public void setTrainingData(int[] data) {
        m_trainingData = data;
    }

    @Override
    public void setMinimalValueConstraint(final int min) {
        m_minConstraint = min;
    }

    @Override
    public void setMaximalValueConstraint(final int max) {
        m_maxConstraint = max;
    }

    @Override
    public void setPopulationSize(final int populationSize) throws DeException {
        assertPopulationSize();
        m_populationSize = populationSize;
    }

    @Override
    public void setGenerationCount(final int generationCount) {
        m_generationCount = generationCount;
    }

    @Override
    public void setDimension(final int dimension) {
        m_dimension = dimension;
    }

    @Override
    public IIndividual getBestSolution() {
        return null;
    }

    public double getParameterAdaptationRate() {
        return m_parameterAdaptationRate;
    }

    public void setParameterAdaptationRate(double parameterAdaptationRate) {
        this.m_parameterAdaptationRate = parameterAdaptationRate;
    }

    public double getMutationGreediness() {
        return m_mutationGreediness;
    }

    public void setMutationGreediness(double mutationGreediness) {
        this.m_mutationGreediness = mutationGreediness;
    }

    public int getMaxArchiveSize() {
        return m_maxArchiveSize;
    }

    public void setMaxArchiveSize(int maxArchiveSize) {
        this.m_maxArchiveSize = maxArchiveSize;
    }
}
