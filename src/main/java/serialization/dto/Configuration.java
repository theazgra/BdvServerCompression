package serialization.dto;

public class Configuration {
    private String methodName = "jade";
    private String dataFile = "D:\\tmp\\server-dump\\initial-load.bin";
    private String reportFile = "result.csv";
    private int dimension = 32;
    private int populationSize = 100;
    private int archiveSize = 100;
    private int minimalPopulationSize = 5;
    private int iterationCount = 1000;
    private int threadCount = 12;
    private double parameterAdaptationRate = 0.05;
    private double mutationGreediness = 0.1;
    private int memorySize = 10;

    @Override
    public String toString() {
        return String.format("Method: %s\nDataFile: %s\nReport file: %s\nDimension: %d\nPopulation size: %d\nMinimal population size: %d\nIteration count: %d\n" +
                        "Thread count: %d\nParameter adaptation rate: %.5f\nMutation greediness: %.5f\nMemory size: %d",
                methodName, dataFile, reportFile, dimension, populationSize, minimalPopulationSize, iterationCount, threadCount, parameterAdaptationRate,
                mutationGreediness, memorySize);
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDataFile() {
        return dataFile;
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public int getArchiveSize() {
        return archiveSize;
    }

    public void setArchiveSize(int archiveSize) {
        this.archiveSize = archiveSize;
    }

    public int getMinimalPopulationSize() {
        return minimalPopulationSize;
    }

    public void setMinimalPopulationSize(int minimalPopulationSize) {
        this.minimalPopulationSize = minimalPopulationSize;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

    public String getReportFile() {
        return reportFile;
    }

    public void setReportFile(String reportFile) {
        this.reportFile = reportFile;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
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

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }
}
