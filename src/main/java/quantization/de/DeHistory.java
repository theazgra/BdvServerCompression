package quantization.de;

public class DeHistory {

    private int m_iteration = 0;
    private double m_avgCost = 0;
    private double m_bestCost = 0;

    public DeHistory(final int it, final double avgCost, final double bestCost) {
        m_iteration = it;
        m_avgCost = avgCost;
        m_bestCost = bestCost;
    }

    public double getBestCost() {
        return m_bestCost;
    }

    public void setBestCost(double bestCost) {
        this.m_bestCost = bestCost;
    }

    public double getAvgCost() {
        return m_avgCost;
    }

    public void setAvgCost(double avgCost) {
        this.m_avgCost = avgCost;
    }

    public int getIteration() {
        return m_iteration;
    }

    public void setIteration(int iteration) {
        this.m_iteration = iteration;
    }
}
