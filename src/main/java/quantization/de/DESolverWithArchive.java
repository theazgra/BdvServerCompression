package quantization.de;

import java.util.ArrayList;
import java.util.Random;

public abstract class DESolverWithArchive extends DESolver {
    protected int maxArchiveSize;
    protected ArrayList<DEIndividual> archive;

    protected DESolverWithArchive(int dimension, int populationSize, int generationCount, int maxArchiveSize) {
        super(dimension, populationSize, generationCount);
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

    public int getMaxArchiveSize() {
        return maxArchiveSize;
    }

    public void setMaxArchiveSize(int maxArchiveSize) {
        this.maxArchiveSize = maxArchiveSize;
    }
}
