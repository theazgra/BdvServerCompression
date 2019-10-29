package quantization.de;

import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.util.Arrays;

public class DEIndividual implements IDEIndividual, Comparable<DEIndividual> {

    protected Double m_fitness = null;
    protected double m_mutationFactor;
    protected double m_crossoverProbability;
    protected int[] m_attributes;

    protected DEIndividual(final int dimensionCount) {
        m_attributes = new int[dimensionCount];
    }

    protected DEIndividual(final int[] attributes) {
        m_attributes = attributes;
    }

    @Override
    public double getFitness() {
        assert (m_fitness != null);
        return m_fitness;
    }

    @Override
    public void setFitness(double fitness) {
        m_fitness = fitness;
    }

    @Override
    public boolean isFitnessCached() {
        return (m_fitness != null);
    }

    @Override
    public int compareTo(final DEIndividual other) {
        return Double.compare(m_fitness, other.m_fitness);
    }

    @Override
    public double getCrossoverProbability() {
        return m_crossoverProbability;
    }

    @Override
    public void setCrossoverProbability(final double cr) {
        m_crossoverProbability = cr;
    }

    @Override
    public double getMutationFactor() {
        return m_mutationFactor;
    }

    @Override
    public void setMutationFactor(final double f) {
        m_mutationFactor = f;
    }

    @Override
    public int[] getAttributes() {
        return m_attributes;
    }

    @Override
    public int getAttribute(final int dimension) {
        return m_attributes[dimension];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(m_attributes);
    }

    @Override
    public DEIndividual createOffspring(final int[] mutationVector, final int jRand,
                                        UniformRealDistribution rndCrDist) {
        assert (m_attributes.length == mutationVector.length);
        DEIndividual offspring = new DEIndividual(m_attributes.length);
        for (int j = 0; j < m_attributes.length; j++) {
            double crRnd = rndCrDist.sample();
            if ((j == jRand) || (crRnd < m_crossoverProbability)) {
                offspring.m_attributes[j] = mutationVector[j];
            } else {
                offspring.m_attributes[j] = m_attributes[j];
            }
        }
        return offspring;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DEIndividual) {
            return (this.hashCode() == ((DEIndividual) obj).hashCode());
        } else {
            return super.equals(obj);
        }
    }
}
