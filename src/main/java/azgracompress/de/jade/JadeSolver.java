package azgracompress.de.jade;

import azgracompress.U16;
import azgracompress.de.DEIndividual;
import azgracompress.de.DESolverWithArchive;
import azgracompress.de.DeException;
import azgracompress.quantization.QTrainIteration;
import azgracompress.utilities.Means;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.Utils;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.Arrays;

public class JadeSolver extends DESolverWithArchive {
    private double muCr = 0.5;
    private double muF = 0.5;

    private double parameterAdaptationRate = 0.05;
    private double mutationGreediness = 0.1;

    private DEIndividual[] currentPopulationSorted = null;


    public JadeSolver(final int dimension, final int populationSize, final int generationCount) {

        super(dimension, populationSize, generationCount, populationSize);
    }

    public JadeSolver(final int dimension, final int populationSize, final int generationCount,
                      final double parameterAdaptationRate, final double mutationGreediness) {

        this(dimension, populationSize, generationCount);
        this.parameterAdaptationRate = parameterAdaptationRate;
        this.mutationGreediness = mutationGreediness;
    }

    @Override
    public QTrainIteration[] train() throws DeException {
        final String delimiter = "-------------------------------------------";
        QTrainIteration[] solutionHistory = new QTrainIteration[generationCount];
        if (trainingData == null || trainingData.length <= 0) {
            throw new DeException("Training data weren't set.");
        }
        muCr = 0.5;
        muF = 0.5;

        generateInitialPopulation();
        double avgFitness = calculateFitnessForPopulationParallel(currentPopulation);
        System.out.println(String.format("Generation %d average fitness(COST): %.5f", 0, avgFitness));

        ArrayList<Double> successfulCr = new ArrayList<Double>();
        ArrayList<Double> successfulF = new ArrayList<Double>();

        Stopwatch stopwatch = new Stopwatch();
        RandomGenerator rg = new MersenneTwister();

        int pBestUpperLimit = (int) Math.floor(populationSize * mutationGreediness);
        UniformIntegerDistribution rndPBestDist = new UniformIntegerDistribution(rg, 0, (pBestUpperLimit - 1));
        UniformIntegerDistribution rndIndDist = new UniformIntegerDistribution(rg, 0, (populationSize - 1));
        UniformIntegerDistribution rndJRandDist = new UniformIntegerDistribution(rg, 0, (dimensionCount - 1));
        UniformRealDistribution rndCrDist = new UniformRealDistribution(rg, 0.0, 1.0);
        DEIndividual[] offsprings = new DEIndividual[populationSize];

        for (int generation = 0; generation < generationCount; generation++) {

            stopwatch.restart();
            StringBuilder generationLog = new StringBuilder(String.format("%s\nGeneration: %d\n", delimiter, (generation + 1)));
            currentPopulationSorted = createSortedCopyOfCurrentPopulation();

            successfulCr.clear();
            successfulF.clear();


            UniformIntegerDistribution rndPopArchiveDist =
                    new UniformIntegerDistribution(rg, 0, ((populationSize - 1) + archive.size()));

            NormalDistribution crNormalDistribution = new NormalDistribution(rg, muCr, 0.1);
            CauchyDistribution fCauchyDistribution = new CauchyDistribution(rg, muF, 0.1);

            for (int i = 0; i < populationSize; i++) {
                DEIndividual current = currentPopulation[i];
                current.setCrossoverProbability(generateCrossoverProbability(crNormalDistribution));
                current.setMutationFactor(generateMutationFactor(fCauchyDistribution));

                DEIndividual x_p_Best = getRandomFromPBest(rndPBestDist, current);
                DEIndividual x_r1 = getRandomFromCurrentPopulation(rndIndDist, current, x_p_Best);
                DEIndividual x_r2 = getRandomFromPopulationAndArchive(rndPopArchiveDist, current, x_p_Best, x_r1);

                int[] mutationVector = createMutationVectorCurrentToPBest(current, x_p_Best, x_r1, x_r2);
                int jRand = rndJRandDist.sample();
                offsprings[i] = current.createOffspringBinominalCrossover(mutationVector, jRand, rndCrDist);
            }

            calculateFitnessForPopulationParallel(offsprings);

            // NOTE(Moravec): We are minimalizing!
            for (int i = 0; i < populationSize; i++) {
                if (offsprings[i].getFitness() <= currentPopulation[i].getFitness()) {
                    final DEIndividual old = currentPopulation[i];
                    currentPopulation[i] = offsprings[i];
                    successfulCr.add(old.getCrossoverProbability());
                    successfulF.add(old.getMutationFactor());
                    archive.add(old);
                }
            }

            double oldMuCr = muCr, oldMuF = muF;
            muCr = ((1.0 - parameterAdaptationRate) * muCr) + (parameterAdaptationRate * Means.arithmeticMean(successfulCr));
            muF = ((1.0 - parameterAdaptationRate) * muF) + (parameterAdaptationRate * Means.lehmerMean(successfulF));

            generationLog.append(String.format("|S_Cr| = %d  |S_F| = %d\n", successfulCr.size(), successfulF.size()));
            generationLog.append(String.format("Old μCR: %.5f    New μCR: %.5f\nOld  μF: %.5f     New μF: %.5f\n",
                    oldMuCr, muCr, oldMuF, muF));

            truncateArchive();
            generationLog.append(String.format("Archive size after truncate: %d\n", archive.size()));
            avgFitness = getMseFromCalculatedFitness(currentPopulation);
            stopwatch.stop();

            final double currentBestFitness = currentPopulationSorted[0].getFitness();
            final double avgPsnr = Utils.calculatePsnr(avgFitness, U16.Max);
            generationLog.append("Current best fitness: ").append(currentBestFitness);
            generationLog.append(String.format("\nAverage fitness(cost): %.6f\nIteration finished in: %d ms", avgFitness, stopwatch.totalElapsedMilliseconds()));

            System.out.println(generationLog.toString());

            solutionHistory[generation] = new QTrainIteration(generation, avgFitness, currentBestFitness, Utils.calculatePsnr(currentBestFitness, U16.Max), avgPsnr);
        }

        Arrays.sort(currentPopulationSorted);
        bestSolution = currentPopulationSorted[0];

        return solutionHistory;
    }


    public double getParameterAdaptationRate() {
        return parameterAdaptationRate;
    }

    public void setParameterAdaptationRate(double parameterAdaptationRate) {
        this.parameterAdaptationRate = parameterAdaptationRate;
    }

    public double getMutationGreediness() {
        return mutationGreediness;
    }

    public void setMutationGreediness(double mutationGreediness) {
        this.mutationGreediness = mutationGreediness;
    }
}
