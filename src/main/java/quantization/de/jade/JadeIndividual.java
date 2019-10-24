package quantization.de.jade;

import org.apache.commons.math3.distribution.UniformRealDistribution;
import quantization.de.IIndividual;

import java.util.Arrays;

public class JadeIndividual implements IIndividual, Comparable<JadeIndividual> {
    /**
     * Quantization values of the individual.
     */
    int[] m_attributes;

    private double m_fitness;
    private double m_crossoverProbability;
    private double m_mutationFactor;

    public JadeIndividual(final int dimension) {
        m_attributes = new int[dimension];
    }

    public static JadeIndividual NewIndividual(final int[] attributes) {
        JadeIndividual individual = new JadeIndividual(attributes.length);
        individual.m_attributes = attributes;
        return individual;
    }

    public JadeIndividual createOffspring(final int[] mutationVector, final int jRand,
                                          UniformRealDistribution rndCrDist) {
        assert (m_attributes.length == mutationVector.length);
        JadeIndividual offspring = new JadeIndividual(m_attributes.length);
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

    public int getAttribute(final int attributeIndex) {
        return m_attributes[attributeIndex];
    }

    public final int[] getAttributes() {
        return m_attributes;
    }

    @Override
    public double getFitness() {
        return m_fitness;
    }

    @Override
    public void setFitness(double fitness) {
        m_fitness = fitness;
    }

    public double getMutationFactor() {
        return m_mutationFactor;
    }

    public void setMutationFactor(final double mutationFactor) {
        this.m_mutationFactor = mutationFactor;
    }

    public double getCrossoverProbability() {
        return m_crossoverProbability;
    }

    public void setCrossoverProbability(final double crossoverProb) {
        this.m_crossoverProbability = crossoverProb;
    }

    @Override
    public int compareTo(JadeIndividual other) {
        return Double.compare(m_fitness, other.m_fitness);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(m_attributes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JadeIndividual) {
            return (this.hashCode() == ((JadeIndividual) obj).hashCode());
        } else {
            return super.equals(obj);
        }
    }

    public String getInfo() {

        StringBuilder sb = new StringBuilder();
        for (int attrib : m_attributes) {
            sb.append(attrib);
            sb.append(" ");
        }
        sb.append(" Fitness: ");
        sb.append(m_fitness);
        return sb.toString();
    }
}
