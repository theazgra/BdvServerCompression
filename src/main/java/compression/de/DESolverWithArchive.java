package compression.de;

import compression.utilities.Utils;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;

import java.util.ArrayList;
import java.util.Random;

public abstract class DESolverWithArchive extends DESolver {
    protected int maxArchiveSize;
    protected ArrayList<DEIndividual> archive;

    protected DESolverWithArchive(int dimension, int currentPopulationSize, int generationCount, int maxArchiveSize) {
        super(dimension, currentPopulationSize, generationCount);
        this.maxArchiveSize = maxArchiveSize;
        archive = new ArrayList<DEIndividual>(maxArchiveSize);
    }


    protected void truncateArchive() {
        int deleteCount = archive.size() - maxArchiveSize;
        if (deleteCount > 0) {
            Random random = new Random();

            for (int i = 0; i < deleteCount; i++) {
                archive.remove(random.nextInt(archive.size()));
            }
        }
        assert (archive.size() <= maxArchiveSize);
    }


    /**
     * Get random individual (different from others) from union of current population and archive.
     *
     * @param rndUnionDist Random distribution for the union of current population and archive.
     * @param others       Other individuals.
     * @return Random individual from union of current population and archive.
     */
    protected DEIndividual getRandomFromPopulationAndArchive(UniformIntegerDistribution rndUnionDist,
                                                           final DEIndividual... others) {
        int rndIndex = rndUnionDist.sample();
        DEIndividual rndIndiv = (rndIndex >= currentPopulationSize) ? archive.get(rndIndex - currentPopulationSize) : currentPopulation[rndIndex];
        while (Utils.arrayContains(others, rndIndiv)) {
            rndIndex = rndUnionDist.sample();
            rndIndiv = (rndIndex >= currentPopulationSize) ? archive.get(rndIndex - currentPopulationSize) : currentPopulation[rndIndex];
        }

        return rndIndiv;
    }

    public int getMaxArchiveSize() {
        return maxArchiveSize;
    }

    public void setMaxArchiveSize(int maxArchiveSize) {
        this.maxArchiveSize = maxArchiveSize;
    }
}
