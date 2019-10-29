package quantization.de;

public interface IDESolver {

    void setMinimalValueConstraint(final int min);

    void setMaximalValueConstraint(final int max);

    void setPopulationSize(final int populationSize) throws DeException;

    void setGenerationCount(final int generationCount);

    void setDimensionCount(final int dimensionCount);

    DeHistory[] train() throws DeException;

    void setTrainingData(final int[] data);

    IDEIndividual getBestSolution();
}
