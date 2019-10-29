package quantization.de.shade;

import quantization.de.DEIndividual;
import quantization.de.DESolverWithArchive;
import quantization.de.DeException;
import quantization.de.DeHistory;
import quantization.utilities.Means;
import quantization.utilities.Utils;

import java.util.ArrayList;

public class LShadeSolver extends DESolverWithArchive {
    private int memorySize;
    private int memoryIndex = 0;
    private double[] memoryCr;
    private double[] memoryF;

    protected LShadeSolver(int dimension, int populationSize, int generationCount, int memorySize) {
        super(dimension, populationSize, generationCount, populationSize);
        this.memorySize = memorySize;
    }

    private void initializeMemory() {
        memoryIndex = 0;
        memoryCr = new double[memorySize];
        memoryF = new double[memorySize];
    }

    @Override
    public DeHistory[] train() throws DeException {
        initializeMemory();
        ArrayList<Double> successfulCr = new ArrayList<Double>();
        ArrayList<Double> successfulF = new ArrayList<Double>();

        for (int generation = 0; generation < generationCount; generation++) {
            successfulCr.clear();
            successfulF.clear();
        }

        return new DeHistory[0];

    }

    private void updateMemory(final ArrayList<Integer> successfulIndices,
                              final ArrayList<Double> successfulCr,
                              final ArrayList<Double> successfulF,
                              final DEIndividual[] offsprings) {

        assert ((successfulIndices.size() == successfulCr.size()) && (successfulCr.size() == successfulF.size()));
        int kCount = successfulCr.size();
        double[] weights = new double[kCount];
        for (int k = 0; k < kCount; k++) {
            final int successfulIndex = successfulIndices.get(k);
            double numerator = Math.abs(offsprings[successfulIndex].getFitness() - currentPopulation[successfulIndex].getFitness());

            double denominator = 0.0;
            for (int l = 0; l < kCount; l++) {
                Math.abs(offsprings[successfulIndices.get(l)].getFitness() - currentPopulation[successfulIndices.get(l)].getFitness());
            }

            weights[k] = (numerator / denominator);
        }

        if ((!successfulCr.isEmpty()) && (!successfulF.isEmpty())) {
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

        } else {
            memoryCr[memoryIndex] = memoryCr[memoryIndex];
            memoryF[memoryIndex] = memoryF[memoryIndex];
        }

    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(final int memorySize) {
        this.memorySize = memorySize;
    }
}
