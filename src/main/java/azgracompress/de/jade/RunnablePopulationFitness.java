package azgracompress.de.jade;

import azgracompress.quantization.scalar.ScalarQuantizer;
import azgracompress.de.DEIndividual;

public class RunnablePopulationFitness implements Runnable {

    private double mse = 0.0;
    private final int[] testData;
    private final DEIndividual[] population;
    private final int fromIndex;
    private final int toIndex;

    public RunnablePopulationFitness(final int[] testData, final DEIndividual[] population, final int popFrom, final int popTo) {
        this.testData = testData;
        this.population = population;
        this.fromIndex = popFrom;
        this.toIndex = popTo;
    }

    @Override
    public void run() {
        double mse = 0.0;
        for (int i = fromIndex; i < toIndex; i++) {

            double indivMse;
            if (population[i].isFitnessCached()) {
                indivMse = population[i].getFitness();
            } else {
                ScalarQuantizer quantizer = new ScalarQuantizer(0, 0xffff, population[i].getAttributes());
                indivMse = quantizer.getMse(testData);
            }
            population[i].setFitness(indivMse);
            mse += indivMse;
        }
        this.mse = mse;
    }

    public double getTotalMse() {
        return mse;
    }

    public double getAvgMse() {
        return (mse / (double) (toIndex - fromIndex));
    }

}
