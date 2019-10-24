package quantization.de.jade;

import quantization.Quantizer;

import javax.lang.model.element.QualifiedNameable;

public class RunnablePopulationFitness implements Runnable {

    private double m_mse = 0.0;
    private final int[] m_testData;
    private final JadeIndividual[] m_population;
    private final int m_fromIndex;
    private final int m_toIndex;

    public RunnablePopulationFitness(final int[] testData, final JadeIndividual[] population, final int popFrom, final int popTo) {
        m_testData = testData;
        m_population = population;
        m_fromIndex = popFrom;
        m_toIndex = popTo;
    }

    @Override
    public void run() {
        double mse = 0.0;
        for (int i = m_fromIndex; i < m_toIndex; i++) {

            double indivMse;
            if (m_population[i].hasCachedFitness()) {
                indivMse = m_population[i].getFitness();
            } else {
                Quantizer quantizer = new Quantizer(0, 0xffff, m_population[i].getAttributes());
                indivMse = quantizer.getMse(m_testData);
            }
            m_population[i].setFitness(indivMse);
            mse += indivMse;
        }
        m_mse = mse;
    }

    public double getTotalMse() {
        return m_mse;
    }

    public double getAvgMse() {
        return (m_mse / (double) (m_toIndex - m_fromIndex));
    }

}
