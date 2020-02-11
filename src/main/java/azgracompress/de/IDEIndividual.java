package azgracompress.de;

import org.apache.commons.math3.distribution.UniformRealDistribution;

public interface IDEIndividual {
    double getFitness();

    void setFitness(final double fitness);

    boolean isFitnessCached();

    double getCrossoverProbability();

    void setCrossoverProbability(final double cr);

    double getMutationFactor();

    void setMutationFactor(final double f);

    int[] getAttributes();

    int getAttribute(final int dimension);

    IDEIndividual createOffspringBinominalCrossover(final int[] mutationVector, final int jRand, UniformRealDistribution rndCrDist);
}
